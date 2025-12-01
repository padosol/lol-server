# Spring RestDocs 작성을 위한 고려사항 및 أفضل 실천 방법

Spring RestDocs를 사용하여 API 문서를 작성할 때 고려하면 좋은 점들을 정리합니다. RestDocs의 핵심 철학은 **"테스트를 통과해야만 문서가 생성된다"** 는 것입니다. 따라서 문서는 항상 실제 코드를 정확하게 반영하게 됩니다.

### 1. 테스트는 문서의 기반입니다

- **가장 중요한 원칙입니다.** 문서를 만들기 위해 테스트를 작성하는 것이 아니라, 잘 작성된 API 테스트의 결과물로 문서가 나온다고 생각해야 합니다.
- 테스트가 API의 실제 동작(성공, 실패, 엣지 케이스)을 검증하면, 문서는 자연스럽게 정확해집니다. API 계약(contract)이 변경되어 테스트가 실패하면, 문서도 생성되지 않아 변경을 강제할 수 있습니다.

### 2. 명확하고 상세한 설명 (Description)

- 단순히 필드가 존재한다는 것만 문서화하지 마세요. `.description()`을 적극적으로 활용하여 각 필드의 의미, 제약 조건, 예시 값 등을 상세히 적어주는 것이 좋습니다.
- **예시:**
  ```java
  fieldWithPath("summonerName").description("소환사 이름 (공백 포함 불가)")
  fieldWithPath("level").description("소환사 레벨 (1 이상)")
  fieldWithPath("profileIconId").description("프로필 아이콘 ID. Riot API에서 제공하는 값을 따름")
  ```

### 3. 재사용 가능한 스니펫(Snippet) 활용

- 여러 API에서 공통적으로 사용되는 요청/응답 필드(예: 페이징 처리, 공통 에러 응답)는 재사용 가능한 스니펫으로 만들어 관리하면 편리합니다.
- 예를 들어, 페이징 응답(`page`, `size`, `totalElements` 등)을 위한 `page-response-fields.adoc` 파일을 만들어두고 각 문서에서 `include`하여 사용할 수 있습니다.

### 4. 문서 구조화

- 생성된 스니펫들을 모아 최종 문서를 만드는 AsciiDoc 파일을 잘 구조화해야 합니다.
- `src/main/asciidoc` 폴더에 `api-guide.adoc` 같은 메인 파일을 만들고, 각 API별로 섹션을 나누어 `include` 구문으로 스니펫을 불러오는 방식을 추천합니다.
- 이렇게 하면 전체 문서의 목차, 제목, 개요 등을 자유롭게 구성할 수 있습니다.

**예시: `src/main/asciidoc/api-guide.adoc`**
```asciidoc
= API 문서
:doctype: book
:icons: font
:source-highlighter: highlightjs
:toc: left
:toclevels: 2

== 소환사(Summoner) API

=== 소환사 정보 조회
`GET /api/summoners/{summonerName}`

include::{snippets}/summoner-lookup/path-parameters.adoc[]
include::{snippets}/summoner-lookup/http-response.adoc[]
include::{snippets}/summoner-lookup/response-fields.adoc[]
```

### 5. 성공과 실패 케이스 모두 문서화

- API 사용자는 성공 케이스만큼이나 어떤 상황에서 에러가 발생하는지, 그리고 그 때 어떤 응답이 오는지를 궁금해합니다.
- `400 Bad Request`, `404 Not Found`, `500 Internal Server Error` 등 주요 실패 시나리오에 대한 테스트와 문서를 반드시 포함하세요.

### 6. 요청/응답 필드를 정확하게 문서화

- `requestFields`, `responseFields`, `pathParameters`, `queryParameters` 등을 사용하여 각 부분을 명확히 문서화하세요.
- 응답 객체가 복잡하게 중첩된 경우, `subsectionWithPath("data.")` 등을 사용하여 특정 하위 객체의 필드만 문서화하면 더 깔끔합니다.
- `relaxedResponseFields`는 응답의 일부 필드만 문서화할 수 있게 해주지만, 문서가 실제 응답과 달라질 수 있으므로 꼭 필요한 경우에만 제한적으로 사용하는 것이 좋습니다.

### 7. 빌드 자동화

- Maven이나 Gradle 같은 빌드 도구에 `asciidoctor` 플러그인을 통합하여, `build` 시점에 자동으로 HTML 문서가 생성되도록 설정하세요.
- 이렇게 하면 CI/CD 파이프라인에서 항상 최신 API 문서를 빌드하고 배포할 수 있습니다.

이러한 점들을 고려하여 RestDocs를 작성하시면, 유지보수가 쉽고 항상 신뢰할 수 있는 "살아있는(Living)" 문서를 만드실 수 있을 겁니다.
