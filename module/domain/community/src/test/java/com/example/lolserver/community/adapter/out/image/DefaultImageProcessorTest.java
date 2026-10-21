package com.example.lolserver.community.adapter.out.image;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.ProcessedImage;
import com.example.lolserver.community.config.CommunityImageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultImageProcessor")
class DefaultImageProcessorTest {

    private final CommunityImageProperties properties = new CommunityImageProperties();
    private final DefaultImageProcessor processor = new DefaultImageProcessor(properties);

    @DisplayName("선언된 Content-Type 이 아니라 매직바이트로 실제 타입을 판별한다")
    @Test
    void detectsRealTypeFromMagicBytes() {
        ProcessedImage result = processor.process(png(100, 50));

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
    }

    @DisplayName("상한 폭을 넘으면 비율을 유지해 축소한다")
    @Test
    void resizesWhenWiderThanMax() {
        properties.setMaxWidth(200);

        ProcessedImage result = processor.process(png(1000, 500));

        assertThat(result.width()).isEqualTo(200);
        assertThat(result.height()).isEqualTo(100);
    }

    @DisplayName("상한 이내여도 JPEG 은 다시 굽는다 — 재인코딩의 목적이 EXIF 제거이기 때문")
    @Test
    void reencodesJpegEvenWithoutResize() {
        byte[] original = jpeg(100, 50);

        ProcessedImage result = processor.process(original);

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.content()).isNotEqualTo(original);
    }

    @DisplayName("GIF 는 원본 그대로 저장한다 — 재인코딩하면 애니메이션의 첫 프레임만 남는다")
    @Test
    void keepsGifAsIs() {
        byte[] original = gif(100, 50);

        ProcessedImage result = processor.process(original);

        assertThat(result.contentType()).isEqualTo("image/gif");
        assertThat(result.content()).isEqualTo(original);
        assertThat(result.width()).isEqualTo(100);
        assertThat(result.height()).isEqualTo(50);
    }

    @DisplayName("WebP 는 ImageIO 리더가 없으므로 헤더를 직접 파싱해 크기를 얻는다")
    @Test
    void readsWebpDimensionsFromHeader() {
        ProcessedImage result = processor.process(losslessWebp(100, 50));

        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(100);
        assertThat(result.height()).isEqualTo(50);
    }

    @DisplayName("SVG 는 화이트리스트에 없다 — 스크립트를 담을 수 있어 저장형 XSS 가 된다")
    @Test
    void rejectsSvg() {
        byte[] svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> processor.process(svg))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_TYPE_NOT_SUPPORTED);
    }

    /**
     * 실제로 6만×6만 이미지를 만들 수는 없다. 헤더만 그렇게 주장하는 33바이트를 보내
     * <b>디코드 전에</b> 걸리는지 확인한다 — 디코드 후에 재면 이미 힙이 터진 뒤다.
     */
    @DisplayName("헤더가 주장하는 픽셀 수가 상한을 넘으면 디코드 전에 거절한다")
    @Test
    void rejectsDecompressionBombBeforeDecoding() {
        byte[] bomb = pngHeaderClaiming(60_000, 60_000);

        assertThatThrownBy(() -> processor.process(bomb))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_SIZE_EXCEEDED);
    }

    @DisplayName("빈 바이트는 IMAGE_FILE_REQUIRED 다")
    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> processor.process(new byte[0]))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_FILE_REQUIRED);
    }

    @DisplayName("확장자를 뒤에 붙일 수 있도록 판별 결과에 확장자가 함께 온다")
    @Test
    void carriesExtension() {
        assertThat(processor.process(jpeg(10, 10)).extension()).isEqualTo("jpg");
        assertThat(processor.process(gif(10, 10)).extension()).isEqualTo("gif");
    }

    private static byte[] png(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png");
    }

    private static byte[] jpeg(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    private static byte[] gif(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "gif");
    }

    private static byte[] encode(BufferedImage image, String format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** PNG 시그니처 + IHDR 만. 픽셀 데이터는 없다(디코드까지 가면 안 되므로 없어도 된다). */
    private static byte[] pngHeaderClaiming(int width, int height) {
        byte[] bytes = new byte[33];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        bytes[11] = 0x0D;
        System.arraycopy("IHDR".getBytes(StandardCharsets.US_ASCII), 0, bytes, 12, 4);
        writeBigEndian(bytes, 16, width);
        writeBigEndian(bytes, 20, height);
        bytes[24] = 8;
        return bytes;
    }

    /** RIFF/WEBP 컨테이너 + VP8L 청크 헤더. Tika 는 "RIFF????WEBP" 로 판별한다. */
    private static byte[] losslessWebp(int width, int height) {
        byte[] bytes = new byte[32];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy("VP8L".getBytes(StandardCharsets.US_ASCII), 0, bytes, 12, 4);
        bytes[20] = 0x2F;
        int bits = ((width - 1) & 0x3FFF) | (((height - 1) & 0x3FFF) << 14);
        writeLittleEndian(bytes, 21, bits);
        return bytes;
    }

    private static void writeBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private static void writeLittleEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
    }

    static {
        // CI 에는 디스플레이가 없다. ImageIO 자체는 헤드리스에서 동작하지만,
        // 명시해 두지 않으면 환경에 따라 AWT 초기화가 다르게 흐른다.
        System.setProperty("java.awt.headless", "true");
    }
}
