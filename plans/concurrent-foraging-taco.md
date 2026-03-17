# Checkstyle 위반 수정 계획

## Context
이전 대화에서 테스트 목적으로 `VersionFinder.java`에 의도적으로 `NeedBraces` checkstyle 위반을 도입했음. 이제 이를 원래 코드로 복원하여 checkstyle을 통과시켜야 함.

## 위반 현황
- **파일**: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/version/application/VersionFinder.java:22`
- **규칙**: `NeedBraces` — `'if' construct must use '{}'s.`
- **위반 코드**: `if (dbVersion.isPresent()) versionCachePort.saveLatestVersion(dbVersion.get());`
- 총 위반 수: **1건** (main + test 전체 확인 완료)

## 수정 내용
22행을 원래의 함수형 스타일로 복원:

**Before (위반):**
```java
if (dbVersion.isPresent()) versionCachePort.saveLatestVersion(dbVersion.get());
```

**After (수정):**
```java
dbVersion.ifPresent(versionCachePort::saveLatestVersion);
```

이 방식이 더 관용적이고, 중괄호 없는 if문 위반 문제도 근본적으로 해결됨.

## 검증
```bash
./gradlew checkstyleMain checkstyleTest
```
