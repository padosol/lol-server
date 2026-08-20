# 커뮤니티 이미지 업로드 설계

> 상태: **구현 완료(인프라 준비 대기)** · 대상 컨텍스트: `module/domain/community` · 관련 이슈: MP-XXX
>
> 코드는 브랜치 `feature/MP-XXX-community-image-upload` 에 있다. 남은 것은 8절 PR 0(버킷·CloudFront·IAM)과
> `lol-db-schema` V36 머지뿐이며, 그 둘이 끝나면 바로 뜬다. 구현 과정에서 설계와 달라진 지점은
> 각 절에 **구현 노트**로 표시했다.

게시글 본문에 이미지를 첨부하는 기능의 설계다. 지금 이 레포에는 파일 업로드·오브젝트 스토리지 관련 코드가 **하나도 없다** — `MultipartFile` 사용처 0건, AWS SDK 의존 0건, 스토리지 설정 0건. 즉 이 문서는 "기존 업로드 파이프라인에 커뮤니티를 얹는" 설계가 아니라 **스토리지 경로를 처음 뚫는** 설계다.

---

## 1. 범위

### 이번 범위 (Phase 1)

- 게시글 본문 이미지 업로드 / 삽입 / 교체 / 삭제
- 업로드된 이미지의 생명주기 추적 (고아 파일 정리 포함)
- 로컬 개발환경에서 **운영과 같은 S3 경로**로 동작 (버킷만 분리해 격리 — D3)

### 이번 범위 밖 (의도적으로 제외)

| 제외 항목 | 이유 |
|---|---|
| 댓글 이미지 | 본문과 같은 파이프라인을 재사용하면 되지만, 댓글 UX(인라인 에디터 유무)가 정해지지 않았다. `community_image.post_id` 옆에 `comment_id` 를 추가하는 확장으로 처리 |
| 회원 프로필 이미지 직접 업로드 | 현재 `member.profile_image_url` 은 OAuth 프로바이더가 준 URL 을 그대로 저장한다. 직접 업로드는 별개 요구사항 |
| 동영상 / 일반 파일 첨부 | 검증·용량·CDN 정책이 다르다 |
| 이미지 리사이즈 변형본(썸네일 세트) 생성 | Phase 1 은 원본 1장 + 상한 리사이즈만. 목록 썸네일이 필요해지면 3절 D6 참고 |

### 전제 (확정 필요 — 9절)

- 프론트엔드 에디터가 본문에 이미지를 **URL 참조**(마크다운 `![](url)` 혹은 HTML `<img src>`) 형태로 삽입한다고 가정한다. base64 인라인은 지원하지 않는다(본문 TEXT 컬럼이 감당 못 한다).
- 운영 스토리지로 **S3 버킷 + CloudFront 배포**를 신규 생성할 수 있다고 가정한다(인프라 담당 확인 필요).

---

## 2. 현재 코드베이스 제약

설계가 반드시 지켜야 하는 기존 규칙들이다.

| 제약 | 출처 | 설계에 미치는 영향 |
|---|---|---|
| `community` 는 `member` **외** 다른 컨텍스트에 의존 금지 | `community/ArchitectureTest#그외_컨텍스트에는_의존하지_않는다` | 이미지를 별도 `module:domain:media` 컨텍스트로 빼면 이 규칙에 걸린다 → 컨텍스트 신설 시 규칙 수정이 선행돼야 함 |
| `domain`·`application` 은 인프라 타입(JPA/웹/QueryDSL) 의존 금지 | 같은 테스트 | `MultipartFile`, `S3Client` 가 application 계층에 들어오면 안 된다 → 포트에서 원시 타입으로 받아야 함 |
| `adapter.in` → `adapter.out` 직접 의존 금지 | 같은 테스트 | 컨트롤러가 스토리지 어댑터를 직접 부를 수 없다 |
| 인프라 config 는 `module/common/config`, SDK 의존은 **쓰는 모듈이 각자 선언** | `RedisConfig` in common + `match`/`championstats` build.gradle 이 각각 `spring-boot-starter-data-redis` 선언 | `S3Config` 는 common, `software.amazon.awssdk:s3` 는 common·community 양쪽에 선언 |
| 엔티티는 상태 enum 을 `String` 컬럼으로 저장 | `DuoPostEntity.status`, `DuoRequestEntity.status` | `ImageStatus` 도 도메인 enum ↔ 엔티티 String 매핑 |
| checkstyle `maxWarnings = 0` | 루트 `build.gradle` | 새 파일도 스타일 위반 0건이어야 빌드 통과 |
| DB 스키마는 `lol-db-schema` 서브모듈의 Flyway 마이그레이션 | `.gitmodules`, V35 까지 존재 | 이번 변경은 **V36**, 서브모듈 PR 이 먼저 머지되고 포인터를 갱신해야 함 |
| `/api/community/**` 는 GET 만 permitAll, 나머지 authenticated | `member/config/SecurityConfig` | 업로드 엔드포인트는 별도 규칙 추가 없이 자동으로 인증 필수가 된다 |

---

## 3. 설계 결정

### D1. 코드 배치 — `community` 컨텍스트 내부

```
module/domain/community/…/community/
├── domain/
│   ├── PostImage.java                     ← 도메인 (상태 전이 규칙 보유)
│   └── vo/ImageStatus.java                ← UPLOADING / PENDING / ATTACHED / DETACHED
├── application/
│   ├── port/in/ImageUseCase.java
│   ├── port/out/ImageStoragePort.java     ← 바이트를 저장/삭제하는 추상
│   ├── port/out/ImagePersistencePort.java
│   ├── command/UploadImageCommand.java
│   ├── model/readmodel/PostImageReadModel.java
│   └── ImageService.java
└── adapter/
    ├── in/web/CommunityImageController.java
    └── out/
        ├── storage/
        │   └── S3ImageStorageAdapter.java        ← 단일 구현 (로컬·운영 공용)
        └── persistence/{entity,repository,mapper,adapter}/
```

**왜 별도 `module:domain:media` 컨텍스트가 아닌가.** 지금 이미지를 필요로 하는 소비자는 커뮤니티 하나뿐이다. 컨텍스트를 신설하면 `community/ArchitectureTest` 의 컨텍스트 격리 규칙을 완화해야 하고(`media` 를 허용 목록에 추가), 모듈·빌드·조립 비용이 붙는다. 두 번째 소비자(회원 프로필 직접 업로드, 듀오 스크린샷 등)가 실제로 생기면 그때 `ImageStoragePort`+어댑터만 들어올려 승격한다 — 포트로 감싸 뒀으므로 이동 비용이 낮다.

