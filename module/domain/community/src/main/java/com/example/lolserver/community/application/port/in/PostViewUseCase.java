package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.domain.vo.ViewerKey;

public interface PostViewUseCase {

    /** 조회를 기록하고, 기록 후 화면에 보일 조회수를 반환한다. */
    int recordView(Long postId, ViewerKey viewer);
}
