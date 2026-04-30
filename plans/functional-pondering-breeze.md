# 소셜 로그인 연동(Account Linking) 프로세스 정리

## 지원 Provider
- **Google** (OIDC) — email, nickname, profileImage 제공
- **Riot RSO** (OIDC) — PUUID(sub)만 제공

---

## 1. 일반 로그인 플로우 (신규 가입 포함)

```
[프론트엔드]                    [백엔드]                         [OAuth Provider]
    │                              │                                  │
    ├─ GET /oauth2/authorize/{provider} ─→│                           │
    │                              │                                  │
    │                   LinkOAuth2AuthorizationRequestResolver        │
    │                   (link_token 없음 → 일반 로그인 모드)            │
    │                              │                                  │
    │                              ├── Authorization URL 리다이렉트 ──→│
    │                              │   (state 포함, CSRF 방지)         │
    │  ←── 302 Provider 로그인 페이지 ──┘                              │
    │                                                                 │
    ├── 사용자 로그인/동의 ──────────────────────────────────────────→│
    │                                                                 │
    │  ←── 302 /login/oauth2/code/{provider}?code=...&state=... ──────┤
    │                              │                                  │
    │                   Spring Security 자동 처리:                     │
    │                   1. code → token 교환                          │
    │                   2. CustomOidcUserService.loadUser()            │
    │                   3. OAuth2UserInfoExtractor로 사용자 정보 추출   │
    │                              │                                  │
    │                   OAuth2AuthenticationSuccessHandler:            │
    │                   - extractLinkMemberId() → null (로그인 모드)    │
    │                   - loginWithOAuthUserInfo() 호출                │
    │                     └→ findOrCreateMemberAndGenerateTokens()     │
    │                        ├ 기존 회원: lastLoginAt 업데이트          │
    │                        └ 신규 회원: Member + SocialAccount 생성   │
    │                   - JWT 토큰 생성 (access + refresh)             │
    │                   - 쿠키에 토큰 저장                              │
    │                              │                                  │
    │  ←── 302 {frontend-callback-url} ──┘                            │
```

### 핵심 파일
| 파일 | 역할 |
|------|------|
| `SecurityConfig.java` | OAuth2 로그인 필터 체인 설정 |
| `CustomOidcUserService.java` | OIDC 사용자 정보 로드 |
| `OAuth2AuthenticationSuccessHandler.java` | 로그인/연동 분기 처리 |
| `MemberAuthService.findOrCreateMemberAndGenerateTokens()` | 회원 조회/생성 + 토큰 발급 |

---

## 2. 소셜 계정 연동 플로우 (기존 회원에 추가 계정 연결)

```
[프론트엔드]                    [백엔드]                         [OAuth Provider]
    │                              │                                  │
    ├─ GET /api/members/me/social-accounts/link/{provider} ──→│       │
    │  (인증 필수: JWT 쿠키)        │                                  │
    │                              │                                  │
    │                   MemberController.initSocialAccountLink():      │
    │                   1. provider 검증 (google/riot만 허용)          │
    │                   2. linkToken 생성 (UUID, 300초 TTL, 1회용)     │
    │                      └→ SocialAccountLinkTokenStore에 저장       │
    │                   3. /oauth2/authorize/{provider}?link_token=... │
    │                              │                                  │
    │  ←── 302 /oauth2/authorize/{provider}?link_token={token} ──┘    │
    │                              │                                  │
    │                   LinkOAuth2AuthorizationRequestResolver:        │
    │                   1. link_token 파라미터 감지                     │
    │                   2. consumeToken(linkToken) → memberId 추출     │
    │                   3. AuthorizationRequest에 link_member_id 저장  │
    │                              │                                  │
    │                              ├── Authorization URL 리다이렉트 ──→│
    │  ←── 302 Provider 로그인 페이지 ──┘                              │
    │                                                                 │
    ├── 사용자 로그인/동의 ──────────────────────────────────────────→│
    │                                                                 │
    │  ←── 302 /login/oauth2/code/{provider}?code=...&state=... ──────┤
    │                              │                                  │
    │                   Spring Security 자동 처리 (위와 동일)           │
    │                              │                                  │
    │                   OAuth2AuthenticationSuccessHandler:            │
    │                   - extractLinkMemberId() → memberId (연동 모드)  │
    │                   - linkSocialAccount(memberId, userInfo) 호출   │
    │                     1. Member 존재 확인                          │
    │                     2. provider+providerId 중복 확인              │
    │                        └ 다른 회원에 이미 연동 → 예외 발생         │
    │                     3. 새 SocialAccount 생성 및 저장              │
    │                              │                                  │
    │  ←── 302 {frontend-callback-url}#linkSuccess=true ──┘           │
```