**왜 `common` 에 몰지 않는가.** `common` 은 공유 커널이지 도메인이 아니다. "이미지의 생명주기"(PENDING→ATTACHED→정리)는 명백히 커뮤니티의 비즈니스 규칙이므로 도메인 모듈에 있어야 한다. 다만 **기술 빈인 `S3Client` 생성만은 `common/config/S3Config`** 에 둔다 — `RedisConfig`/`RestClientConfig` 와 같은 자리다.

### D2. 업로드 방식 — 서버 경유 멀티파트 (권장)

| | ① 서버 경유 multipart **(권장)** | ② Presigned PUT |
|---|---|---|
| 서버 부하 | 파일이 앱을 통과 (5MB×동시업로드) | 메타데이터만 |
| 검증 시점 | 저장 **전** 완전 검증 가능 | 저장 **후** 사후 검증 → 이미 버킷에 올라간 상태 |
| EXIF 제거·리사이즈·정규화 | 서버에서 한 번에 | Lambda/Edge 함수 별도 구축 필요 |
| CORS | 불필요 (기존 CORS 정책 그대로) | 버킷 CORS 별도 구성 |
| 클라이언트 단계 | 1단계 | 3단계(발급→PUT→확정) |
| 구현/운영 복잡도 | 낮음 | 중간 |

**①을 권장하는 이유.** 지금 규모(단일 Spring 앱, 커뮤니티 게시글 수 소규모)에서 5MB 파일이 앱을 통과하는 비용보다, 악성 파일을 **버킷에 올리기 전에** 걷어낼 수 있다는 이점이 훨씬 크다. Presigned 방식은 "확정 API 를 안 부르고 버킷만 채우는" 남용 경로가 구조적으로 열려 있고, 이를 막으려면 결국 사후 검증 워커를 따로 만들어야 한다. 또한 EXIF GPS 제거는 개인정보 이슈라 Phase 1 에서 빼기 어렵다.

전환 여지는 남긴다: `ImageStoragePort` 가 바이트를 받으므로, 트래픽이 커지면 포트에 `createUploadTicket()` 을 추가하고 어댑터만 바꿔 presigned 로 이행할 수 있다.

```java
public interface ImageStoragePort {
    StoredImageLocation allocate(String extension);              // 키·URL 선발급
    void store(String storageKey, byte[] content, String contentType);
    void delete(String storageKey);
    void deleteAll(List<String> storageKeys);   // 정리 배치용 배치 삭제
}
```

> **구현 노트 — `allocate` 가 생긴 이유.** 최초 설계는 `store()` 가 키를 만들어 돌려주는 형태였다.
> 그런데 D7 에서 순서를 "DB INSERT → S3 PUT" 으로 뒤집으면서, INSERT 시점에 이미 키를 알아야 하게 됐다.
> 그래서 키 발급과 저장을 분리했다. 키 레이아웃(환경 prefix 포함)은 여전히 어댑터만 안다.

`MultipartFile` 은 컨트롤러(adapter.in)에서 `byte[]` + 메타로 풀어서 커맨드에 담는다. ArchUnit 의 "application 은 웹 타입 의존 금지" 규칙 때문이기도 하고, 포트를 웹 프레임워크에서 떼어놓는 게 맞기 때문이기도 하다.

> **구현 노트 — `ImageProcessorPort` 를 하나 더 뒀다.** 타입 판별(Tika)·크기 파싱·리사이즈(ImageIO)는
> 기술에 강하게 묶인다. 이걸 서비스에 두면 애플리케이션이 Tika/ImageIO 를 의존하게 되고, 서비스
> 단위테스트가 실제 이미지를 디코딩해야 돌아간다. 포트로 빼서 `adapter/out/image` 에 구현을 두고,
> ArchUnit 에 `software.amazon.awssdk..`·`org.apache.tika..`·`javax.imageio..`·`java.awt..` 금지 규칙을 추가했다.

### D3. 스토리지 — 로컬도 실제 S3, 버킷을 분리해 격리한다

**어댑터는 하나뿐이다.** 로컬 파일시스템 어댑터를 두지 않고 `S3ImageStorageAdapter` 만 둔다.
로컬과 운영이 **완전히 같은 코드 경로**를 타고, 다른 것은 설정값(버킷·prefix·CDN 도메인)뿐이다.
"로컬에서 통과했으니 운영에서도 통과한다"가 성립하려면 검증 대상 코드가 같아야 한다 —
로컬만 파일시스템을 타면 S3 권한·키 규칙·CDN 캐시·삭제 동작은 **운영에 배포한 뒤에야 처음 실행된다.**

| | local | prod |
|---|---|---|
| 버킷 | `lol-community-images-dev` | `lol-community-images-prod` |
| 키 prefix | `local/` | `prod/` |
| CloudFront | dev 배포 | prod 배포 |
| 크리덴셜 | 개발자 IAM 사용자 (`~/.aws/credentials`) | ECS Task Role |
| Lifecycle | **30일 후 전량 만료** | 없음 |

#### 격리를 버킷으로 하는 이유 (prefix 분리가 아니라)

같은 버킷 안에서 prefix 로만 나누면, 로컬 크리덴셜이 **운영 객체에 접근할 권한을 물리적으로 갖는다.**
IAM 조건(`s3:prefix`)으로 좁힐 수는 있지만, 정책 한 줄만 잘못 써도 개발자 노트북의 키가 운영
이미지를 지울 수 있는 구조가 된다. 버킷을 나누면 **로컬 IAM 정책에 운영 버킷 ARN 자체가 등장하지 않는다** —
실수의 여지가 정책 실수에서 "존재하지 않는 권한"으로 바뀐다.

버킷 분리가 부수적으로 주는 것들:
- **dev 버킷에만 Lifecycle 30일 만료**를 걸 수 있다. 여러 개발자가 각자 로컬 DB 로 붙으면 내 DB 에 없는
  남의 파일은 정리 배치가 지우지 못해 dev 버킷에 고아가 쌓이는데, Lifecycle 이 이를 자동 청소한다.
  운영에는 절대 걸면 안 되는 규칙이라 버킷이 같으면 이 설정을 쓸 수 없다.
- dev 버킷은 통째로 비우거나 지워도 된다.
- 비용·용량 지표가 환경별로 분리돼 보인다.

