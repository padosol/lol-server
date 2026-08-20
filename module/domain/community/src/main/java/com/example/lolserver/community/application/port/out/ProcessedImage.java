package com.example.lolserver.community.application.port.out;

import java.util.Arrays;

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

    /**
     * record 가 자동 생성하는 {@code equals} 는 배열 컴포넌트를 <b>참조로</b> 비교한다.
     * 값 타입처럼 생긴 것이 값처럼 비교되지 않으면 언젠가 조용히 틀린 결과를 낸다.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProcessedImage other)) {
            return false;
        }
        return width == other.width
                && height == other.height
                && Arrays.equals(content, other.content)
                && contentType.equals(other.contentType)
                && extension.equals(other.extension);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + contentType.hashCode();
        result = 31 * result + extension.hashCode();
        result = 31 * result + width;
        return 31 * result + height;
    }

    /**
     * 바이트 내용은 찍지 않고 <b>길이만</b> 남긴다. 최대 5MB 짜리 배열을 로그에 풀면
     * 로그가 터지고, 무엇보다 사용자가 올린 이미지 원문이 로그에 남는다.
     */
    @Override
    public String toString() {
        return "ProcessedImage[contentType=%s, extension=%s, %dx%d, %d bytes]"
                .formatted(contentType, extension, width, height, content.length);
    }
}
