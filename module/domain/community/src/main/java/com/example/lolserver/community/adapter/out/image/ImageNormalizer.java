package com.example.lolserver.community.adapter.out.image;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.ProcessedImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 디코드 → 상한 리사이즈 → 재인코딩.
 *
 * <p>재인코딩의 진짜 목적은 리사이즈가 아니라 <b>EXIF 제거</b>다. 스마트폰 사진의 GPS 좌표가
 * 그대로 공개되는 건 개인정보 사고이고, {@code ImageIO} 로 디코드→리인코드하면 메타데이터가
 * 통째로 사라진다. 그래서 크기가 상한 이내여도 JPEG/PNG 는 항상 다시 굽는다.
 */
final class ImageNormalizer {

    private static final String JPEG = "image/jpeg";
    private static final float JPEG_QUALITY = 0.85f;

    private ImageNormalizer() {
    }

    /**
     * 결과 크기는 리사이즈 이후 값이어야 하므로 바이트와 함께 돌려준다 —
     * 되읽어 재디코드하면 같은 이미지를 세 번 디코드하게 된다.
     */
    static ProcessedImage normalize(byte[] content, String contentType,
            String extension, int maxWidth) {
        BufferedImage source = decode(content);
        BufferedImage prepared = prepare(source, contentType, maxWidth);
        byte[] encoded = JPEG.equals(contentType) ? encodeJpeg(prepared) : encodePng(prepared);
        return new ProcessedImage(encoded, contentType, extension,
                prepared.getWidth(), prepared.getHeight());
    }

    private static BufferedImage decode(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                // 매직바이트는 통과했는데 리더가 없거나 본문이 깨진 경우.
                throw new CoreException(ErrorType.IMAGE_INVALID);
            }
            return image;
        } catch (IOException e) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        }
    }

    /**
     * 상한 초과 시 비율을 유지해 축소하고, JPEG 로 나갈 이미지의 알파 채널을 걷어낸다
     * (JPEG 라이터는 알파를 가진 래스터를 쓰지 못하고 IOException 을 던진다).
     */
    private static BufferedImage prepare(BufferedImage source, String contentType, int maxWidth) {
        boolean jpeg = JPEG.equals(contentType);
        boolean needsResize = source.getWidth() > maxWidth;
        boolean needsFlatten = jpeg && source.getColorModel().hasAlpha();
        if (!needsResize && !needsFlatten) {
            return source;
        }

        int targetWidth = needsResize ? maxWidth : source.getWidth();
        int targetHeight = needsResize
                ? Math.max(1, Math.round(source.getHeight() * (float) maxWidth / source.getWidth()))
                : source.getHeight();

        BufferedImage target = new BufferedImage(targetWidth, targetHeight,
                jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            if (jpeg) {
                // 투명 영역이 검게 나오지 않도록 흰 배경을 깐다.
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
            }
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static byte[] encodePng(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        }
        return out.toByteArray();
    }
}