#### 그럼에도 키 prefix 에 환경을 넣는다

```
{env}/community/{yyyy}/{MM}/{uuid}.{ext}
   ↑ local | prod
```

버킷이 이미 갈렸는데 prefix 까지 넣는 건 **설정 실수에 대한 이중 안전장치**다. 누군가 로컬 설정에
운영 버킷 이름을 넣더라도 객체가 `local/` 아래로 떨어져 운영 데이터와 섞이지 않고, 로그·콘솔에서
어느 환경이 만든 객체인지 즉시 식별된다. 비용은 문자열 몇 바이트다.

#### 크리덴셜 — 코드는 동일, 해석만 환경이 다르다

`S3Client` 는 프로파일 분기 없이 단일 빈이고 `DefaultCredentialsProvider` 를 쓴다.
운영에서는 ECS Task Role 을, 로컬에서는 `~/.aws/credentials` 프로필(또는 `.env` 의 액세스 키)을
같은 체인이 알아서 해석한다. 코드에 `if (local)` 이 등장하지 않는다.

로컬 개발자용 IAM 정책은 **dev 버킷 ARN 에 대해서만** `PutObject`·`GetObject`·`DeleteObject` 를 허용한다.

#### dev 에도 CloudFront 를 둔다

dev 버킷을 퍼블릭으로 열면 손쉽지만, 그러면 **"버킷 비공개 + OAC" 구성이 로컬에서 한 번도 검증되지 않는다** —
이 설계에서 가장 틀리기 쉬운 부분이 정확히 그 지점이다. 트래픽이 없는 배포의 CloudFront 비용은 사실상 0 이므로
dev 배포를 따로 만들어 URL 구조까지 운영과 같게 맞춘다.

> **트레이드오프(수용).** 이 결정으로 **로컬 개발에 AWS 크리덴셜이 필수**가 된다. 신규 개발자 온보딩에
> AWS 계정·프로필 설정 단계가 추가되고, 오프라인에서는 이미지 업로드 기능을 띄울 수 없다.
> 파일시스템 어댑터가 주던 "AWS 없이 바로 실행"을 포기하는 대신, 운영 반영 전에 실제 경로를 검증하는 쪽을 택했다.

#### 테스트는 S3 를 치지 않는다

**로컬 실행(`bootRun`)과 테스트(`gradle test`)는 다른 이야기다.** 단위·서비스 테스트는 `ImageStoragePort` 를
fake 구현으로 대체한다 — 포트를 둔 이유가 여기서 값을 낸다. CI 는 AWS 크리덴셜 없이 돌아야 하므로
테스트가 실제 버킷을 치면 안 된다. S3 어댑터 자체의 검증이 필요하면 LocalStack testcontainer 를 쓰되,
`@Tag("integration")` 으로 분리해 기본 `test` 태스크에서 제외한다.

> ⚠️ **URL 을 DB 에 저장할지, 키만 저장할지**: `storage_key`(불변)와 `url`(파생) 둘 다 저장하되, **응답에는 `storage_key` + 설정된 base URL 로 조합한 값**을 쓴다. CDN 도메인이 바뀌어도 기존 행을 마이그레이션하지 않아도 되게 하기 위해서다. `url` 컬럼은 감사·디버깅용 스냅샷.

### D4. 이미지 ↔ 게시글 연결 — 별도 테이블 + 2-phase 확정

본문 문자열에 URL 만 남기고 테이블을 두지 않으면, ① 어떤 파일이 어느 글 소유인지 알 수 없고 ② 글 삭제 시 스토리지가 영원히 자란다. 그래서 `community_image` 테이블을 둔다.

```
① POST /api/community/images  (multipart)
      → row INSERT(status=UPLOADING) → S3 PUT → status=PENDING
      → {imageId, url}                    ※ 이 시점에 DB 행이 이미 존재한다
② 클라이언트가 본문에 URL 삽입
③ POST/PUT /api/community/posts  body 에 imageIds:[…] 동봉
      → 소유자 검증 후 status=ATTACHED, post_id 세팅   ※ S3 재조회 없음, UPDATE 뿐
④ 글 수정으로 빠진 이미지 → status=DETACHED
⑤ 정리 배치: UPLOADING(1h) / PENDING(24h) / DETACHED(7일) → 스토리지 삭제 + row 삭제
```

**이미지 행은 글보다 먼저 생긴다.** 업로드 API 가 이미 `community_image` 를 INSERT 하므로,
글 저장 단계에서 하는 일은 **기존 행의 두 컬럼을 UPDATE** 하는 것뿐이다. 크기·MIME·width/height 는
업로드 때 서버가 디코드하면서 이미 알고 있던 값이라 그때 함께 저장한다 — 글 저장 시점에
S3 로 `HeadObject` 를 날려 되물을 이유가 없고, 애초에 S3 는 "누가 올렸는지 / 어느 글 소유인지"를 모른다.

```sql
-- 글 저장 시 실행되는 전부. S3 호출 0회.
UPDATE community_image
   SET post_id = :postId, status = 'ATTACHED', updated_at = now()
 WHERE id IN (:imageIds)
   AND member_id = :memberId    -- 남의 이미지 도용 차단
   AND status = 'PENDING';      -- 이미 다른 글에 붙은 이미지 재사용 차단
```

**왜 `imageIds` 를 클라이언트가 보내는가 (본문 파싱 대신).** 본문에서 URL 을 정규식으로 긁는 방식은 마크다운/HTML/에디터 방언에 따라 깨지고, 외부 URL 과 우리 URL 을 구분하는 로직이 또 필요해진다. 클라이언트가 자기가 업로드한 id 를 그대로 돌려주는 게 정확하고 검증도 쉽다(소유자·상태만 보면 된다).

**본문과 `imageIds` 가 어긋나면?** 서버는 "본문에 실제로 쓰였는지"까지 강제하지 않는다. `imageIds` 에 있으나 본문에 없는 이미지는 그냥 ATTACHED 로 남아 다음 수정 때 정리되고, 반대로 본문에만 있고 목록에 없으면 PENDING 인 채로 24시간 뒤 파일이 사라져 깨진 이미지가 된다. **후자가 실질 리스크**이므로 정리 유예를 24시간으로 넉넉히 두고, 프론트엔드 통합 시 반드시 확인할 항목으로 9절에 남긴다.

### D5. 삭제 정책 — 즉시 삭제하지 않는다

