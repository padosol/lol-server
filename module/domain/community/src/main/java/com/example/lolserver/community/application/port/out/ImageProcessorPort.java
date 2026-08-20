package com.example.lolserver.community.application.port.out;

/**
 * 업로드된 바이트의 실제 타입 판별 + 검증 + 정규화.
 *
 * <p>포트로 뺀 이유는 구현이 Tika·ImageIO 같은 기술에 묶이기 때문이다. 애플리케이션은
 * "검증된 이미지를 받는다"만 알면 되고, 서비스 테스트가 실제 이미지 디코딩 없이 돌 수 있다.
 */
public interface ImageProcessorPort {

    /**
     * @throws com.example.lolserver.common.error.CoreException 형식이 화이트리스트 밖이거나,
     *         픽셀 수 상한을 넘거나(폭탄 이미지), 디코드에 실패한 경우
     */
    ProcessedImage process(byte[] content);
}
