# Member 저장 시 social_account 테이블 미저장 버그 수정

## Context

신규 회원 가입(OAuth2) 시 `social_account` 테이블에 레코드가 저장되지 않는 버그.
도메인에서 `Member.createNewWithSocialAccount()`로 SocialAccount를 포함하여 생성하지만, Adapter에서 JPA 엔티티로 변환할 때 socialAccounts가 누락됨.

## 근본 원인

`MemberMapperImpl.toEntity()` (MapStruct 생성 코드)가 `@AfterMapping setSocialAccountRelationships()`를 호출하지 않음.

- MapStruct 1.5.5 + Lombok Builder 조합에서, 빌더 기반 매핑 메서드(`toEntity()`)에 `@MappingTarget MemberEntity` 타입의 `@AfterMapping`이 적용되지 않음
- `updateEntityFromDomain()`은 기존 인스턴스를 `@MappingTarget`으로 받으므로 정상 작동

```
// MemberMapperImpl.java 생성 코드:
toEntity()                → setSocialAccountRelationships() 호출 ❌
updateEntityFromDomain()  → setSocialAccountRelationships() 호출 ✅
```

## 수정 방안

### 수정 파일
- `module/infra/persistence/postgresql/src/main/java/com/example/lolserver/repository/member/adapter/MemberPersistenceAdapter.java` (Line 50-52)

### 변경 내용

`save()` 메서드의 else 분기(신규 Member 생성)에서 `setSocialAccountRelationships()`를 수동 호출:

```java
// Before
} else {
    entity = memberMapper.toEntity(member);
}

// After
} else {
    entity = memberMapper.toEntity(member);
    memberMapper.setSocialAccountRelationships(member, entity);
}
```

### 이 방식을 선택한 이유
- `setSocialAccountRelationships()`는 MemberMapper의 `default` 메서드 → public API로 직접 호출 가능
- 내부에서 `isSocialAccountsLoaded()` + empty 체크를 이미 수행 → 방어 로직 중복 불필요
- 로직이 한 곳에만 존재하여 향후 변경 시 자동 반영
- `entity.addSocialAccount()`가 양방향 관계 설정 (`setMember(this)`) → CascadeType.ALL로 정상 저장

## 검증
1. `./gradlew test` — 기존 테스트 회귀 확인
2. `./gradlew build` — 빌드 성공 확인
