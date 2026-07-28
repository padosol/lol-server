package com.example.lolserver.duo.application.port.in;

import com.example.lolserver.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.duo.application.command.UpdateDuoPostCommand;
import com.example.lolserver.duo.application.model.resultmodel.DuoPostResultModel;

public interface DuoPostUseCase {
    DuoPostResultModel createDuoPost(Long memberId, CreateDuoPostCommand command);
    DuoPostResultModel updateDuoPost(Long memberId, Long duoPostId, UpdateDuoPostCommand command);
    void deleteDuoPost(Long memberId, Long duoPostId);
}
