package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.UploadImageCommand;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;

public interface ImageUseCase {

    PostImageReadModel upload(Long memberId, UploadImageCommand command);

    /** 에디터에서 사용자가 명시적으로 뺀 경우. 본인 소유 + PENDING 만 허용한다. */
    void delete(Long memberId, Long imageId);
}
