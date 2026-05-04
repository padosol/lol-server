# Plan: API 모듈 규칙 파일 작성

## Context

API 모듈의 개발 컨벤션을 `.claude/rules/` 디렉토리에 규칙 파일로 정리하여, Claude가 API 모듈 관련 파일을 읽거나 수정할 때 자동으로 해당 규칙을 로드하도록 한다. 현재 `.claude/rules/` 디렉토리는 존재하지 않으므로 새로 생성한다.

## 작업 내용

### 1. `.claude/rules/api-module.md` 파일 생성

YAML frontmatter의 `paths` 필드를 사용하여 API 모듈 경로에만 적용되도록 스코핑한다.

```yaml
---
paths:
  - "module/infra/api/**/*.java"
---
```

### 2. 규칙 파일에 포함할 내용

기존 코드베이스의 패턴을 기반으로 다음 3가지 영역을 문서화한다:

#### (a) Request/Response 객체 규칙
- **Java `record`** 사용 (전통적인 class 사용 금지)
- Request 객체 위치: `controller.{name}.request` 패키지
- Response 객체 위치: `controller.{name}.response` 패키지
- Request → Command 변환: `toCommand()` 메서드 정의
- Response ← ReadModel 변환: `static from(ReadModel)` 팩토리 메서드 정의
- 중첩 Response는 내부 record 클래스로 정의

#### (b) 값 검증 규칙
- `jakarta.validation.constraints` 어노테이션 사용
  - `@NotBlank`: 빈 문자열/공백 불가 문자열 필드
  - `@NotNull`: null 불가 비문자열 필드
  - `@Size(min=, max=)`: 문자열 길이 제약
- 컨트롤러 파라미터에 `@Valid @RequestBody` 필수
- nullable 필드는 검증 어노테이션 생략 (Optional 표현)

#### (c) RestDocs 규칙
- API 작성 후 반드시 RestDocs 테스트 추가
- 테스트 위치: `src/test/java/com/example/lolserver/docs/controller/`
- `RestDocsSupport` 상속, `@ExtendWith(MockitoExtension.class)` 사용
- `@AuthenticationPrincipal` 사용 시 `customArgumentResolvers()`에 `TestAuthenticatedMemberResolver` 등록
- `document()` 호출 시 `preprocessRequest(prettyPrint())`, `preprocessResponse(prettyPrint())` 필수
- `requestFields`, `responseFields`로 모든 필드 문서화
- AsciiDoc 스니펫 파일을 `src/docs/asciidoc/api/{name}/` 하위에 추가
- 문서 수정 후 `./gradlew :module:infra:api:asciidoctor` 실행

## 수정 대상 파일

- **신규 생성**: `.claude/rules/api-module.md`

## 검증

- `.claude/rules/api-module.md` 파일이 올바른 YAML frontmatter와 paths 패턴을 갖추었는지 확인
- 규칙 내용이 기존 CLAUDE.md와 중복/충돌하지 않는지 확인
