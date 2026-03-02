---
title: "QueryDSL Tuple + CAST(AS DATE)에서 발생하는 ClassCastException 분석"
date: 2026-03-01
author: lol-server team
category: troubleshooting
tags: [QueryDSL, JDBC, ClassCastException, java.sql.Date, LocalDate, Type Erasure]
---

# QueryDSL Tuple + CAST(AS DATE)에서 발생하는 ClassCastException 분석

## 1. 에러 증상

```
java.lang.ClassCastException: class java.sql.Date cannot be cast to class java.time.LocalDate
    at ...MatchSummonerRepositoryCustomImpl.lambda$findDailyGameCounts$1(line:239)
```

일별 게임 횟수를 조회하는 `findDailyGameCounts` 메서드에서, `gameCreateDatetime`(LocalDateTime)을 날짜(DATE)로 그룹핑하기 위해 `CAST({0} AS DATE)` SQL 표현식을 사용했다. 런타임에 Tuple에서 값을 꺼내는 시점에서 `ClassCastException`이 발생했다.

---

## 2. 버그가 있던 코드

```java
// ❌ 버그 코드
DateTemplate<LocalDate> gameDate = Expressions.dateTemplate(
        LocalDate.class, "CAST({0} AS DATE)", matchEntity.gameCreateDatetime);

return jpaQueryFactory
        .select(gameDate, matchSummonerEntity.count())
        ...
        .fetch()
        .stream()
        .map(tuple -> new DailyGameCountDTO(
                tuple.get(gameDate),    // ← 여기서 ClassCastException
                tuple.get(matchSummonerEntity.count())))
        .toList();
```

개발자의 의도: `DateTemplate<LocalDate>`로 선언했으니 `tuple.get(gameDate)`가 `LocalDate`를 반환할 것이다.

---

## 3. 근본 원인: JDBC 타입 시스템과 QueryDSL 제네릭의 괴리

### 3-1. 데이터 흐름 전체 그림

```
[PostgreSQL]                [JDBC 드라이버]              [QueryDSL Tuple]           [애플리케이션]

CAST(ts AS DATE)  ──SQL──▶  ResultSet.getObject()  ──▶  Tuple 내부 Object[]  ──▶  tuple.get(expr)
                            ↓                            ↓                        ↓
                            java.sql.Date 반환           Object[0] = java.sql.Date  (T) cast 시도
```

### 3-2. 핵심: QueryDSL 제네릭 `<T>`는 JDBC 반환 타입을 바꾸지 않는다

```java
DateTemplate<LocalDate> gameDate = Expressions.dateTemplate(
        LocalDate.class, "CAST({0} AS DATE)", ...);
```

이 코드에서 `LocalDate.class`는 **컴파일 타임 타입 힌트**일 뿐이다. QueryDSL이 SQL을 생성하고 JDBC로 실행할 때, 실제 결과값의 타입은 **JDBC 드라이버가 결정**한다.

| 계층 | 역할 | 타입 결정 |
|------|------|-----------|
| QueryDSL `DateTemplate<T>` | SQL 표현식 생성 + 컴파일 타임 타입 체크 | 제네릭 `T`는 컴파일러 힌트 |
| JDBC 드라이버 | `ResultSet`에서 실제 Java 객체 반환 | **PostgreSQL DATE → `java.sql.Date`** |
| QueryDSL `Tuple.get(expr)` | 내부 `Object[]`에서 값을 꺼내 `(T)` 캐스팅 | **JDBC가 반환한 실제 타입** |

### 3-3. `Tuple.get()` 내부 동작

```java
// QueryDSL Tuple 내부 (간략화)
public <T> T get(Expression<T> expr) {
    int index = findIndex(expr);
    return (T) values[index];   // ← unchecked cast!
}
```

`values[index]`에는 JDBC 드라이버가 반환한 `java.sql.Date` 객체가 들어있다. 제네릭 `T`가 `LocalDate`이므로 컴파일러는 이를 `(LocalDate) java.sql.Date`로 캐스팅하려 하고, **두 클래스 사이에 상속 관계가 없으므로** `ClassCastException`이 발생한다.

```
java.sql.Date    extends java.util.Date
java.time.LocalDate  — 완전히 별개의 클래스 (java.time 패키지)

→ java.sql.Date를 LocalDate로 직접 캐스팅 불가
```

### 3-4. 왜 컴파일 타임에 잡히지 않는가?

Java 제네릭은 **타입 소거(Type Erasure)** 때문에 런타임에 `T`의 실제 타입 정보가 사라진다. `Tuple.get(Expression<T>)`의 반환 타입 `T`는 컴파일 시에만 존재하고, 런타임에는 단순한 `Object` 캐스팅이 된다. 따라서 컴파일러는 에러를 감지할 수 없고, 실제 실행 시점에서야 `ClassCastException`이 터진다.

---

## 4. 수정 방법

