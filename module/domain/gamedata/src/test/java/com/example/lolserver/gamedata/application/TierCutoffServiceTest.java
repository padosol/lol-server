package com.example.lolserver.gamedata.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.gamedata.application.port.out.TierCutoffPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("TierCutoffService.findTierCutoffs — 목록 조회 필터")
@ExtendWith(MockitoExtension.class)
class TierCutoffServiceTest {

    @Mock
    private TierCutoffPersistencePort tierCutoffPersistencePort;

    @InjectMocks
    private TierCutoffService tierCutoffService;

    @DisplayName("tier 필터에 맞는 데이터가 없으면 404 가 아니라 빈 목록")
    @Test
    void tier필터_데이터없음_빈목록() {
        given(tierCutoffPersistencePort.findByQueueAndTierAndPlatformId("RANKED_FLEX_SR", "CHALLENGER", "KR"))
                .willReturn(Optional.empty());

        assertThat(tierCutoffService.findTierCutoffs("kr", "RANKED_FLEX_SR", "CHALLENGER")).isEmpty();
    }

    @DisplayName("queue 는 tier 유무와 관계없이 QueueType 이름 표기(RANKED_SOLO_5x5)로 맞춘다")
    @Test
    void queue_대소문자_일관() {
        tierCutoffService.findTierCutoffs("kr", "ranked_solo_5X5", null);
        tierCutoffService.findTierCutoffs("kr", "RANKED_SOLO_5X5", "challenger");

        then(tierCutoffPersistencePort).should().findByPlatformIdAndQueue("KR", "RANKED_SOLO_5x5");
        then(tierCutoffPersistencePort).should()
                .findByQueueAndTierAndPlatformId("RANKED_SOLO_5x5", "CHALLENGER", "KR");
    }

    @DisplayName("tier 만 주고 queue 를 빼면 INVALID_INPUT")
    @Test
    void tier만_400() {
        assertThatThrownBy(() -> tierCutoffService.findTierCutoffs("kr", null, "CHALLENGER"))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT);
    }

    @DisplayName("알 수 없는 platform 은 500 이 아니라 INVALID_INPUT")
    @Test
    void 알수없는플랫폼_400() {
        assertThatThrownBy(() -> tierCutoffService.findTierCutoffs("xx", null, null))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT);
        then(tierCutoffPersistencePort).should(never()).findAllByPlatformId(any());
    }
}
