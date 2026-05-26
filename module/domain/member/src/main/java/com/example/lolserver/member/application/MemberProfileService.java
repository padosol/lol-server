package com.example.lolserver.member.application;

import com.example.lolserver.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.member.application.model.MemberProfileReadModel;
import com.example.lolserver.member.application.model.MemberReadModel;
import com.example.lolserver.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.member.domain.Member;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.member.domain.vo.OAuthProvider;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService
        implements MemberCommandUseCase, MemberQueryUseCase {

    private final MemberPersistencePort memberPersistencePort;

    @Override
    @Transactional
    public MemberReadModel updateNickname(
            Long memberId, UpdateNicknameCommand command) {
        Member member = findActiveMember(memberId);

        member.updateNickname(command.getNickname());
        memberPersistencePort.save(member);

        return MemberReadModel.of(member);
    }

    @Override
    public MemberReadModel getMyProfile(Long memberId) {
        Member member = findActiveMember(memberId);
        return MemberReadModel.of(member);
    }

    @Override
    public MemberProfileReadModel getMemberProfile(Long memberId) {
        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));
        return MemberProfileReadModel.of(member);
    }

    @Override
    public List<MemberProfileReadModel> getMemberProfiles(
            Collection<Long> memberIds) {
        return memberPersistencePort.findAllByIdIn(memberIds).stream()
                .map(MemberProfileReadModel::of)
                .toList();
    }

    @Override
    public Optional<String> findRiotPuuid(Long memberId) {
        Member member = memberPersistencePort
                .findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));

        return member.getSocialAccounts().stream()
                .filter(sa -> OAuthProvider.RIOT.name().equals(sa.getProvider())
                        && sa.getPuuid() != null)
                .map(SocialAccount::getPuuid)
                .findFirst();
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberPersistencePort
                .findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));
        member.validateNotWithdrawn();
        return member;
    }
}