```java
// ✅ 수정된 코드
DateTemplate<java.sql.Date> gameDate = Expressions.dateTemplate(
        java.sql.Date.class, "CAST({0} AS DATE)", matchEntity.gameCreateDatetime);
//      ^^^^^^^^^^^^^^^^
//      JDBC 드라이버가 실제로 반환하는 타입과 일치시킴

return jpaQueryFactory
        .select(gameDate, matchSummonerEntity.count())
        ...
        .fetch()
        .stream()
        .map(tuple -> new DailyGameCountDTO(
                tuple.get(gameDate).toLocalDate(),
//                                 ^^^^^^^^^^^^
//              java.sql.Date → LocalDate 명시적 변환
                tuple.get(matchSummonerEntity.count())))
        .toList();
```

**변경 포인트 2가지:**

| # | 변경 | 이유 |
|---|------|------|
| 1 | `DateTemplate<LocalDate>` → `DateTemplate<java.sql.Date>` | JDBC 드라이버 반환 타입과 일치시켜 `Tuple.get()`의 unchecked cast가 성공하도록 함 |
| 2 | `tuple.get(gameDate)` → `tuple.get(gameDate).toLocalDate()` | `java.sql.Date`가 제공하는 `toLocalDate()` 메서드로 명시적 타입 변환 |

---

## 5. `java.sql.Date.toLocalDate()` 변환 원리

```java
// java.sql.Date 클래스 내부 (JDK 소스)
public LocalDate toLocalDate() {
    return LocalDate.of(getYear() + 1900, getMonth() + 1, getDate());
}
```

`java.sql.Date`는 Java 8에서 `toLocalDate()` 브릿지 메서드가 추가되었다. 이를 통해 레거시 JDBC 타입에서 모던 `java.time` 타입으로 안전하게 변환할 수 있다.

---

## 6. 대안 비교: 왜 Tuple 방식을 택했는가

| 방식 | 코드 | 장단점 |
|------|------|--------|
| **Tuple + 명시적 변환** (현재) | `tuple.get(gameDate).toLocalDate()` | JDBC 타입을 정직하게 다룸, 추가 Q클래스 불필요 |
| `@QueryProjection` DTO | `new QDailyGameCountDTO(gameDate, count)` | Q클래스 생성 필요, 하지만 `CAST AS DATE` 결과가 여전히 `java.sql.Date`이므로 DTO 생성자에서 같은 문제 발생 |
| `Projections.constructor()` | `Projections.constructor(DailyGameCountDTO.class, gameDate, count)` | 리플렉션 기반이지만 타입 미스매치는 동일하게 발생 |
| Hibernate `@ColumnTransformer` | JPA 엔티티 레벨 설정 | 이 경우 엔티티가 아닌 집계 쿼리이므로 부적합 |

`@QueryProjection`이나 `Projections.constructor()` 방식도 결국 JDBC가 `java.sql.Date`를 반환하는 것은 변하지 않으므로, Tuple에서 명시적으로 변환하는 것이 가장 간단하고 투명한 해결책이다.

---

## 7. 핵심 교훈

### 교훈 1: QueryDSL 제네릭 타입 파라미터 ≠ JDBC 반환 타입
`Expressions.dateTemplate(LocalDate.class, ...)` 처럼 선언해도, JDBC 드라이버는 `java.sql.Date`를 반환한다. 제네릭 `<T>`는 컴파일 타임 편의 기능이지, 런타임 타입 변환기가 아니다.

### 교훈 2: Native SQL 표현식 사용 시 JDBC 레벨 타입을 기준으로 생각하라
`CAST(... AS DATE)`, `EXTRACT(...)` 등 SQL 함수를 `Expressions.xxxTemplate()`으로 감쌀 때, 제네릭 타입은 **JDBC 드라이버가 해당 SQL 타입에 대해 반환하는 Java 타입**으로 맞춰야 한다.

| SQL 타입 | PostgreSQL JDBC 반환 타입 | 올바른 제네릭 |
|----------|--------------------------|---------------|
| `DATE` | `java.sql.Date` | `DateTemplate<java.sql.Date>` |
| `TIMESTAMP` | `java.sql.Timestamp` | `DateTimeTemplate<java.sql.Timestamp>` |
| `INTEGER` | `Integer` | `NumberTemplate<Integer>` |

### 교훈 3: Java 제네릭의 타입 소거를 항상 인지하라
컴파일이 통과한다고 런타임에 안전하지 않다. 특히 프레임워크 내부에서 `(T) object` 형태의 unchecked cast가 일어나는 구간에서는, 제네릭 타입이 실제 객체 타입과 일치하는지 개발자가 직접 보장해야 한다.

---

## 8. 관련 파일

| 파일 | 경로 |
|------|------|
| Repository 구현체 (수정 대상) | `module/infra/persistence/postgresql/.../matchsummoner/dsl/impl/MatchSummonerRepositoryCustomImpl.java` |
| DTO | `module/infra/persistence/postgresql/.../match/dto/DailyGameCountDTO.java` |
| ReadModel | `module/core/lol-server-domain/.../match/application/model/DailyGameCountReadModel.java` |
| Adapter | `module/infra/persistence/postgresql/.../match/adapter/MatchPersistenceAdapter.java` |
