package com.example.lolserver.domain.member.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @DisplayName("소셜 계정을 연동하면 socialAccounts에 추가된다")
    @Test
    void linkSocialAccount_success() {
        // given
        Member member = createMemberWithSocialAccounts();

        // when
        member.linkSocialAccount(
                "GOOGLE", "google-123", "test@gmail.com",
                "테스터", null, null);

        // then
        assertThat(member.getSocialAccounts()).hasSize(1);
        SocialAccount result = member.getSocialAccounts().get(0);
        assertThat(result.getProvider()).isEqualTo("GOOGLE");
        assertThat(result.getProviderId()).isEqualTo("google-123");
        assertThat(result.getMemberId()).isEqualTo(1L);
    }

    @DisplayName("이미 같은 provider가 연동되어 있으면 예외가 발생한다")
    @Test
    void linkSocialAccount_duplicateProvider() {
        // given
        Member member = createMemberWithSocialAccounts(
                SocialAccount.builder()
                        .id(1L).memberId(1L)
                        .provider("GOOGLE").providerId("google-123")
                        .linkedAt(LocalDateTime.now()).build());

        // when & then
        assertThatThrownBy(() -> member.linkSocialAccount(
                "GOOGLE", "google-456", "other@gmail.com",
                "다른유저", null, null))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.SOCIAL_ACCOUNT_ALREADY_LINKED);
    }

    @DisplayName("소셜 계정을 연동 해제하면 socialAccounts에서 제거된다")
    @Test
    void unlinkSocialAccount_success() {
        // given
        SocialAccount account = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123")
                .linkedAt(LocalDateTime.now()).build();
        Member member = createMemberWithSocialAccounts(account);

        // when
        member.unlinkSocialAccount(1L);

        // then
        assertThat(member.getSocialAccounts()).isEmpty();
    }

    @DisplayName("존재하지 않는 소셜 계정을 연동 해제하면 예외가 발생한다")
    @Test
    void unlinkSocialAccount_notFound() {
        // given
        Member member = createMemberWithSocialAccounts(
                SocialAccount.builder()
                        .id(1L).memberId(1L)
                        .provider("GOOGLE").providerId("google-123")
                        .linkedAt(LocalDateTime.now()).build());

        // when & then
        assertThatThrownBy(() -> member.unlinkSocialAccount(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.SOCIAL_ACCOUNT_NOT_FOUND);
    }

    @DisplayName("소셜 계정이 없는 Member에서 연동 해제하면 예외가 발생한다")
    @Test
    void unlinkSocialAccount_emptyList() {
        // given
        Member member = createMemberWithSocialAccounts();

        // when & then
        assertThatThrownBy(() -> member.unlinkSocialAccount(1L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.SOCIAL_ACCOUNT_NOT_FOUND);
    }

    @DisplayName("신규 회원과 최초 소셜 계정을 함께 생성한다")
    @Test
    void createNewWithSocialAccount() {
        // when
        Member member = Member.createNewWithSocialAccount(
                "GOOGLE", "google-123", "test@gmail.com",
                "테스터", null, null);

        // then
        assertThat(member.getUuid()).isNotNull();
        assertThat(member.getNickname()).isNotBlank();
        assertThat(member.getRole()).isEqualTo("USER");
        assertThat(member.getSocialAccounts()).hasSize(1);

        SocialAccount account = member.getSocialAccounts().get(0);
        assertThat(account.getMemberId()).isNull();
        assertThat(account.getProvider()).isEqualTo("GOOGLE");
        assertThat(account.getProviderId()).isEqualTo("google-123");
    }

    @DisplayName("회원 탈퇴 시 개인정보가 익명화된다")
    @Test
    void withdraw_anonymizesPersonalInfo() {
        // given
        SocialAccount googleAccount = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123")
                .email("test@gmail.com").nickname("구글유저")
                .profileImageUrl("https://example.com/photo.jpg")
                .linkedAt(LocalDateTime.now()).build();
        SocialAccount riotAccount = SocialAccount.builder()
                .id(2L).memberId(1L)
                .provider("RIOT").providerId("riot-456")
                .email("test@riot.com").nickname("라이엇유저")
                .profileImageUrl("https://example.com/riot.jpg")
                .linkedAt(LocalDateTime.now()).build();
        Member member = Member.builder()
                .id(1L).uuid("test-uuid")
                .email("original@test.com")
                .nickname("테스터")
                .profileImageUrl("https://example.com/profile.jpg")
                .role("USER")
                .socialAccounts(
                        new ArrayList<>(List.of(googleAccount, riotAccount)))
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        // when
        member.withdraw();

        // then
        assertThat(member.getWithdrawnAt()).isNotNull();
        assertThat(member.getEmail()).startsWith("withdrawn_");
        assertThat(member.getNickname()).startsWith("탈퇴한회원_");
        assertThat(member.getProfileImageUrl()).isNull();

        List<SocialAccount> accounts = member.getSocialAccounts();
        assertThat(accounts).hasSize(2);
        accounts.forEach(account -> {
            assertThat(account.getProviderId()).startsWith("withdrawn_");
            assertThat(account.getEmail()).isNull();
            assertThat(account.getNickname()).isNull();
            assertThat(account.getProfileImageUrl()).isNull();
        });
    }

    @DisplayName("이미 탈퇴한 회원이 다시 탈퇴하면 예외가 발생한다")
    @Test
    void withdraw_alreadyWithdrawn_throwsException() {
        // given
        Member member = createMemberWithSocialAccounts();
        member.withdraw();

        // when & then
        assertThatThrownBy(() -> member.withdraw())
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_ALREADY_WITHDRAWN);
    }

    @DisplayName("탈퇴 여부를 확인할 수 있다")
    @Test
    void isWithdrawn_returnsCorrectState() {
        // given
        Member member = createMemberWithSocialAccounts();

        // then - 활성 회원
        assertThat(member.isWithdrawn()).isFalse();

        // when
        member.withdraw();

        // then - 탈퇴 회원
        assertThat(member.isWithdrawn()).isTrue();
    }

    private Member createMemberWithSocialAccounts(
            SocialAccount... accounts) {
        return Member.builder()
                .id(1L).uuid("test-uuid")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>(List.of(accounts)))
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }
}
