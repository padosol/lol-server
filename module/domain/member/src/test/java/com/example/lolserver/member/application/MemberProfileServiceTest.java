package com.example.lolserver.member.application;

import com.example.lolserver.member.application.model.MemberProfileReadModel;
import com.example.lolserver.member.application.model.MemberReadModel;
import com.example.lolserver.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.member.domain.Member;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.member.domain.vo.OAuthProvider;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @InjectMocks
    private MemberProfileService memberProfileService;

    @DisplayName("내 프로필을 조회하면 회원 정보와 소셜 계정을 반환한다")
    @Test
    void getMyProfile() {
        // given
        Long memberId = 1L;
        SocialAccount socialAccount = SocialAccount.builder()
                .id(1L).memberId(memberId)
                .provider("GOOGLE").providerId("google-123")
                .email("test@gmail.com")
                .linkedAt(LocalDateTime.now()).build();

        Member member = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>(List.of(socialAccount)))
                .createdAt(LocalDateTime.now()).build();

        given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));

        // when
        MemberReadModel result = memberProfileService.getMyProfile(memberId);

        // then
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
        assertThat(result.getSocialAccounts()).hasSize(1);
        assertThat(result.getSocialAccounts().get(0).getProvider())
                .isEqualTo("GOOGLE");
    }

    @DisplayName("존재하지 않는 회원의 프로필을 조회하면 예외가 발생한다")
    @Test
    void getMyProfile_notFound() {
        // given
        given(memberPersistencePort.findByIdWithSocialAccounts(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberProfileService.getMyProfile(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
    }

    @DisplayName("작성자 프로필 단건 조회 시 경량 모델(id/닉네임/프로필이미지)을 반환한다")
    @Test
    void getMemberProfile() {
        // given
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId).uuid("uuid").email("a@b.com")
                .nickname("작성자").profileImageUrl("http://img/1.png")
                .role("USER").createdAt(LocalDateTime.now()).build();
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        MemberProfileReadModel result = memberProfileService.getMemberProfile(memberId);

        // then
        assertThat(result.getId()).isEqualTo(memberId);
        assertThat(result.getNickname()).isEqualTo("작성자");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://img/1.png");
    }

    @DisplayName("존재하지 않는 작성자 단건 조회 시 예외가 발생한다")
    @Test
    void getMemberProfile_notFound() {
        // given
        given(memberPersistencePort.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberProfileService.getMemberProfile(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
    }

    @DisplayName("작성자 프로필 배치 조회 시 존재하는 회원만 ReadModel로 반환한다")
    @Test
    void getMemberProfiles() {
        // given
        Member m1 = Member.builder().id(1L).uuid("u1").nickname("회원1")
                .role("USER").createdAt(LocalDateTime.now()).build();
        Member m2 = Member.builder().id(2L).uuid("u2").nickname("회원2")
                .role("USER").createdAt(LocalDateTime.now()).build();
        given(memberPersistencePort.findAllByIdIn(List.of(1L, 2L)))
                .willReturn(List.of(m1, m2));

        // when
        List<MemberProfileReadModel> result =
                memberProfileService.getMemberProfiles(List.of(1L, 2L));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemberProfileReadModel::getNickname)
                .containsExactlyInAnyOrder("회원1", "회원2");
    }

    @DisplayName("RIOT 연동 puuid 조회 시 연동된 puuid를 반환한다")
    @Test
    void findRiotPuuid() {
        // given
        Long memberId = 1L;
        SocialAccount google = SocialAccount.builder()
                .id(1L).memberId(memberId)
                .provider(OAuthProvider.GOOGLE.name()).providerId("g-1")
                .linkedAt(LocalDateTime.now()).build();
        SocialAccount riot = SocialAccount.builder()
                .id(2L).memberId(memberId)
                .provider(OAuthProvider.RIOT.name()).providerId("r-1")
                .puuid("riot-puuid-123")
                .linkedAt(LocalDateTime.now()).build();
        Member member = Member.builder()
                .id(memberId).uuid("uuid").nickname("회원").role("USER")
                .socialAccounts(new ArrayList<>(List.of(google, riot)))
                .createdAt(LocalDateTime.now()).build();
        given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));

        // when
        Optional<String> result = memberProfileService.findRiotPuuid(memberId);

        // then
        assertThat(result).contains("riot-puuid-123");
    }

    @DisplayName("RIOT 연동이 없으면 빈 값을 반환한다")
    @Test
    void findRiotPuuid_notLinked() {
        // given
        Long memberId = 1L;
        SocialAccount google = SocialAccount.builder()
                .id(1L).memberId(memberId)
                .provider(OAuthProvider.GOOGLE.name()).providerId("g-1")
                .linkedAt(LocalDateTime.now()).build();
        Member member = Member.builder()
                .id(memberId).uuid("uuid").nickname("회원").role("USER")
                .socialAccounts(new ArrayList<>(List.of(google)))
                .createdAt(LocalDateTime.now()).build();
        given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));

        // when
        Optional<String> result = memberProfileService.findRiotPuuid(memberId);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("존재하지 않는 회원의 RIOT puuid 조회 시 예외가 발생한다")
    @Test
    void findRiotPuuid_memberNotFound() {
        // given
        given(memberPersistencePort.findByIdWithSocialAccounts(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberProfileService.findRiotPuuid(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
    }
}
