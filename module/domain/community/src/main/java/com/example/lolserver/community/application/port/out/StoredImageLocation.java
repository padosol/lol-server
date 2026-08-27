package com.example.lolserver.community.application.port.out;

/**
 * 저장 <b>전에</b> 미리 발급되는 위치. DB INSERT 가 S3 PUT 보다 먼저이므로
 * 키와 최종 URL 을 PUT 이전에 알아야 한다.
 *
 * @param storageKey 스토리지 내 불변 키
 * @param url        CDN 을 통한 영구 접근 URL(presigned 가 아니다 — 만료되면 옛 글이 깨진다)
 */
public record StoredImageLocation(String storageKey, String url) {
}
