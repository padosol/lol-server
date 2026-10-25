package com.example.lolserver.community.application.port.in;

import java.util.List;

/**
 * 글 저장 시 이미지를 확정·해제한다. {@code PostService} 가 같은 컨텍스트의 port.in 으로
 * 주입받아 호출하므로 트랜잭션이 하나로 묶인다.
 */
public interface ImageAttachUseCase {

    /** 글 생성. 소유자·상태 검증은 도메인이 한다. */
    void attach(Long memberId, Long postId, List<Long> imageIds);

    /** 글 수정 — <b>전체 교체 시맨틱</b>. 요청에 없는 기존 첨부는 DETACHED 로 전이된다. */
    void replace(Long memberId, Long postId, List<Long> imageIds);

    /** 글 삭제. 파일은 아직 지우지 않고 유예 후 정리 배치가 처리한다. */
    void detachByPostId(Long postId);
}
