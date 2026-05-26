package com.example.lolserver.duo.application.port.in;

import com.example.lolserver.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.duo.application.command.UpdateDuoPostCommand;
import com.example.lolserver.duo.application.model.DuoPostReadModel;

public interface DuoPostUseCase {
    DuoPostReadModel createDuoPost(Long memberId, CreateDuoPostCommand command);
    DuoPostReadModel updateDuoPost(Long memberId, Long duoPostId, UpdateDuoPostCommand command);
    void deleteDuoPost(Long memberId, Long duoPostId);
}
