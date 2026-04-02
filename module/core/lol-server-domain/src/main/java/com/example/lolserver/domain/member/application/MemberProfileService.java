package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileService
        implements MemberCommandUseCase, MemberQueryUseCase {

    private final MemberPersistencePort memberPersistencePort;

    @Override
    @Transactional
    public MemberReadModel updateNickname(
            Long memberId, UpdateNicknameCommand command) {
        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));

        member.updateNickname(command.getNickname());
        memberPersistencePort.save(member);

        return MemberReadModel.of(member);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberReadModel getMyProfile(Long memberId) {
        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));

        return MemberReadModel.of(member);
    }
}
