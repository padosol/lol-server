package com.example.lolserver.community.domain.vo;

/**
 * 커뮤니티 이미지의 생명주기 상태.
 *
 * <pre>
 *   업로드 API 진입
 *          │  ① INSERT (S3 PUT 전)
 *          ▼
 *     UPLOADING ──② PUT 성공──▶ PENDING ──글 저장(imageIds)──▶ ATTACHED
 *          │                      │                              │
 *          │ 1h 경과               │ 24h 경과 / 본인 삭제           │ 글 삭제·수정으로 제외
 *          ▼                      ▼                              ▼
 *      (물리 삭제)             (물리 삭제)                     DETACHED ──7일──▶ (물리 삭제)
 * </pre>
 *
 * <p>{@link #UPLOADING} 은 "행은 있는데 파일이 아직 없을 수 있는" 유일한 구간이다.
 * 이를 모호함이 아니라 <b>명시적 상태</b>로 둔 덕분에 정리 배치가 실패한 업로드를 잡을 수 있고,
 * 조회 API 에는 절대 노출되지 않는다.
 */
public enum ImageStatus {
    UPLOADING,
    PENDING,
    ATTACHED,
    DETACHED
}
