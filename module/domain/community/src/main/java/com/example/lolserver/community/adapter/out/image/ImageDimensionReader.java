package com.example.lolserver.community.adapter.out.image;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;

import java.nio.charset.StandardCharsets;

/**
 * 파일 헤더만 읽어 이미지 크기를 구한다.
 *
 * <p><b>디코드하지 않는 것이 핵심이다.</b> 폭탄 이미지(decompression bomb) 방어는 디코드
 * <i>전에</i> 픽셀 수를 알아야 성립한다 — 몇백 KB 짜리 PNG 가 디코드되는 순간 수 GB 의 힙을
 * 요구할 수 있어, 용량 검사만으로는 막히지 않는다. {@code ImageIO.read()} 로 크기를 재면
 * 이미 늦다.
 *
 * <p>WebP 를 직접 파싱하는 이유: JDK 표준 {@code ImageIO} 에는 WebP 리더가 없어
 * {@code ImageIO} 경유로는 크기조차 알 수 없다.
 */
final class ImageDimensionReader {

    private ImageDimensionReader() {
    }

    static ImageDimensions read(byte[] content, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> readJpeg(content);
            case "image/png" -> readPng(content);
            case "image/gif" -> readGif(content);
            case "image/webp" -> readWebp(content);
            default -> throw new CoreException(ErrorType.IMAGE_TYPE_NOT_SUPPORTED);
        };
    }

    /**
     * SOF(Start Of Frame) 세그먼트에 크기가 들어 있다. 마커를 건너뛰며 찾는다.
     *
     * <p>"건너뛸 바이트인가" 판단을 {@link #skipNonSegment} 로 분리해 루프 본문에서
     * 분기 탈출(break·continue)을 없앴다. 판단과 전진이 섞이면 마커 규칙을 고칠 때
     * 어느 경로가 인덱스를 얼마나 움직이는지 따라가기 어렵다.
     */
    private static ImageDimensions readJpeg(byte[] c) {
        int i = 2;
        while (i + 9 < c.length) {
            int skipped = skipNonSegment(c, i);
            if (skipped > i) {
                i = skipped;
            } else {
                int length = u16be(c, i + 2);
                if (length < 2) {
                    // 세그먼트 길이가 자기 자신보다 짧다 = 손상된 파일.
                    throw new CoreException(ErrorType.IMAGE_INVALID);
                }
                if (isStartOfFrame(c[i + 1] & 0xFF)) {
                    return new ImageDimensions(u16be(c, i + 7), u16be(c, i + 5));
                }
                i += 2 + length;
            }
        }
        throw new CoreException(ErrorType.IMAGE_INVALID);
    }

    /**
     * 길이 필드가 없어 그냥 지나쳐야 하는 바이트면 다음 위치를, 길이 필드를 가진
     * 세그먼트의 시작이면 {@code i} 를 그대로 돌려준다.
     */
    private static int skipNonSegment(byte[] c, int i) {
        if ((c[i] & 0xFF) != 0xFF) {
            return i + 1;
        }
        int marker = c[i + 1] & 0xFF;
        // 0xFF 채움 바이트, SOI, TEM, RSTn 은 길이 필드가 없다.
        if (marker == 0xFF) {
            return i + 1;
        }
        if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
            return i + 2;
        }
        return i;
    }

    /** 0xC4(DHT)·0xC8(JPG)·0xCC(DAC) 는 SOF 가 아니다. */
    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF
                && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    /** IHDR 은 항상 첫 청크이고 위치가 고정이다. */
    private static ImageDimensions readPng(byte[] c) {
        requireLength(c, 24);
        return new ImageDimensions(u32be(c, 16), u32be(c, 20));
    }

    private static ImageDimensions readGif(byte[] c) {
        requireLength(c, 10);
        return new ImageDimensions(u16le(c, 6), u16le(c, 8));
    }

    /** RIFF 컨테이너의 청크 종류(VP8 / VP8L / VP8X)마다 크기 위치가 다르다. */
    private static ImageDimensions readWebp(byte[] c) {
        requireLength(c, 26);
        String chunk = new String(c, 12, 4, StandardCharsets.US_ASCII);
        return switch (chunk) {
            case "VP8 " -> readLossyWebp(c);
            case "VP8L" -> readLosslessWebp(c);
            case "VP8X" -> readExtendedWebp(c);
            default -> throw new CoreException(ErrorType.IMAGE_INVALID);
        };
    }

    private static ImageDimensions readLossyWebp(byte[] c) {
        requireLength(c, 30);
        // 26·28 의 상위 2비트는 스케일 필드다.
        return new ImageDimensions(u16le(c, 26) & 0x3FFF, u16le(c, 28) & 0x3FFF);
    }

    private static ImageDimensions readLosslessWebp(byte[] c) {
        requireLength(c, 25);
        int bits = u32le(c, 21);
        return new ImageDimensions((bits & 0x3FFF) + 1, ((bits >>> 14) & 0x3FFF) + 1);
    }

    private static ImageDimensions readExtendedWebp(byte[] c) {
        requireLength(c, 30);
        return new ImageDimensions(u24le(c, 24) + 1, u24le(c, 27) + 1);
    }

    private static void requireLength(byte[] c, int required) {
        if (c.length < required) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        }
    }

    private static int u16be(byte[] c, int i) {
        return ((c[i] & 0xFF) << 8) | (c[i + 1] & 0xFF);
    }

    private static int u32be(byte[] c, int i) {
        return ((c[i] & 0xFF) << 24) | ((c[i + 1] & 0xFF) << 16)
                | ((c[i + 2] & 0xFF) << 8) | (c[i + 3] & 0xFF);
    }

    private static int u16le(byte[] c, int i) {
        return (c[i] & 0xFF) | ((c[i + 1] & 0xFF) << 8);
    }

    private static int u24le(byte[] c, int i) {
        return (c[i] & 0xFF) | ((c[i + 1] & 0xFF) << 8) | ((c[i + 2] & 0xFF) << 16);
    }

    private static int u32le(byte[] c, int i) {
        return (c[i] & 0xFF) | ((c[i + 1] & 0xFF) << 8)
                | ((c[i + 2] & 0xFF) << 16) | ((c[i + 3] & 0xFF) << 24);
    }
}
