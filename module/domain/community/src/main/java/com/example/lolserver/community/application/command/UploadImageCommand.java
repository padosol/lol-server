package com.example.lolserver.community.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 컨트롤러가 {@code MultipartFile} 을 풀어 담는다. 애플리케이션 계층에 웹 타입이 들어오지
 * 않게 하려는 것이기도 하고(ArchUnit), 포트를 웹 프레임워크에서 떼어놓는 게 맞기 때문이기도 하다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageCommand {

    private byte[] content;

    /**
     * 클라이언트가 선언한 Content-Type. <b>검증에는 쓰지 않는다</b> — 위조가 자유롭기 때문이며,
     * 실제 타입은 매직바이트로 판별한다. 남용 추적 시 로그 대조용으로만 남긴다.
     */
    private String declaredContentType;
}