게시글은 soft delete(`deleted=true`)다. 글을 지웠다고 파일을 즉시 지우면 복구 요청에 대응할 수 없고, 삭제 직후 캐시/CDN 에 남은 참조가 404 를 뿜는다. 그래서:

- 글 삭제 → 이미지 `status=DETACHED` 로만 전이 (파일 유지)
- 유예기간 경과 후 배치가 실제 삭제
- 사용자가 에디터에서 명시적으로 삭제한 **PENDING** 이미지는 `DELETE /api/community/images/{id}` 로 즉시 삭제 허용(본인 + PENDING 한정)

### D6. 이미지 정규화 — 상한 리사이즈 + EXIF 제거

- 최대 폭 **1920px** 초과 시 축소(비율 유지). 원본 그대로 두면 4000px 사진이 목록에서 그대로 내려간다.
- **EXIF 전량 제거** — 스마트폰 사진의 GPS 좌표가 그대로 공개되는 건 개인정보 사고다. `ImageIO` 로 디코드→리인코드하면 부수적으로 제거된다.
- 리인코드는 GIF 를 제외하고 적용(애니메이션 GIF 는 `ImageIO` 로 재인코딩하면 첫 프레임만 남는다 → GIF 는 크기 검증만 하고 원본 저장).

> **구현 노트 — WebP 도 재인코딩에서 빠진다.** JDK 표준 `ImageIO` 에는 WebP 리더/라이터가 아예 없다.
> 설계대로 "GIF 만 예외"로 두면 WebP 업로드는 전부 `IMAGE_INVALID` 로 떨어졌을 것이다. GIF 와 같이
> 원본 그대로 저장한다. **대가는 GIF/WebP 의 EXIF 가 남는다는 것**이다 — JPEG(스마트폰 사진, GPS 가
> 실제로 붙는 경로)는 항상 재인코딩되므로 위험의 대부분은 덮이지만, 완전하지는 않다. 필요해지면
> 메타데이터만 제거하는 경로를 따로 붙인다.
>
> 이 때문에 크기를 **헤더에서 직접 파싱**한다(`ImageDimensionReader`: JPEG SOF / PNG IHDR /
> GIF 헤더 / WebP VP8·VP8L·VP8X). 폭탄 이미지 가드가 "디코드 전"에 서야 한다는 요구와도 맞물린다 —
> `ImageIO.read()` 로 크기를 재면 이미 늦다.
- 썸네일 세트는 Phase 1 에서 만들지 않는다. 필요해지면 CloudFront 앞단의 이미지 리사이즈(Lambda@Edge 또는 CloudFront Functions + S3 Object Lambda)로 붙이는 편이, 업로드 시점에 N 종을 굽는 것보다 운영이 편하다.

### D7. 고아 판별 — S3 를 스캔하지 않는다, DB 가 진실원천

버킷을 `ListObjects` 로 훑어 DB 와 대조하는 방식은 쓰지 않는다. 객체 수에 비례해 비싸고,
무엇보다 **"방금 올라간 정상 파일"과 "고아"를 구분할 수 없다** — 목록에는 둘 다 그냥 객체로 보인다.
대신 상태와 경과시간만 보면 된다.

```sql
-- ① 업로드 도중 끊김(S3 PUT 실패 / 앱 크래시)
WHERE status = 'UPLOADING' AND updated_at < now() - interval '1 hour'
-- ② 글을 쓰다 이탈
WHERE status = 'PENDING'   AND updated_at < now() - interval '24 hours'
-- ③ 글에서 떨어져 나옴(글 삭제 / 수정으로 제외)
WHERE status = 'DETACHED'  AND updated_at < now() - interval '7 days'
```

`idx_ci_status_updated` 인덱스가 정확히 이 세 스캔을 위해 존재한다. 걸린 행의 `storage_key` 를 모아
S3 `DeleteObjects`(배치 삭제) 후 row 를 지운다.

**판정 기준이 `created_at` 이 아니라 `updated_at` 인 이유.** DETACHED 는 "떨어져 나온 시점"부터
유예를 세야 한다. 1년 전에 올린 이미지를 오늘 글에서 뺐는데 `created_at` 으로 재면 즉시 삭제 대상이 된다.
UPLOADING·PENDING 은 두 값이 같으므로 `updated_at` 하나로 통일하면 인덱스도 하나면 된다.

#### 이 판별이 성립하려면 DB 가 S3 의 상위집합이어야 한다

S3 PUT 을 먼저 하고 DB INSERT 를 나중에 하면, **PUT 성공 후 INSERT 실패**한 파일은 DB 에 흔적이 없어
위 쿼리로 영영 잡히지 않는다. 버킷에 조용히 쌓이기만 한다. 그래서 순서를 뒤집는다.

```
1. INSERT (status='UPLOADING', storage_key = 앱이 미리 생성한 UUID 키)
2. S3 PUT
3. UPDATE status='PENDING'
```

이러면 **S3 에 있는 파일은 반드시 DB 에 행이 있다.** 2·3단계에서 죽어도 `UPLOADING` 으로 남아 배치가
잡고, S3 에 실제로 파일이 없더라도 `DeleteObject` 는 멱등이라 그냥 지우면 된다. 대가는 DB 왕복 1회
추가인데 업로드 빈도를 생각하면 무시할 수준이다.

> `UPLOADING` 구간에는 "행은 있는데 파일이 없는" 상태가 존재한다. 이를 **상태로 명시**했기 때문에
> 모호함이 아니라 정상적인 중간 상태가 된다 — 이 상태의 행은 조회 API 에 절대 노출되지 않는다.

### D8. 정리 배치 — `@Scheduled` (Spring Batch 아님)

기존에 `DuoPostExpirationScheduler` 가 **컴포지션 루트(`app/application/config`)에 얇은 트리거만 두고 도메인 UseCase 를 호출**하는 패턴을 쓰고 있다. 동일하게 간다.

```java
// module/app/application/config/OrphanImageCleanupScheduler.java
@Scheduled(cron = "0 30 4 * * *")   // 매일 04:30
public void cleanupOrphanImages() {
    imageCleanupUseCase.cleanupOrphans();
}
```

Spring Batch 메타 테이블(V27)이 있긴 하나, 하루 수십~수백 행 삭제에 잡 인프라를 쓸 이유가 없다. 다중 인스턴스로 확장되면 Redisson 분산 락(이미 `championstats` 에서 쓰는 인프라)으로 단일 실행을 보장한다.

