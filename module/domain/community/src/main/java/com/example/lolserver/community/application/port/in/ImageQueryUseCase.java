package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;

import java.util.List;

public interface ImageQueryUseCase {

    /**
     * 게시글에 현재 붙어 있는 이미지. 본문에 URL 이 이미 들어 있어 렌더링에는 불필요하지만,
     * 수정 화면이 "현재 첨부 목록"을 알아야 한다.
     */
    List<PostImageReadModel> getPostImages(Long postId);
}
