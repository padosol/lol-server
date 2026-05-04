---
paths:
  - "module/infra/api/**/*.java"
---

# API 모듈 규칙

## Request / Response 객체

- Request, Response 모두 **Java `record`** 로 작성 (전통적인 class 사용 금지)
- Request 위치: `controller.{name}.request` 패키지
- Response 위치: `controller.{name}.response` 패키지
- Request → Command 변환: `toCommand()` 인스턴스 메서드 정의
- Response ← ReadModel 변환: `static from(ReadModel)` 팩토리 메서드 정의
- 중첩 Response 는 부모 Response 안에 내부 record 클래스로 정의

## 값 검증

- `jakarta.validation.constraints` 어노테이션 사용
  - `@NotBlank` — 빈 문자열·공백 불가 문자열 필드
  - `@NotNull` — null 불가 비문자열 필드 (Long, Enum 등)
  - `@Size(min=, max=)` — 문자열 길이 제약
- 컨트롤러 파라미터에 `@Valid @RequestBody` 필수
- nullable(선택) 필드는 검증 어노테이션 생략

## RestDocs

- API 작성 후 반드시 RestDocs 테스트를 추가한다
- 테스트 위치: `src/test/java/com/example/lolserver/docs/controller/`
- `RestDocsSupport` 상속, `@ExtendWith(MockitoExtension.class)` 사용
- `@AuthenticationPrincipal` 사용 시 `customArgumentResolvers()` 에 `TestAuthenticatedMemberResolver` 등록
- `document()` 호출 시 `preprocessRequest(prettyPrint())`, `preprocessResponse(prettyPrint())` 필수
- `requestFields`, `responseFields` 로 모든 필드 문서화 (`result`, `errorMessage`, `data.*` 포함)
- AsciiDoc 스니펫 파일을 `src/docs/asciidoc/api/{name}/` 하위에 추가
- RestDocs 관련 파일 수정 후 `./gradlew :module:infra:api:asciidoctor` 실행
