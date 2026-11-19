package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.example.lolserver.esports.domain.RosterFixtures.GEN;
import static com.example.lolserver.esports.domain.RosterFixtures.PERSON;
import static com.example.lolserver.esports.domain.RosterFixtures.T1;
import static com.example.lolserver.esports.domain.RosterFixtures.T1A;
import static com.example.lolserver.esports.domain.RosterFixtures.contract;
import static com.example.lolserver.esports.domain.RosterFixtures.date;
import static com.example.lolserver.esports.domain.RosterFixtures.endedPlayer;
import static com.example.lolserver.esports.domain.RosterFixtures.event;
import static com.example.lolserver.esports.domain.RosterFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 효력 발생일 기준 입단·재계약·이적·팀 내 이동 규칙. 날짜는 2026 시즌(GCD 종료일 2026-11-16) 기준. */
class PersonTimelineContractTest {

    // ── 입단 ──

    @DisplayName("계약이 없는 FA 는 새 팀에 입단한다")
    @Test
    void sign_FA_입단() {
        PersonTimeline timeline = timeline(List.of());

        Contract signed = timeline.sign(GEN, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15), null);

        assertThat(signed.getContractType()).isEqualTo(ContractType.SIGN);
        assertThat(signed.getTeamId()).isEqualTo("gen");
        assertThat(timeline.newContracts()).containsExactly(signed);
        assertThat(timeline.endedContracts()).isEmpty();
    }

    @DisplayName("진행 중인 정식 계약과 기간이 겹치면 입단을 거부한다")
    @Test
    void sign_정식계약과_겹치면_CONTRACT_PERIOD_CONFLICT() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2025, 11, 18), date(2027, 11, 15))));

        assertError(() -> timeline.sign(GEN, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15), null),
                ErrorType.CONTRACT_PERIOD_CONFLICT);
    }

    @DisplayName("은퇴 상태에서는 선수로 입단할 수 없다")
    @Test
    void sign_은퇴상태_선수계약은_PERSON_RETIRED() {
        PersonTimeline timeline = timeline(List.of(), List.of(event(1L, CareerEventType.RETIRE, date(2026, 1, 10))));

        assertError(() -> timeline.sign(GEN, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15), null),
                ErrorType.PERSON_RETIRED);
    }

    @DisplayName("은퇴한 선수도 코치로는 입단할 수 있다")
    @Test
    void sign_은퇴상태라도_코치계약은_허용() {
        PersonTimeline timeline = timeline(List.of(), List.of(event(1L, CareerEventType.RETIRE, date(2026, 1, 10))));

        Contract coach = timeline.sign(T1, Role.COACH, null, date(2026, 11, 17), date(2027, 11, 15), null);

        assertThat(coach.getRole()).isEqualTo(Role.COACH);
    }

    @DisplayName("지난 이력은 뒤 기록과 겹치지 않으면 사이에 끼워 넣는다")
    @Test
    void sign_과거이력은_뒤기록과_겹치지않으면_끼워넣는다() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2019, 11, 18), date(2021, 11, 15))));

        Contract past = timeline.sign(GEN, Role.PLAYER, Position.MID, date(2017, 11, 20), date(2018, 11, 18), null);

        assertThat(past.getStartDate()).isEqualTo(date(2017, 11, 20));
    }

    // ── 재계약 ──

    @DisplayName("만료 다음 날 재계약은 직전 계약을 건드리지 않고 이어 붙인다 (스토브리그 기본)")
    @Test
    void renew_만료다음날_재계약은_직전계약을_건드리지않는다() {
        Contract prev = player(1L, T1, date(2024, 11, 18), date(2026, 11, 16));
        PersonTimeline timeline = timeline(List.of(prev));

        Contract renewal = timeline.renew(T1, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2028, 11, 20), null);

        assertThat(timeline.endedContracts()).isEmpty();
        assertThat(prev.getEndReason()).isNull();
        assertThat(renewal.getContractType()).isEqualTo(ContractType.RENEWAL);
        assertThat(renewal.getStartDate()).isEqualTo(date(2026, 11, 17));
        assertThat(renewal.getEndDate()).isEqualTo(date(2028, 11, 20));
    }

    @DisplayName("만료 전 연장은 직전 계약을 시작일 전날 RENEWED 로 끝낸다")
    @Test
    void renew_만료전_연장은_직전계약을_RENEWED로_조기종료() {
        Contract prev = player(1L, T1, date(2024, 11, 18), date(2026, 11, 16));
        PersonTimeline timeline = timeline(List.of(prev));

        timeline.renew(T1, Role.PLAYER, Position.MID, date(2026, 3, 1), date(2029, 11, 19), null);

        assertThat(prev.getActualEndDate()).isEqualTo(date(2026, 2, 28));
        assertThat(prev.getEndReason()).isEqualTo(EndReason.RENEWED);
        assertThat(timeline.endedContracts()).containsExactly(prev);
    }

    @DisplayName("직전 계약과 공백이 있으면 재계약이 아니다 (신규 입단으로 넣는다)")
    @Test
    void renew_공백뒤면_NOT_A_CONTINUATION() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2024, 11, 18), date(2026, 11, 16))));

        assertError(() -> timeline.renew(T1, Role.PLAYER, Position.MID, date(2026, 12, 1), date(2028, 11, 20), null),
                ErrorType.NOT_A_CONTINUATION);
    }

    @DisplayName("같은 조직의 다른 팀으로 재계약하며 승격할 수 있다 (T1A → T1)")
    @Test
    void renew_같은조직_다른팀으로_재계약하며_승격() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1A, date(2025, 11, 18), date(2026, 11, 16))));

        Contract renewal = timeline.renew(T1, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15), null);

        assertThat(renewal.getTeamId()).isEqualTo("t1");
        assertThat(renewal.getContractType()).isEqualTo(ContractType.RENEWAL);
    }

    @DisplayName("다른 조직과는 재계약할 수 없다")
    @Test
    void renew_다른조직이면_NOT_A_CONTINUATION() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2024, 11, 18), date(2026, 11, 16))));

        assertError(() -> timeline.renew(GEN, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15), null),
                ErrorType.NOT_A_CONTINUATION);
    }

    @DisplayName("선수 계약을 코치 계약으로 재계약할 수 없다")
    @Test
    void renew_선수에서_코치면_PLAYER_STAFF_SWITCH() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2024, 11, 18), date(2026, 11, 16))));

        assertError(() -> timeline.renew(T1, Role.COACH, null, date(2026, 11, 17), date(2027, 11, 15), null),
                ErrorType.PLAYER_STAFF_SWITCH);
    }

    // ── 이적 ──

    @DisplayName("이적하면 현재 계약은 전날 TRANSFERRED 로 끝나고 새 팀에 TRANSFER 계약이 생긴다")
    @Test
    void transfer_직전계약을_전날_TRANSFERRED로_종료() {
        Contract prev = player(1L, T1, date(2025, 11, 18), date(2027, 11, 15));
        PersonTimeline timeline = timeline(List.of(prev));

        Contract moved = timeline.transfer(GEN, Role.PLAYER, Position.MID, date(2026, 11, 20), date(2028, 11, 20), null);

        assertThat(prev.getActualEndDate()).isEqualTo(date(2026, 11, 19));
        assertThat(prev.getEndReason()).isEqualTo(EndReason.TRANSFERRED);
        assertThat(moved.getContractType()).isEqualTo(ContractType.TRANSFER);
        assertThat(moved.getStartDate()).isEqualTo(date(2026, 11, 20));
        assertThat(timeline.endedContracts()).containsExactly(prev);
        assertThat(timeline.newContracts()).containsExactly(moved);
    }

    @DisplayName("만료 다음 날 다른 팀에 가면 이적이 아니라 FA 입단이다")
    @Test
    void transfer_만료다음날은_NO_ACTIVE_CONTRACT() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2024, 11, 18), date(2026, 11, 16))));

        assertError(() -> timeline.transfer(GEN, Role.PLAYER, Position.MID, date(2026, 11, 17), date(2027, 11, 15),
                null), ErrorType.NO_ACTIVE_CONTRACT);
    }

    @DisplayName("같은 조직으로는 이적할 수 없다 (팀 내 이동이다)")
    @Test
    void transfer_같은조직이면_SAME_ORGANIZATION() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1A, date(2025, 11, 18), date(2027, 11, 15))));

        assertError(() -> timeline.transfer(T1, Role.PLAYER, Position.MID, date(2026, 11, 20), date(2027, 11, 15),
                null), ErrorType.SAME_ORGANIZATION);
    }

    @DisplayName("임차팀으로 이적하면(구매 선택권) 임대 계약도 전날 TRANSFERRED 로 끝난다")
    @Test
    void transfer_임차팀으로_이적하면_임대도_TRANSFERRED() {
        Contract parent = player(1L, T1, date(2025, 11, 18), date(2027, 11, 15));
        Contract loan = contract(2L, GEN, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2027, 6, 30));
        PersonTimeline timeline = timeline(List.of(parent, loan));

        timeline.transfer(GEN, Role.PLAYER, Position.MID, date(2026, 11, 20), date(2028, 11, 20), null);

        assertThat(parent.getEndReason()).isEqualTo(EndReason.TRANSFERRED);
        assertThat(loan.getEndReason()).isEqualTo(EndReason.TRANSFERRED);
        assertThat(loan.getActualEndDate()).isEqualTo(date(2026, 11, 19));
        assertThat(timeline.endedContracts()).containsExactlyInAnyOrder(parent, loan);
    }

    @DisplayName("다른 팀에 임대 중인 선수는 이적시킬 수 없다")
    @Test
    void transfer_다른팀에_임대중이면_ALREADY_ON_LOAN() {
        Contract parent = player(1L, T1, date(2025, 11, 18), date(2027, 11, 15));
        Contract loan = contract(2L, GEN, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2027, 6, 30));
        PersonTimeline timeline = timeline(List.of(parent, loan));

        Team other = Team.builder().teamId("hle").teamCode("HLE").gameCode("lol").name("HLE").build();
        assertError(() -> timeline.transfer(other, Role.PLAYER, Position.MID, date(2026, 11, 20),
                date(2028, 11, 20), null), ErrorType.ALREADY_ON_LOAN);
    }

    @DisplayName("조기 종료가 이미 기록된 계약은 이적으로 다시 끝낼 수 없다")
    @Test
    void transfer_조기종료가_이미_기록된_계약이면_CONTRACT_PERIOD_CONFLICT() {
        PersonTimeline timeline = timeline(List.of(endedPlayer(1L, T1, date(2025, 11, 18), date(2027, 11, 15),
                date(2027, 5, 31), EndReason.RELEASED)));

        assertError(() -> timeline.transfer(GEN, Role.PLAYER, Position.MID, date(2026, 11, 20), date(2028, 11, 20),
                null), ErrorType.CONTRACT_PERIOD_CONFLICT);
    }

    // ── 팀 내 이동 ──

    @DisplayName("콜업하면 CL 계약은 전날 MOVED 로 끝나고 1군 MOVE 계약이 end_date 를 물려받는다")
    @Test
    void move_콜업은_end_date를_물려받는다() {
        Contract prev = player(1L, T1A, date(2026, 1, 1), date(2026, 11, 16));
        PersonTimeline timeline = timeline(List.of(prev));

        Contract moved = timeline.move(T1, Role.PLAYER, Position.MID, date(2026, 6, 1), null);

        assertThat(prev.getActualEndDate()).isEqualTo(date(2026, 5, 31));
        assertThat(prev.getEndReason()).isEqualTo(EndReason.MOVED);
        assertThat(moved.getContractType()).isEqualTo(ContractType.MOVE);
        assertThat(moved.getStartDate()).isEqualTo(date(2026, 6, 1));
        assertThat(moved.getEndDate()).isEqualTo(date(2026, 11, 16));
    }

    @DisplayName("같은 팀에서 포지션만 바꿔도 팀 내 이동이다")
    @Test
    void move_같은팀_포지션전향은_허용() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2026, 1, 1), date(2026, 11, 16))));

        Contract moved = timeline.move(T1, Role.PLAYER, Position.TOP, date(2026, 6, 1), null);

        assertThat(moved.getPosition()).isEqualTo(Position.TOP);
    }

    @DisplayName("코치가 감독으로 승격하는 것도 팀 내 이동이다")
    @Test
    void move_코치에서_감독은_허용() {
        PersonTimeline timeline = timeline(List.of(contract(1L, T1, Role.COACH, null, ContractType.SIGN,
                date(2026, 1, 1), date(2026, 11, 16))));

        Contract moved = timeline.move(T1, Role.HEAD_COACH, null, date(2026, 6, 1), null);

        assertThat(moved.getRole()).isEqualTo(Role.HEAD_COACH);
    }

    @DisplayName("팀·역할·포지션이 모두 같으면 거부한다")
    @Test
    void move_바뀐것이_없으면_NOTHING_CHANGED() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2026, 1, 1), date(2026, 11, 16))));

        assertError(() -> timeline.move(T1, Role.PLAYER, Position.MID, date(2026, 6, 1), null),
                ErrorType.NOTHING_CHANGED);
    }

    @DisplayName("다른 조직으로는 팀 내 이동이 아니라 이적이다")
    @Test
    void move_다른조직이면_DIFFERENT_ORGANIZATION() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2026, 1, 1), date(2026, 11, 16))));

        assertError(() -> timeline.move(GEN, Role.PLAYER, Position.MID, date(2026, 6, 1), null),
                ErrorType.DIFFERENT_ORGANIZATION);
    }

    @DisplayName("선수가 코치가 되는 것은 팀 내 이동이 아니다 (다른 계약이다)")
    @Test
    void move_선수에서_코치면_PLAYER_STAFF_SWITCH() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2026, 1, 1), date(2026, 11, 16))));

        assertError(() -> timeline.move(T1, Role.COACH, null, date(2026, 6, 1), null),
                ErrorType.PLAYER_STAFF_SWITCH);
    }

    @DisplayName("임대 중인 선수는 팀 내 이동시킬 수 없다")
    @Test
    void move_임대중이면_ALREADY_ON_LOAN() {
        Contract parent = player(1L, T1A, date(2026, 1, 1), date(2026, 11, 16));
        Contract loan = contract(2L, GEN, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 3, 1), date(2026, 8, 31));
        PersonTimeline timeline = timeline(List.of(parent, loan));

        assertError(() -> timeline.move(T1, Role.PLAYER, Position.MID, date(2026, 6, 1), null),
                ErrorType.ALREADY_ON_LOAN);
    }

    private static PersonTimeline timeline(List<Contract> contracts) {
        return timeline(contracts, List.of());
    }

    private static PersonTimeline timeline(List<Contract> contracts, List<CareerEvent> events) {
        return new PersonTimeline(PERSON, new ArrayList<>(contracts), new ArrayList<>(events));
    }

    private static void assertError(ThrowingCallable call, ErrorType expected) {
        assertThatThrownBy(call)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(expected);
    }
}
