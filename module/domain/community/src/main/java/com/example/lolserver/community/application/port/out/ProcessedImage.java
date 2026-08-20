package com.example.lolserver.community.application.port.out;

/**
 * 검증·정규화를 마쳐 저장해도 되는 이미지.
 *
 * @param content     저장할 바이트(JPEG/PNG 는 재인코딩되어 EXIF 가 제거된 결과)
 * @param contentType 매직바이트로 판별한 <b>실제</b> 타입. 클라이언트가 보낸 헤더가 아니다
 * @param extension   저장 키에 붙일 확장자
 */
public record ProcessedImage(
        byte[] content,
        String contentType,
        String extension,
        int width,
        int height
) {
    public long sizeBytes() {
        return content.length;
    }
}
