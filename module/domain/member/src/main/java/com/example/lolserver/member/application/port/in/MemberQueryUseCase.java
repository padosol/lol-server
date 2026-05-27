package com.example.lolserver.member.application.port.in;

import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import com.example.lolserver.member.application.model.readmodel.MemberReadModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberQueryUseCase {

    MemberReadModel getMyProfile(Long memberId);

    /**
     * 단건 작성자 프로필 조회. 회원이 없으면 예외를 던진다.
     */
    MemberProfileReadModel getMemberProfile(Long memberId);

    /**
     * 다건 작성자 프로필 배치 조회. 존재하는 회원만 반환한다(순서/개수 보장 없음).
     */
    List<MemberProfileReadModel> getMemberProfiles(Collection<Long> memberIds);

    /**
     * 회원에 연동된 RIOT 계정의 puuid를 조회한다.
     * 회원이 없으면 예외를 던지고, RIOT 연동이 없으면 빈 값을 반환한다.
     */
    Optional<String> findRiotPuuid(Long memberId);
}
