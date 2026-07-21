package com.example.lolserver.community.application.port.in;

import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;

public interface BookmarkQueryUseCase {

    SliceResult<PostListReadModel> getMyBookmarks(Long memberId, int page);
}
