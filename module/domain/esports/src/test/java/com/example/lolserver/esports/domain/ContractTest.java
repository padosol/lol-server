package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.lolserver.esports.domain.RosterFixtures.PERSON;
import static com.example.lolserver.esports.domain.RosterFixtures.T1;
import static com.example.lolserver.esports.domain.RosterFixtures.T1A;
import static com.example.lolserver.esports.domain.RosterFixtures.date;
import static com.example.lolserver.esports.domain.RosterFixtures.endedPlayer;
import static com.example.lolserver.esports.domain.RosterFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractTest {

    @DisplayName("새 계약은 팀의 조직 ID 를 물려받는다")
    @Test
    void open_팀의_조직ID를_물려받는다() {
        Contract contract = Contract.open(PERSON, T1A, Role.PLAYER, Position.MID, ContractType.SIGN,
                date(2026, 11, 17), date(2027, 11, 15), null);

        assertThat(contract.getTeamId()).isEqualTo("t1a");
        assertThat(contract.getOrganizationId()).isEqualTo("t1");
    }

    @DisplayName("선수 계약인데 포지션이 없으면 거부한다")
    @Test
    void open_선수인데_포지션없으면_INVALID_INPUT() {
        assertThatThrownBy(() -> Contract.open(PERSON, T1, Role.PLAYER, null, ContractType.SIGN,
                date(2026, 11, 17), date(2027, 11, 15), null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
    }

    @DisplayName("코치 계약인데 포지션이 있으면 거부한다")
    @Test
    void open_코치인데_포지션있으면_INVALID_INPUT() {
        assertThatThrownBy(() -> Contract.open(PERSON, T1, Role.COACH, Position.MID, ContractType.SIGN,
                date(2026, 11, 17), date(2027, 11, 15), null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
    }

    @DisplayName("종료일이 시작일보다 빠르면 거부한다")
    @Test
    void open_종료일이_시작일보다_빠르면_INVALID_INPUT() {
        assertThatThrownBy(() -> Contract.open(PERSON, T1, Role.PLAYER, Position.MID, ContractType.SIGN,
                date(2026, 11, 17), date(2026, 11, 16), null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
    }

    @DisplayName("마지막 날은 조기 종료일이 있으면 조기 종료일, 없으면 계약 종료일")
    @Test
    void lastDay_조기종료일이_있으면_조기종료일() {
        Contract ended = endedPlayer(1L, T1, date(2025, 11, 18), date(2027, 11, 15),
                date(2026, 5, 31), EndReason.RELEASED);

        assertThat(ended.lastDay()).isEqualTo(date(2026, 5, 31));
        assertThat(player(2L, T1, date(2025, 11, 18), date(2027, 11, 15)).lastDay()).isEqualTo(date(2027, 11, 15));
        assertThat(ended.isEffectiveOn(date(2026, 5, 31))).isTrue();
        assertThat(ended.isEffectiveOn(date(2026, 6, 1))).isFalse();
    }

    @DisplayName("이미 종료 사유가 있는 계약을 다시 조기 종료하면 거부한다")
    @Test
    void endEarly_이미_종료사유가_있으면_CONTRACT_PERIOD_CONFLICT() {
        Contract ended = endedPlayer(1L, T1, date(2025, 11, 18), date(2027, 11, 15),
                date(2026, 5, 31), EndReason.RELEASED);

        assertThatThrownBy(() -> ended.endEarly(date(2026, 4, 30), EndReason.TRANSFERRED))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONTRACT_PERIOD_CONFLICT);
    }

    @DisplayName("정정할 때 조기 종료일과 종료 사유는 짝이어야 하고 EXPIRED 는 저장할 수 없다")
    @Test
    void correct_조기종료일과_사유는_짝이어야_한다() {
        Contract contract = player(1L, T1, date(2025, 11, 18), date(2027, 11, 15));

        assertThatThrownBy(() -> contract.correct(date(2025, 11, 18), date(2027, 11, 15),
                date(2026, 5, 31), null, Position.MID, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
        assertThatThrownBy(() -> contract.correct(date(2025, 11, 18), date(2027, 11, 15),
                date(2026, 5, 31), EndReason.EXPIRED, Position.MID, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);

        contract.correct(date(2025, 11, 20), date(2027, 11, 15), date(2026, 5, 31), EndReason.RELEASED,
                Position.TOP, "정정");

        assertThat(contract.getStartDate()).isEqualTo(date(2025, 11, 20));
        assertThat(contract.lastDay()).isEqualTo(date(2026, 5, 31));
        assertThat(contract.getPosition()).isEqualTo(Position.TOP);
    }
}