**삭제 순서도 S3 가 먼저, row 가 나중이다.** 업로드(D7)와 같은 이유로 방향만 반대다 — row 를 먼저 지우면 S3 delete 가 실패했을 때 파일이 추적 불가능한 고아로 남는다. S3 를 먼저 지우면 실패해도 row 가 남아 다음 배치가 재시도하고, 이미 없는 객체에 대한 `DeleteObject` 는 멱등이라 재시도가 안전하다.

---

## 4. 데이터 모델

`lol-db-schema` 서브모듈에 **V36__add_community_image.sql** 로 추가한다.

```sql
-- 커뮤니티 게시글 본문 이미지.
--
-- post_id 가 NULL 을 허용하는 이유: 업로드는 글 저장보다 먼저 일어난다(에디터에서
-- 이미지를 넣은 뒤 글을 쓰다 만 상태). 이 구간의 행이 status=PENDING 이고,
-- 유예기간이 지나면 정리 배치가 스토리지와 함께 지운다.
--
-- 행은 S3 PUT '전에' 먼저 INSERT 된다(status=UPLOADING). 그래야 DB 가 항상 S3 의
-- 상위집합이 되어, 버킷에만 존재하고 DB 가 모르는 파일이 생기지 않는다.
CREATE TABLE community_image (
    id           BIGINT        NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    member_id    BIGINT        NOT NULL,
    post_id      BIGINT        NULL,
    -- 스토리지 내 불변 키. URL 은 CDN 도메인 변경에 따라 바뀔 수 있으므로
    -- 참조의 진실원천은 이 컬럼이다.
    storage_key  VARCHAR(512)  NOT NULL,
    -- 발급 당시 URL 스냅샷(감사/디버깅용). 조회 응답은 storage_key + base-url 로 조합한다.
    url          VARCHAR(1024) NOT NULL,
    content_type VARCHAR(50)   NOT NULL,
    size_bytes   BIGINT        NOT NULL,
    width        INT           NULL,
    height       INT           NULL,
    -- UPLOADING / PENDING / ATTACHED / DETACHED. duo_post.status 와 같이 문자열로
    -- 저장하고 도메인 enum 매핑은 애플리케이션이 담당한다(값 추가 시 마이그레이션 불필요).
    status       VARCHAR(20)   NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_community_image PRIMARY KEY (id),
    CONSTRAINT fk_ci_member FOREIGN KEY (member_id) REFERENCES member (id),
    -- ON DELETE 없음: 게시글은 soft delete 이므로 물리 삭제가 발생하지 않는다.
    CONSTRAINT fk_ci_post FOREIGN KEY (post_id) REFERENCES community_post (id),
    -- 같은 키로 두 행이 생기면 정리 배치가 남의 파일을 지운다.
    CONSTRAINT uq_ci_storage_key UNIQUE (storage_key)
);

-- 게시글 상세/수정 시 첨부 목록 조회.
CREATE INDEX idx_ci_post ON community_image (post_id) WHERE post_id IS NOT NULL;

-- 정리 배치: status + 경과시간으로 스캔한다. created_at 이 아니라 updated_at 인 이유는
-- DETACHED 유예를 "떨어져 나온 시점"부터 세야 하기 때문이다(D7). UPLOADING·PENDING 은
-- 두 값이 같으므로 이 인덱스 하나로 세 조회가 모두 커버된다.
CREATE INDEX idx_ci_status_updated ON community_image (status, updated_at);

-- 회원별 업로드 조회(남용 추적·마이페이지 확장 대비).
CREATE INDEX idx_ci_member_created ON community_image (member_id, created_at DESC);

COMMENT ON TABLE community_image IS '커뮤니티 게시글 본문 이미지';
```

### 도메인 상태 전이

```
   업로드 API 진입
          │  ① INSERT (S3 PUT 전)
          ▼
     ┌───────────┐  ② S3 PUT 성공   ┌─────────┐  글 저장 시    ┌──────────┐
     │ UPLOADING │ ───────────────▶ │ PENDING │ ────────────▶ │ ATTACHED │
     └───────────┘                  └─────────┘  imageIds 포함 └──────────┘
          │                              │                          │
          │ 1h 경과                       │ 24h 경과                  │ 글 삭제 /
          │ (PUT 실패·크래시)              │ or 본인 삭제              │ 수정으로 제외
          ▼                              ▼                          ▼
      (물리 삭제)                     (물리 삭제)               ┌──────────┐
                                                              │ DETACHED │
                                                              └──────────┘
                                                                   │ 7일 경과
                                                                   ▼
                                                              (물리 삭제)
```

`UPLOADING` 은 조회 API 에 절대 노출되지 않는다 — 파일이 아직 없을 수 있는 유일한 구간이다.

전이 규칙은 `PostImage` 도메인이 직접 강제한다(서비스에서 `if + throw` 금지 — 프로젝트 컨벤션).

```java
public void attachTo(Long postId, Long requesterId) {
    validateOwner(requesterId);          // 남의 이미지를 내 글에 붙이지 못하게
    validateStatus(ImageStatus.PENDING); // 이미 다른 글에 붙은 이미지 재사용 차단
    this.postId = postId;
    this.status = ImageStatus.ATTACHED;
}
```

---

## 5. API 계약

### 5.1 이미지 업로드

```
POST /api/community/images
Content-Type: multipart/form-data
Authorization: Bearer <access-token>       (SecurityConfig 상 자동 인증 필수)

file=<binary>
```

**201 Created**

```json
{
  "result": "SUCCESS",
  "data": {
    "imageId": 1042,
    "url": "https://cdn.example.com/community/2026/08/9f2c….webp",
    "width": 1920,
    "height": 1080,
    "sizeBytes": 481203
  }
}
```

**에러**

| 상황 | 상태 | ErrorType (신규) |
|---|---|---|
| 파일 없음/빈 파일 | 400 | `IMAGE_FILE_REQUIRED` |
| 용량 초과 | 400 | `IMAGE_SIZE_EXCEEDED` |
| 허용되지 않는 형식 | 400 | `IMAGE_TYPE_NOT_SUPPORTED` |
| 손상된 이미지 / 디코드 실패 | 400 | `IMAGE_INVALID` |
| 분당 업로드 한도 초과 | 429 | `IMAGE_UPLOAD_RATE_LIMITED` |
| 스토리지 장애 | 500 | `IMAGE_STORAGE_FAILED` |
| 없는 이미지 id (글 저장/삭제 시) | 404 | `IMAGE_NOT_FOUND` |
| 이미 다른 글에 붙었거나 정리된 이미지 | 400 | `IMAGE_NOT_ATTACHABLE` |
| 글당 첨부 상한 초과 | 400 | `IMAGE_COUNT_EXCEEDED` |

