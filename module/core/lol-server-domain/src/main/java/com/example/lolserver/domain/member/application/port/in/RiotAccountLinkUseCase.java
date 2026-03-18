package com.example.lolserver.domain.member.application.port.in;

import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;

import java.util.List;

public interface RiotAccountLinkUseCase {

    RiotAccountLinkReadModel linkRiotAccount(Long memberId, RiotLinkCommand command);

    List<RiotAccountLinkReadModel> getLinkedAccounts(Long memberId);

    void unlinkRiotAccount(Long memberId, Long linkId);
}
