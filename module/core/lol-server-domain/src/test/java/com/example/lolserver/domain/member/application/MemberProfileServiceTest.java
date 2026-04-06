package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    @DisplayName("내 프로필을 조회하면 회원 정보를 반환한다")
    @Test
    void getMyProfile() {
        // given
        Long memberId = 1L;
        Member member = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(memberPersistencePort.findById(memberId)).willReturn(Optional.of(member));

        // when
        MemberReadModel result = memberProfileService.getMyProfile(memberId);

        // then
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
    }

    @DisplayName("존재하지 않는 회원의 프로필을 조회하면 예외가 발생한다")
    @Test
    void getMyProfile_notFound() {
        // given
        given(memberPersistencePort.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberProfileService.getMyProfile(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
    }
}