> `ErrorType` 은 `module/common/error/ErrorType.java` 에 커뮤니티 블록과 나란히 추가한다. 현재 `ErrorCode` enum 은 `E400/E401/E403/E404/E409/E500/E503` 만 있으므로 **`E429` 를 함께 추가**해야 한다.

> ⚠️ **용량 초과가 500 으로 떨어지는 함정.** `spring.servlet.multipart.max-file-size` 를 넘기면 Spring 이 `MaxUploadSizeExceededException` 을 던지는데, 현재 `CoreExceptionAdvice` 에는 이 핸들러가 없어 `exception(Exception)` 폴백이 잡아 **500 `DEFAULT_ERROR`** 를 반환한다. 사용자에게는 "알 수 없는 오류"로 보인다. 아래 핸들러를 함께 추가해야 400 `IMAGE_SIZE_EXCEEDED` 가 나간다.
>
> ```java
> @ExceptionHandler
> public ResponseEntity<ApiResponse<ErrorMessage>> maxUploadSize(
>         MaxUploadSizeExceededException e) {
>     return ResponseEntity.status(HttpStatus.BAD_REQUEST)
>             .body(ApiResponse.error(ErrorType.IMAGE_SIZE_EXCEEDED));
> }
> ```
>
> 앱 레벨 검증(6절 1단계)과 이중이 되는 게 아니라, **앱 코드에 도달하기 전에** 톰캣/Spring 이 끊는 경로를 덮는 것이다.

### 5.2 이미지 삭제 (에디터에서 뺀 경우)

```
DELETE /api/community/images/{imageId}
→ 204 No Content
```

본인 소유 + `PENDING` 상태만 허용. `ATTACHED` 는 글 수정 API 를 통해서만 떨어진다(403 `FORBIDDEN`).

### 5.3 게시글 생성/수정 — 기존 API 확장

```diff
 POST /api/community/posts
 {
   "title": "…",
   "content": "…![img](https://cdn…/9f2c….webp)…",
-  "categoryId": 3
+  "categoryId": 3,
+  "imageIds": [1042, 1043]
 }
```

- `imageIds` 는 **선택 필드** — 기존 클라이언트가 깨지지 않는다.
- `@Size(max = 10)` — 글당 이미지 상한(진실원천은 `community.image.max-count-per-post`, 어노테이션은 상수라 값이 중복된다).
- 수정(`PUT`)은 **전체 교체 시맨틱**: 목록에 없는 기존 첨부는 DETACHED 로 전이.

> **구현 노트 — 수정 시 `null` 과 `[]` 를 구분한다.**
> `imageIds` 필드가 **아예 없으면** 첨부를 건드리지 않고, **빈 배열이면** 전부 해제한다.
> 둘을 같게 취급하면(둘 다 "빈 목록") 제목만 고치는 화면이나 이 필드를 모르는 구 클라이언트가
> PUT 을 보내는 순간 사용자가 손대지 않은 이미지가 조용히 떨어져 나가 본문이 즉시 깨진다.
> 설계 문서가 "요청에 없는"을 목록 안의 원소 기준으로만 정의해 필드 부재는 미정의였고,
> 한쪽 해석이 데이터 손실이라 안전한 쪽을 택했다. **프론트엔드는 목록을 보낼 때 유지할 이미지를
> 모두 담아야 한다** — 그래서 상세 응답이 현재 첨부 목록을 함께 내려준다.

응답 `PostResponse` 에 첨부 목록을 추가할지는 선택이다. 본문에 URL 이 이미 들어 있으므로 렌더링에는 불필요하지만, **수정 화면이 "현재 첨부 목록"을 알아야 하므로 추가하는 쪽을 권장**한다.

```diff
 PostResponse {
   …,
+  "images": [{ "imageId": 1042, "url": "https://…" }]
 }
```

---

## 6. 처리 흐름

### 업로드

```
CommunityImageController
  │  MultipartFile → 원본 바이트 + 선언 content-type
  ▼
ImageService.upload(memberId, UploadImageCommand)
  ├─ 1. rate limit 검사       (Redis INCR + TTL 60s)
  ├─ 2. 용량 검증            (설정값, 기본 5MB)
  ├─ 3. 매직바이트로 실제 타입 판별  ← 클라이언트가 보낸 Content-Type 은 신뢰하지 않는다
  ├─ 4. 화이트리스트 대조     (jpeg/png/gif/webp — SVG 는 명시적 금지)
  ├─ 5. 디코드 + 상한 리사이즈 + EXIF 제거   (GIF 는 건너뜀)
  ├─ 6. storageKey 생성 + ImagePersistencePort.save(PostImage.uploading(...))
  ├─ 7. ImageStoragePort.store(bytes, type, storageKey)
  └─ 8. PostImage.markUploaded() → status=PENDING
  ▼
201 { imageId, url, width, height }
```

**저장 키 규칙**

```
community/{yyyy}/{MM}/{uuid}.{ext}
```

원본 파일명을 절대 쓰지 않는다 — 경로 조작(`../`), 한글/특수문자 인코딩 문제, 파일명으로 인한 정보 노출을 한 번에 없앤다.

**순서 주의 ① — DB INSERT(6)가 S3 PUT(7)보다 먼저다.** 반대로 하면 PUT 성공 후 INSERT 가 실패한 파일이 DB 에 흔적 없이 버킷에만 남아, 정리 배치가 영영 찾지 못한다. 이 순서라면 DB 가 항상 S3 의 상위집합이 되어 모든 파일이 추적된다 (D7).

**순서 주의 ② — S3 PUT 을 DB 트랜잭션 안에서 호출하지 않는다.** 6·8 은 각각 짧은 트랜잭션으로 커밋하고, 7 은 트랜잭션 **밖**에서 실행한다. 외부 네트워크 호출이 트랜잭션을 잡고 있으면 S3 지연이 그대로 DB 커넥션 점유 시간이 되고, 업로드가 몰릴 때 커넥션 풀이 먼저 마른다.

**rate limit 을 앞으로 올린 이유(1번)**: 디코드·리사이즈는 이 파이프라인에서 가장 비싼 단계다. 한도를 넘긴 요청에 CPU 를 쓰고 나서 거절하면 방어의 의미가 없다.

### 글 저장 시 확정

