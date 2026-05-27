package com.example.lolserver.duo.application.port.in;

import com.example.lolserver.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.duo.application.model.resultmodel.DuoMatchResultModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoRequestResultModel;

public interface DuoRequestUseCase {
    DuoRequestResultModel createDuoRequest(Long memberId, Long duoPostId,
            CreateDuoRequestCommand command);
    DuoMatchResultModel acceptDuoRequest(Long memberId, Long requestId);
    DuoMatchResultModel confirmDuoRequest(Long memberId, Long requestId);
    void rejectDuoRequest(Long memberId, Long requestId);
    void cancelDuoRequest(Long memberId, Long requestId);
}