### 핵심 파일
| 파일 | 역할 |
|------|------|
| `MemberController.initSocialAccountLink()` | 연동 시작 엔드포인트 (L67-83) |
| `SocialAccountLinkTokenStore` | 1회용 link_token 생성/소비 (인메모리, 300초 TTL) |
| `LinkOAuth2AuthorizationRequestResolver` | link_token → link_member_id 변환 (L44-67) |
| `OAuth2AuthenticationSuccessHandler` | link_member_id 유무로 로그인/연동 분기 (L85-96) |
| `MemberAuthService.linkSocialAccount()` | 연동 비즈니스 로직 (L100-121) |

---

## 3. 로그인 vs 연동 — 분기 메커니즘

**핵심**: `link_member_id` 속성의 유무로 구분

| 구분 | 일반 로그인 | 소셜 계정 연동 |
|------|------------|---------------|
| 시작 엔드포인트 | `/oauth2/authorize/{provider}` | `/api/members/me/social-accounts/link/{provider}` |
| 인증 필요 여부 | 불필요 | **필수** (JWT) |
| link_token | 없음 | 있음 (300초, 1회용) |
| AuthorizationRequest 속성 | `link_member_id` 없음 | `link_member_id` = 회원 ID |
| SuccessHandler 호출 | `loginWithOAuthUserInfo()` | `linkSocialAccount()` |
| 결과 | Member 생성/조회 + 토큰 발급 | SocialAccount만 생성 |
| 리다이렉트 fragment | 없음 (쿠키에 토큰) | `#linkSuccess=true` |

---

## 4. 연동 해제 플로우

```
[프론트엔드] → DELETE /api/members/me/social-accounts/{socialAccountId}
             (인증 필수: JWT)

MemberAuthService.unlinkSocialAccount():
  1. SocialAccount 조회 (없으면 SOCIAL_ACCOUNT_NOT_FOUND)
  2. 소유권 확인 (memberId 일치 여부, 불일치 시 FORBIDDEN)
  3. SocialAccount 삭제
```

---

## 5. 토큰/상태 저장소 정리

| 저장 대상 | 저장소 | TTL | 키 패턴 |
|-----------|--------|-----|---------|
| OAuth State (CSRF) | Redis | 300초 | `oauth:state:{uuid}` |
| Link Token | 인메모리 (ConcurrentHashMap) | 300초 | UUID |
| Authorization Request | 인메모리 (ConcurrentHashMap) | 300초 | state 값 |
| Access Token | 쿠키 (HttpOnly) | 1800초 (30분) | `accessToken` |
| Refresh Token | 쿠키 (HttpOnly) + Redis | 1209600초 (14일) | `refreshToken` |

---

## 6. 보안 고려사항

1. **link_token 1회용**: `consumeToken()`으로 사용 즉시 삭제 → 재사용 불가
2. **link_token 시간 제한**: 300초 TTL → 만료 후 연동 시도 불가
3. **인증 필수**: 연동 시작(`/api/members/me/...`)은 JWT 인증된 사용자만 호출 가능
4. **중복 연동 방지**: 동일 provider+providerId가 다른 회원에 이미 연동 시 `SOCIAL_ACCOUNT_ALREADY_LINKED`
5. **State 검증**: OAuth CSRF 공격 방지 (Redis 기반)
6. **쿠키 보안**: HttpOnly, Secure(prod), SameSite=Lax

---

## 7. 도메인 모델

```
Member (1) ──── (*) SocialAccount
  - id                  - id
  - uuid                - memberId (FK)
  - email               - provider ("GOOGLE"/"RIOT")
  - nickname            - providerId (provider 고유 ID)
  - role                - email, nickname, profileImageUrl
  - lastLoginAt         - linkedAt
```

- `SocialAccount`에 **unique 제약**: `(provider, providerId)` → 하나의 소셜 계정은 하나의 회원에만 연동 가능