```
PostService.createPost(memberId, command)
  ├─ 기존: 카테고리 검증 → Post.create → save
  └─ 추가: imageIds 가 있으면
           ImageAttachUseCase.attach(memberId, postId, imageIds)
             └─ 각 PostImage.attachTo(postId, memberId)  ← 소유자·상태 검증은 도메인이
```

`PostService` 가 `ImageUseCase`(같은 컨텍스트의 port.in)를 주입받는다. 같은 컨텍스트 내부이므로 아키텍처 규칙 위반이 아니고, 트랜잭션도 하나로 묶인다.

**수정 시 차집합 처리**

```
기존 ATTACHED 집합 A, 요청 imageIds 집합 B
  B \ A → PENDING 검증 후 ATTACHED
  A \ B → DETACHED
```

---

## 7. 보안 체크리스트

| 항목 | 조치 |
|---|---|
| **Content-Type 위조** | 클라이언트 헤더 무시, 매직바이트(파일 시그니처)로 판별 |
| **SVG 업로드** | **금지**. SVG 는 스크립트를 담을 수 있어 같은 도메인에서 서빙되면 저장형 XSS 가 된다 |
| **폭탄 이미지(decompression bomb)** | 디코드 전에 헤더의 width×height 로 픽셀 수 상한(예: 50MP) 검사 |
| **용량** | 앱 레벨 검증 + `spring.servlet.multipart.max-file-size` 이중 방어 |
| **경로 조작** | 원본 파일명 미사용, UUID 키 |
| **MIME 스니핑** | CloudFront 응답에 `X-Content-Type-Options: nosniff`, `Content-Type` 은 저장 시 확정값 고정 |
| **남용/스팸** | 회원당 분당 업로드 수 제한(Redis 카운터). 미인증 업로드는 SecurityConfig 가 이미 차단 |
| **개인정보** | EXIF(GPS 포함) 전량 제거 |
| **타 회원 이미지 도용** | `attachTo` 에서 업로더 == 작성자 검증 |
| **버킷 노출** | 퍼블릭 액세스 차단 + CloudFront OAC. 쓰기 권한은 앱 IAM 역할만 |
| **로컬이 운영 데이터를 건드림** | 버킷 분리 — 개발자 IAM 정책에 운영 버킷 ARN 이 아예 없다. 키 prefix(`local/`·`prod/`)로 이중 방어 (D3) |
| **개발자 액세스 키 유출** | `~/.aws/credentials` 프로필 권장(레포 밖). `.env` 를 쓸 경우 `.gitignore` 확인 필수. dev 버킷 전용 권한이라 유출돼도 운영 영향 없음 |

---

## 8. 구현 순서 (PR 분할)

각 PR 은 독립적으로 머지 가능하도록 쪼갰다.

| # | 범위 | 산출물 | 상태 |
|---|---|---|---|
| **0** | 인프라 준비 (코드 아님) | **버킷 2벌**(dev/prod) + CloudFront 2벌 + ECS Task Role + 개발자 IAM 사용자, dev 버킷 Lifecycle 30일, 배포 환경변수 | ⬜ **미착수 — 유일한 블로커** |
| **1** | 스키마 | `lol-db-schema` V36 + 서브모듈 포인터 갱신 | ✅ 파일 작성, 서브모듈 PR 머지 대기 |
| **2** | 스토리지 포트/어댑터 | `S3Config`(common), `ImageStoragePort`, `S3ImageStorageAdapter`(단일), `ImageProcessorPort`+`DefaultImageProcessor`, `StorageProperties` | ✅ |
| **3** | 도메인 + 영속성 | `PostImage`, `ImageStatus`, 엔티티/리포지토리/매퍼/어댑터 | ✅ `PostImageTest`, `ImagePersistenceAdapterTest` |
| **4** | 업로드 API | `ImageService`, `CommunityImageController`, `ErrorType`+`ErrorCode.E429`, `CoreExceptionAdvice` 의 `MaxUploadSizeExceededException` 핸들러 | ✅ `ImageServiceTest`, RestDocs |
| **5** | 게시글 연동 | `CreatePostRequest/UpdatePostRequest.imageIds`, `PostService` attach/replace/detach, `PostResponse.images` | ✅ `PostServiceTest` 확장 |
| **6** | 정리 배치 | `ImageCleanupUseCase`/`ImageCleanupService`, `OrphanImageCleanupScheduler` | ✅ `ImageCleanupServiceTest` |
| **7** | 문서 | 이 문서 갱신, RestDocs 스니펫 | ✅ (`docs/ARCHITECTURE.md` 는 제외 — 옛 레이어 구조 기준이라 이 변경만 얹을 수 없다) |

`./gradlew test archTest checkstyleMain checkstyleTest` 전부 통과한다(Docker 불필요).

> **구현 노트 — `TestJpaConfig` 에 제외 필터 3개를 추가해야 했다.** `@DataJpaTest` 슬라이스가
> `com.example.lolserver` 전체를 스캔하는데, 새 driven 어댑터 세 개가 각각 `S3Client` /
> `CommunityImageProperties` / `StringRedisTemplate` 를 요구해 **커뮤니티의 기존 리포지토리 테스트가
> 전부 컨텍스트 로딩에서 죽었다.** 기존 `client`/`messaging`/`cache`/`oauth` 와 같은 이유·같은 방식으로
> `adapter.out.storage`·`adapter.out.image`·`adapter.out.ratelimit` 를 제외했다.
> 새 driven 어댑터 패키지를 만들 때마다 반복될 함정이다.

### 추가되는 설정

```yaml
# api-local.yml / api-prod.yml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 6MB

community:
  image:
    max-size-bytes: 5242880
    max-count-per-post: 10
    max-pixels: 50000000
    max-width: 1920
    allowed-types: image/jpeg,image/png,image/gif,image/webp
    upload-rate-per-minute: 10
    uploading-retention-hours: 1
    pending-retention-hours: 24
    detached-retention-days: 7

# storage 는 프로파일별로 값만 다르다. provider 스위치도, 어댑터 분기도 없다.
storage:
  s3:
    bucket: ${S3_BUCKET}
    region: ${AWS_REGION}
    key-prefix: ${STORAGE_KEY_PREFIX}   # local | prod — 설정 실수에 대한 이중 안전장치
    base-url: ${CDN_BASE_URL}
```

프로파일별 실제 값:

