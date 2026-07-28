# ResponseEntity + RESTful 상태코드 적용

## Context
Duo/Community 컨트롤러가 `ApiResponse<T>`를 직접 반환하여 항상 200 OK를 내려주고 있음. 기존 컨트롤러(Match, Summoner 등)는 `ResponseEntity<ApiResponse<T>>`를 사용. RESTful 설계에 부합하도록 통일하고 CLAUDE.md에 컨벤션 추가.

## 상태코드 매핑

| HTTP 메서드 | 용도 | 상태코드 |
|-------------|------|----------|
| POST | 리소스 생성 | `201 Created` |
| GET | 조회 | `200 OK` |
| PUT | 수정 | `200 OK` |
| DELETE | 삭제 | `204 No Content` |

## 수정 대상 (컨트롤러 5개 + RestDocs 테스트 4개)

### 컨트롤러
1. `module/infra/api/src/main/java/.../controller/duo/DuoPostController.java`
2. `module/infra/api/src/main/java/.../controller/duo/DuoRequestController.java`
3. `module/infra/api/src/main/java/.../controller/community/CommunityPostController.java`
4. `module/infra/api/src/main/java/.../controller/community/CommunityCommentController.java`
5. `module/infra/api/src/main/java/.../controller/community/CommunityVoteController.java`

### RestDocs 테스트
6. `module/infra/api/src/test/java/.../docs/controller/DuoPostControllerTest.java`
7. `module/infra/api/src/test/java/.../docs/controller/DuoRequestControllerTest.java`
8. `module/infra/api/src/test/java/.../docs/controller/CommunityPostControllerTest.java`
9. `module/infra/api/src/test/java/.../docs/controller/CommunityCommentControllerTest.java`

### CLAUDE.md
10. `CLAUDE.md` — 코드 컨벤션에 ResponseEntity + 상태코드 규칙 추가

## 변환 패턴

```java
// Before
@PostMapping("/posts")
public ApiResponse<DuoPostResponse> createDuoPost(...) {
    return ApiResponse.success(DuoPostResponse.from(result));
}

// After
@PostMapping("/posts")
public ResponseEntity<ApiResponse<DuoPostResponse>> createDuoPost(...) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(DuoPostResponse.from(result)));
}

// DELETE: 204 No Content, body 없음
@DeleteMapping("/posts/{postId}")
public ResponseEntity<Void> deleteDuoPost(...) {
    duoPostUseCase.deleteDuoPost(member.memberId(), postId);
    return ResponseEntity.noContent().build();
}
```

## RestDocs 테스트 변경
- POST 테스트: `.andExpect(status().isCreated())`
- DELETE 테스트: `.andExpect(status().isNoContent())`, responseFields 제거
- GET/PUT: `.andExpect(status().isOk())` (기존과 동일)

## 검증
```bash
./gradlew test
./gradlew :module:infra:api:asciidoctor
```
