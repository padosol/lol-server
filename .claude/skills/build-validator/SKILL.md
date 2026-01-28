---
name: build-validator
description: |
  Java/Gradle 프로젝트 빌드 오류 자동 분석 및 해결 도구.
  다음 상황에서 사용:
  (1) ./gradlew build 또는 ./gradlew test 실행 후 컴파일 오류 발생 시 자동 트리거
  (2) 사용자가 /build-validator 명령으로 수동 호출
  (3) "빌드 실패", "컴파일 에러", "build failed" 등의 키워드 언급 시
  주요 기능: 컴파일 오류 분석, 누락된 import 추가, 타입 불일치 수정, 문법 오류 해결
---

# Build Validator

Java/Gradle 프로젝트 빌드 오류를 분석하고 자동으로 해결하는 스킬.

## 워크플로우

### 1. 빌드 실행 및 오류 수집

```bash
./gradlew build 2>&1
```

빌드 실패 시 전체 출력을 분석 대상으로 수집.

### 2. 오류 파싱

Gradle/Java 컴파일 오류 패턴:
- `파일경로:라인번호: error: 오류메시지`
- `> Task :모듈:compileJava FAILED`

각 오류에서 추출:
- 파일 경로
- 라인 번호
- 오류 유형
- 오류 메시지

### 3. 오류 유형별 해결 전략

#### 3.1 누락된 import

**패턴**: `cannot find symbol`, `symbol: class XXX`

**해결**:
1. 누락된 클래스명 추출
2. 프로젝트 내 해당 클래스 검색 (`Grep` 또는 `Glob` 사용)
3. 올바른 import문 추가

```java
// 오류: cannot find symbol - class SummonerResponse
// 해결: import 추가
import com.example.lolserver.domain.summoner.application.dto.SummonerResponse;
```

#### 3.2 타입 불일치

**패턴**: `incompatible types`, `required: X found: Y`

**해결**:
1. 요구 타입과 실제 타입 확인
2. 적절한 변환 또는 타입 수정

#### 3.3 메서드/필드 없음

**패턴**: `cannot find symbol`, `symbol: method XXX`

**해결**:
1. 해당 클래스 파일 읽기
2. 사용 가능한 메서드 확인
3. 올바른 메서드명으로 수정 또는 메서드 추가

#### 3.4 문법 오류

**패턴**: `;` expected, `illegal start of expression`

**해결**:
1. 해당 라인 확인
2. 누락된 세미콜론, 괄호 등 추가

### 4. 수정 적용

오류 파일을 `Read`로 읽고 `Edit`로 수정.

### 5. 재빌드 검증

수정 후 빌드 재실행하여 해결 확인:

```bash
./gradlew build 2>&1
```

실패 시 3단계로 돌아가 반복.

## 사용 예시

### 자동 트리거 시나리오

사용자가 코드 수정 후 빌드 실행:
```
> ./gradlew build
BUILD FAILED - compileJava 오류 발생
```

이 스킬이 자동으로:
1. 오류 메시지 파싱
2. 원인 파일 분석
3. 수정 적용
4. 재빌드로 검증

### 수동 호출

```
/build-validator
```

현재 프로젝트의 빌드 상태를 확인하고 오류가 있으면 해결.

## 주의사항

- 한 번에 하나의 오류씩 순차적으로 해결 (연쇄 오류 방지)
- 수정 전 원본 코드 확인 필수
- 3회 이상 같은 오류 반복 시 사용자에게 보고
