# SocialAccountEntity "Multiple representations" 에러 수정 플랜

## Context

OAuth 로그인 또는 소셜 계정 연동/탈퇴 시 `SocialAccountEntity#19`에 대해 **managed 인스턴스와 detached 인스턴스가 동시에 존재**하여 JPA merge cascade에서 에러가 발생한다.

## 근본 원인

**`MemberMapper`의 `@AfterMapping setSocialAccountRelationships`가 `updateEntityFromDomain`에도 자동 적용된다.**

생성된 `MemberMapperImpl.java:101`에서 확인:

```java
// MemberMapperImpl.java (generated)
@Override
public void updateEntityFromDomain(Member member, MemberEntity entity) {
    // ... 필드 매핑 ...
    setSocialAccountRelationships(member, entity);  // ← 여기서 문제 발생!
}
```

### 에러 발생 흐름 (예: `linkSocialAccount`)

1. `findByIdWithSocialAccounts()` → MemberEntity + **managed** SocialAccountEntity#19 로드 (FETCH JOIN)
2. 도메인에서 새 SocialAccount 추가 (id=null)
3. `save(member)` 호출:
   - `findById()` → 같은 managed MemberEntity 반환 (1차 캐시)
   - `updateEntityFromDomain(member, entity)` 호출
   - **`@AfterMapping`에 의해 `setSocialAccountRelationships` 자동 실행:**
     - `toSocialAccountEntity(SA{id=19})` → **새로운 detached SocialAccountEntity(id=19) 생성**
     - `entity.addSocialAccount()` → managed 컬렉션에 detached 인스턴스 추가
   - `syncSocialAccounts()` 실행 → 기존 managed 인스턴스 업데이트하지만 detached 인스턴스는 제거하지 않음
   - `memberJpaRepository.save(entity)` → merge cascade 시 **managed #19 + detached #19** 공존 → **에러!**

## 수정 방법

### 파일: `MemberMapper.java`
경로: `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/member/MemberMapper.java`

`@AfterMapping` 어노테이션을 제거한다. 이 메서드는 이미 `MemberPersistenceAdapter.save()`에서 새 엔티티 생성 시 **명시적으로 호출**되고 있으므로, `@AfterMapping`으로 자동 적용될 필요가 없다.

```java
// Before
@AfterMapping
default void setSocialAccountRelationships(
        Member member, @MappingTarget MemberEntity entity) { ... }

// After
default void setSocialAccountRelationships(
        Member member, @MappingTarget MemberEntity entity) { ... }
```

변경 사항:
- `@AfterMapping` 제거 → `updateEntityFromDomain` 호출 시 자동 실행 방지
- `MemberPersistenceAdapter.save()`의 새 엔티티 경로에서는 기존대로 명시적 호출 유지 (line 52)

### 영향 범위

| 메서드 | 변경 전 | 변경 후 |
|--------|---------|---------|
| `toEntity()` | `@AfterMapping` 미적용 (생성 코드 확인) | 변화 없음 |
| `updateEntityFromDomain()` | `@AfterMapping` 적용 → **버그** | `@AfterMapping` 제거 → 정상 |
| 명시적 호출 (adapter line 52) | 정상 동작 | 정상 동작 (변화 없음) |

## 검증

1. `./gradlew build` → 컴파일 및 전체 테스트 통과 확인
2. 생성된 `MemberMapperImpl.java`에서 `updateEntityFromDomain`이 더 이상 `setSocialAccountRelationships`를 호출하지 않는지 확인
3. `linkSocialAccount`, `unlinkSocialAccount`, `withdraw`, `loginExistingMember` 시나리오에서 에러 미발생 확인