```yaml
# api-local.yml — 개발자는 .env 로 덮어쓸 수 있다
storage:
  s3:
    bucket: ${S3_BUCKET:lol-community-images-dev}
    region: ${AWS_REGION:ap-northeast-2}
    key-prefix: local
    base-url: ${CDN_BASE_URL:https://dev-cdn.example.com}

# api-prod.yml — 전부 환경변수 주입, 기본값 없음
storage:
  s3:
    bucket: ${S3_BUCKET}
    region: ${AWS_REGION}
    key-prefix: prod
    base-url: ${CDN_BASE_URL}
```

> `api-prod.yml` 에 기본값(`:`)을 두지 않는 이유는 기존 파일의 관례와 같다 — 운영 설정이 빠지면
> 조용히 잘못된 버킷을 쓰는 대신 **부팅이 실패해야** 한다. 반대로 로컬은 기본값을 둬서
> `.env` 없이도 팀 공용 dev 버킷으로 바로 붙는다.

**로컬 크리덴셜.** 코드가 `DefaultCredentialsProvider` 를 쓰므로 아래 중 아무거나면 된다.

```bash
# ① ~/.aws/credentials 프로필 (권장 — 키가 레포 근처에 안 온다)
export AWS_PROFILE=lol-dev

# ② 루트 .env (bootRun 이 자동으로 읽어 환경변수로 주입한다)
AWS_ACCESS_KEY_ID=…
AWS_SECRET_ACCESS_KEY=…
AWS_REGION=ap-northeast-2
```

### 추가되는 의존성

```gradle
// module/common/build.gradle  (S3Config 용)
implementation platform('software.amazon.awssdk:bom:2.28.16')
implementation 'software.amazon.awssdk:s3'

// module/domain/community/build.gradle
implementation platform('software.amazon.awssdk:bom:2.28.16')
implementation 'software.amazon.awssdk:s3'          // S3ImageStorageAdapter
implementation 'org.apache.tika:tika-core:2.9.2'    // 매직바이트 타입 판별
implementation 'org.springframework.boot:spring-boot-starter-data-redis'  // rate limit
```

> `common` 과 `community` 양쪽에 SDK 를 선언하는 건 `implementation` 이 전이되지 않는 이 레포의 기존 패턴(`match`/`championstats` 의 redis 선언)과 같다.

### ArchUnit 규칙 (추가 완료)

```java
@Test
void domain과_application은_스토리지_이미지_SDK에_의존하지_않는다() {
    noClasses()
        .that().resideInAnyPackage("..community.domain..", "..community.application..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("software.amazon.awssdk..", "org.apache.tika..",
                "javax.imageio..", "java.awt..")
        .check(classes);
}
```

`javax.imageio`·`java.awt` 를 함께 막는 이유는, 리사이즈 코드가 "잠깐만" 서비스로 올라오는 게
가장 흔한 새는 경로이기 때문이다.

---

## 9. 확정 필요 사항

코드는 이 답들과 무관하게 완성돼 있다 — 아래는 **뜨기 전에 사람이 정해야 하는 것**들이다.
1~3 은 인프라(PR 0)라 답이 없으면 로컬에서도 실행되지 않고, 4~7 은 답이 늦어도 코드가 기다려 준다.
괄호 안은 답이 없을 때 진행할 기본값.

1. **버킷·CloudFront 를 2벌(dev/prod) 만들 수 있는가?** 계정·비용·IaC 관리 주체 확인. dev 배포까지 두는 이유는 D3 참고. (기본값: 만들 수 있다고 보고 PR 0 을 인프라 작업으로 분리)
2. **개발자 IAM 사용자를 어떻게 발급·회수하는가?** 인원이 늘면 개인별 사용자 대신 SSO/AssumeRole 이 낫다. (기본값: 팀 공용 dev 전용 IAM 사용자 1개)
3. **dev 버킷을 개발자끼리 공유하는가, 개인별로 나누는가?** 공유해도 정리 배치는 자기 DB 기준이라 서로를 지우지 않는다. 남는 고아는 Lifecycle 30일이 청소한다. 개인별로 나눈다면 키 prefix 를 `local/{개발자}/…` 로 한 단계 더 쪼개면 된다. (기본값: 공유 + Lifecycle)
4. **에디터가 마크다운인가 HTML(WYSIWYG)인가?** 서버 설계는 동일하지만, HTML 이면 본문 sanitize(XSS) 정책이 **이 설계 밖에서** 별도로 필요하다. (기본값: 마크다운 가정)
5. **`imageIds` 를 클라이언트가 보내는 계약에 프론트엔드가 동의하는가?** 본문 파싱 대안 대비 프론트 작업량이 조금 늘어난다. 특히 **수정 시 전체 교체**(목록을 보내면 빠진 것은 해제, 필드를 아예 빼면 그대로 유지)를 반드시 맞춰야 한다 — 5.3 구현 노트. (기본값: 이 계약으로 진행)
6. **댓글 이미지가 곧 필요한가?** 필요하다면 지금 `community_image` 에 `comment_id` 컬럼을 함께 넣는 편이 마이그레이션 한 번을 아낀다. (기본값: 넣지 않음)
7. **Linear 이슈 번호(MP-XXX)** — 브랜치/커밋 컨벤션상 필요하다.

---

## 10. 참고

### 구현 위치

| 관심사 | 파일 |
|---|---|
| 상태 전이 규칙 | `community/domain/PostImage.java` |
| 업로드 파이프라인·첨부 확정 | `community/application/ImageService.java` |
| 고아 정리 | `community/application/ImageCleanupService.java` + `app/config/OrphanImageCleanupScheduler.java` |
| S3 키 레이아웃·PUT/DELETE | `community/adapter/out/storage/S3ImageStorageAdapter.java` |
| 타입 판별·폭탄 가드·EXIF 제거 | `community/adapter/out/image/` (`DefaultImageProcessor`, `ImageDimensionReader`, `ImageNormalizer`) |
| 업로드 rate limit | `community/adapter/out/ratelimit/RedisImageRateLimitAdapter.java` |
| S3 클라이언트 빈 | `common/config/S3Config.java` |
| 스키마 | `lol-db-schema/db/migration/V36__add_community_image.sql` |

- `module/domain/community/ArchitectureTest` — 이 설계가 통과해야 하는 규칙
- `module/app/application/config/DuoPostExpirationScheduler` — 정리 배치가 따를 패턴
- `lol-db-schema/db/migration/V31__add_community_bookmark.sql` — 마이그레이션 주석/제약 작성 스타일
- `docs/ARCHITECTURE.md` — 모듈 의존 그래프(구현 후 갱신 대상)
