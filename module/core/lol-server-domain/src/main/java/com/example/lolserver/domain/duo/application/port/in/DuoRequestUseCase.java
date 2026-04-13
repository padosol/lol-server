package com.example.lolserver.domain.duo.application.port.in;

import com.example.lolserver.domain.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;

public interface DuoRequestUseCase {
    DuoRequestReadModel createDuoRequest(Long memberId, Long duoPostId,
            CreateDuoRequestCommand command);
    DuoMatchResultReadModel acceptDuoRequest(Long memberId, Long requestId);
    DuoMatchResultReadModel confirmDuoRequest(Long memberId, Long requestId);
    void rejectDuoRequest(Long memberId, Long requestId);
    void cancelDuoRequest(Long memberId, Long requestId);
}
