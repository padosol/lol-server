package com.example.lolserver.community.adapter.out.image;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.ImageProcessorPort;
import com.example.lolserver.community.application.port.out.ProcessedImage;
import com.example.lolserver.community.config.CommunityImageProperties;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 업로드 바이트의 실제 타입 판별 + 검증 + 정규화.
 *
 * <pre>
 *   1. 매직바이트로 실제 타입 판별   ← 클라이언트가 보낸 Content-Type 은 신뢰하지 않는다
 *   2. 화이트리스트 대조            ← SVG 는 명시적으로 없다(스크립트 → 저장형 XSS)
 *   3. 헤더에서 크기 읽기 → 픽셀 상한 검사   ← 디코드 '전에' 해야 폭탄 이미지를 막는다
 *   4. JPEG/PNG 만 디코드 + 상한 리사이즈 + 재인코딩(EXIF 제거)
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class DefaultImageProcessor implements ImageProcessorPort {

    private static final Tika TIKA = new Tika();

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    /**
     * 재인코딩 대상.
     *
     * <p>GIF 를 제외하는 이유: 애니메이션 GIF 를 {@code ImageIO} 로 재인코딩하면 첫 프레임만 남는다.
     * WebP 를 제외하는 이유: JDK 표준 {@code ImageIO} 에 WebP 리더/라이터가 아예 없다.
     * 두 형식은 원본 그대로 저장하고 크기 검증만 한다 — 대신 EXIF 가 남을 수 있다는 것이
     * 이 선택의 대가이며, 필요해지면 별도 라이브러리로 메타데이터만 제거하는 경로를 붙인다.
     */
    private static final Set<String> REENCODABLE = Set.of("image/jpeg", "image/png");

    private final CommunityImageProperties properties;

    @Override
    public ProcessedImage process(byte[] content) {
        if (content == null || content.length == 0) {
            throw new CoreException(ErrorType.IMAGE_FILE_REQUIRED);
        }

        String contentType = TIKA.detect(content);
        if (!properties.getAllowedTypes().contains(contentType)) {
            throw new CoreException(ErrorType.IMAGE_TYPE_NOT_SUPPORTED);
        }

        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            // 화이트리스트에 확장자 매핑이 없는 타입이 추가된 경우(설정 실수).
            throw new CoreException(ErrorType.IMAGE_TYPE_NOT_SUPPORTED);
        }

        ImageDimensions dimensions = ImageDimensionReader.read(content, contentType);
        validate(dimensions);

        if (REENCODABLE.contains(contentType)) {
            return ImageNormalizer.normalize(content, contentType, extension,
                    properties.getMaxWidth());
        }
        return new ProcessedImage(content, contentType, extension,
                dimensions.width(), dimensions.height());
    }

    private void validate(ImageDimensions dimensions) {
        if (dimensions.width() <= 0 || dimensions.height() <= 0) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        }
        if (dimensions.pixels() > properties.getMaxPixels()) {
            throw new CoreException(ErrorType.IMAGE_SIZE_EXCEEDED);
        }
    }
}
