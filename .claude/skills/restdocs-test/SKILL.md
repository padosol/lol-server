---
allowed-tools: Read, Write, Edit, Bash(./gradlew:*), Grep, Glob
argument-hint: [ControllerClassName]
description: Generate RestDocs test for a specific controller
---

## Context

- 대상 컨트롤러: $1
- 프로젝트 루트: !`pwd`
- 기존 테스트 패턴: @module/infra/api/src/test/java/com/example/lolserver/docs/RestDocsSupport.java
- 기존 테스트 예시: @module/infra/api/src/test/java/com/example/lolserver/docs/controller/ChampionControllerTest.java

## Your task

$1 컨트롤러에 대한 RestDocs 테스트 코드를 생성하세요.

### 1단계: 컨트롤러 분석

1. 컨트롤러 파일 찾기 및 읽기
2. 모든 API 엔드포인트 파악 (@GetMapping, @PostMapping 등)
3. 각 엔드포인트의 파라미터 타입 확인 (@PathVariable, @RequestParam, @RequestBody)
4. 응답 타입 분석 (ApiResponse<T>의 T 타입 구조)
5. 의존하는 서비스 클래스 확인

### 2단계: 테스트 클래스 생성

테스트 파일 위치: `module/infra/api/src/test/java/com/example/lolserver/docs/controller/{Controller}Test.java`

**필수 구조:**
```java
@ExtendWith(MockitoExtension.class)
class {Controller}Test extends RestDocsSupport {

    @Mock
    private {Service} {service};

    @InjectMocks
    private {Controller} {controller};

    @Override
    protected Object initController() {
        return {controller};
    }

    // 각 API에 대한 테스트 메서드
}
```

### 3단계: 테스트 메서드 작성 규칙

**구조:**
- `@DisplayName("한글 API 설명")`
- BDD 스타일: `// given`, `// when & then` 주석 사용
- Mock 설정: `BDDMockito.given()` 사용
- 문서화: `document()` 메서드로 RestDocs 스니펫 생성

**파라미터 문서화:**
- 경로 파라미터: `pathParameters(parameterWithName("name").description("설명"))`
- 쿼리 파라미터: `queryParameters(parameterWithName("name").description("설명"))`
- 요청 본문: `requestFields(fieldWithPath("field").type(JsonFieldType.TYPE).description("설명"))`

**응답 필드 문서화:**
- 필수 필드: `fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부")`
- 선택 필드: `.optional()` 추가
- 배열: `fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("설명")`
- 복잡한 객체: `subsectionWithPath("data.nested").type(JsonFieldType.OBJECT).description("설명")`
- Null: `fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보")`

**필수 import:**
```java
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.restdocs.payload.JsonFieldType;
```

### 4단계: 검증

테스트 생성 후 다음 명령어로 검증:
```bash
./gradlew test --tests "*{Controller}Test"
```

테스트가 실패하면 오류를 분석하고 수정하세요.
