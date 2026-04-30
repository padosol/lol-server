# 듀오 API RestDocs AsciiDoc 스니펫 추가

## Context

듀오 찾기 API의 RestDocs 테스트(`DuoPostControllerTest`, `DuoRequestControllerTest`)는 이미 작성되어 있으나, 테스트가 생성하는 스니펫을 포함하는 AsciiDoc 파일과 `index.adoc` 등록이 누락되어 있다. 다른 모든 도메인(community, auth, member 등)은 `src/docs/asciidoc/api/{name}/` 하위에 `.adoc` 파일이 존재하고 `index.adoc`에서 include하고 있다.

## 수정 대상 파일

### 1. 신규 생성 — `src/docs/asciidoc/api/duo/*.adoc` (13개)

기존 community 패턴을 따른다. 각 `.adoc` 파일은 테스트의 `document()` 식별자와 1:1 매핑.

**DuoPost (6개)**

| 파일명 | document ID | 포함 스니펫 |
|--------|------------|------------|
| `duo-post-create.adoc` | `duo-post-create` | request-fields, response-fields |
| `duo-post-list.adoc` | `duo-post-list` | query-parameters, response-fields |
| `duo-post-detail.adoc` | `duo-post-detail` | path-parameters, response-fields |
| `duo-post-update.adoc` | `duo-post-update` | path-parameters, request-fields, response-fields |
| `duo-post-delete.adoc` | `duo-post-delete` | path-parameters (204, body 없음) |
| `duo-post-my-list.adoc` | `duo-post-my-list` | query-parameters, response-fields |

**DuoRequest (7개)**

| 파일명 | document ID | 포함 스니펫 |
|--------|------------|------------|
| `duo-request-create.adoc` | `duo-request-create` | path-parameters, request-fields, response-fields |
| `duo-request-list-for-post.adoc` | `duo-request-list-for-post` | path-parameters, response-fields |
| `duo-request-accept.adoc` | `duo-request-accept` | path-parameters, response-fields |
| `duo-request-confirm.adoc` | `duo-request-confirm` | path-parameters, response-fields |
| `duo-request-reject.adoc` | `duo-request-reject` | path-parameters (204, body 없음) |
| `duo-request-cancel.adoc` | `duo-request-cancel` | path-parameters (204, body 없음) |
| `duo-request-my-list.adoc` | `duo-request-my-list` | query-parameters, response-fields |

### 2. 수정 — `src/docs/asciidoc/index.adoc`

Community API 섹션 앞에 `[[Duo-API]]` 섹션 추가하여 13개 `.adoc` 파일을 include.

## 구현 순서

1. `src/docs/asciidoc/api/duo/` 디렉토리 생성
2. 13개 `.adoc` 파일 작성 (community 패턴 참고)
3. `index.adoc`에 Duo API 섹션 추가
4. RestDocs 테스트 실행하여 스니펫 생성 확인: `./gradlew test --tests "*.DuoPostControllerTest" --tests "*.DuoRequestControllerTest"`
5. AsciiDoctor 문서 재생성: `./gradlew :module:infra:api:asciidoctor`

## 검증

- `./gradlew test --tests "*.DuoPostControllerTest" --tests "*.DuoRequestControllerTest"` 통과
- `./gradlew :module:infra:api:asciidoctor` 성공
- `build/docs/asciidoc/index.html`에서 Duo API 섹션 확인
