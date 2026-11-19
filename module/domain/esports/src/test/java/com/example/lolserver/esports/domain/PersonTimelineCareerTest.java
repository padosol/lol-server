package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.HiatusReason;
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
import static com.example.lolserver.esports.domain.RosterFixtures.event;
import static com.example.lolserver.esports.domain.RosterFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 효력 발생일 기준 임대·임대 복귀·방출·은퇴·은퇴 번복·활동 중단·활동 재개 규칙. */
class PersonTimelineCareerTest {

    // ── 임대 ──

    @DisplayName("임대는 원 계약을 그대로 두고 LOAN 계약을 만든다. 포지션을 빼면 원 계약 포지션")
    @Test
    void loan_원계약은_그대로_두고_LOAN을_만든다() {
        Contract parent = contract(1L, GEN, Role.PLAYER, Position.JGL, ContractType.SIGN,
                date(2026, 1, 1), date(2027, 11, 15));
        PersonTimeline timeline = timeline(List.of(parent));

        Contract loan = timeline.loan(T1, null, date(2026, 11, 20), date(2027, 6, 30), null);

        assertThat(timeline.endedContracts()).isEmpty();
        assertThat(parent.getEndReason()).isNull();
        assertThat(loan.getContractType()).isEqualTo(ContractType.LOAN);
        assertThat(loan.getRole()).isEqualTo(Role.PLAYER);
        assertThat(loan.getPosition()).isEqualTo(Position.JGL);
        assertThat(loan.getTeamId()).isEqualTo("t1");
    }

    @DisplayName("코치는 임대할 수 없다")
    @Test
    void loan_코치는_NOT_A_PLAYER() {
        PersonTimeline timeline = timeline(List.of(contract(1L, GEN, Role.COACH, null, ContractType.SIGN,
                date(2026, 1, 1), date(2027, 11, 15))));

        assertError(() -> timeline.loan(T1, null, date(2026, 11, 20), date(2027, 6, 30), null),
                ErrorType.NOT_A_PLAYER);
    }

    @DisplayName("원 계약이 없으면 임대할 수 없다")
    @Test
    void loan_원계약이_없으면_NO_ACTIVE_CONTRACT() {
        PersonTimeline timeline = timeline(List.of());

        assertError(() -> timeline.loan(T1, Position.MID, date(2026, 11, 20), date(2027, 6, 30), null),
                ErrorType.NO_ACTIVE_CONTRACT);
    }

    @DisplayName("같은 조직으로는 임대할 수 없다")
    @Test
    void loan_같은조직이면_SAME_ORGANIZATION() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1A, date(2026, 1, 1), date(2027, 11, 15))));

        assertError(() -> timeline.loan(T1, null, date(2026, 11, 20), date(2027, 6, 30), null),
                ErrorType.SAME_ORGANIZATION);
    }

    @DisplayName("임대 종료일이 원 계약 마지막 날보다 뒤면 거부한다")
    @Test
    void loan_원계약보다_길면_LOAN_EXCEEDS_CONTRACT() {
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15))));

        assertError(() -> timeline.loan(T1, null, date(2026, 11, 20), date(2027, 11, 16), null),
                ErrorType.LOAN_EXCEEDS_CONTRACT);
    }

    @DisplayName("임대 기간이 겹치는 다른 임대가 있으면 거부한다")
    @Test
    void loan_이미_임대중이면_ALREADY_ON_LOAN() {
        Contract parent = player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15));
        Contract loan = contract(2L, T1, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2026, 12, 31));
        PersonTimeline timeline = timeline(List.of(parent, loan));

        Team other = Team.builder().teamId("hle").teamCode("HLE").gameCode("lol").name("HLE").build();
        assertError(() -> timeline.loan(other, null, date(2026, 11, 20), date(2027, 6, 30), null),
                ErrorType.ALREADY_ON_LOAN);
    }

    // ── 임대 복귀 ──

    @DisplayName("임대 조기 복귀는 임대를 그날 RECALLED 로 끝낸다")
    @Test
    void recall_그날_RECALLED로_종료() {
        Contract loan = contract(2L, T1, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2026, 12, 31));
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15)), loan));

        timeline.recall(2L, date(2026, 9, 1));

        assertThat(loan.getActualEndDate()).isEqualTo(date(2026, 9, 1));
        assertThat(loan.getEndReason()).isEqualTo(EndReason.RECALLED);
        assertThat(timeline.endedContracts()).containsExactly(loan);
    }

    @DisplayName("임대 계약이 아니면(없는 ID 포함) 복귀시킬 수 없다")
    @Test
    void recall_임대가_아니면_NOT_A_LOAN() {
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15))));

        assertError(() -> timeline.recall(1L, date(2026, 9, 1)), ErrorType.NOT_A_LOAN);
        assertError(() -> timeline.recall(99L, date(2026, 9, 1)), ErrorType.NOT_A_LOAN);
    }

    @DisplayName("이미 끝난 임대는 복귀시킬 수 없다")
    @Test
    void recall_이미_끝난_임대면_NO_ACTIVE_CONTRACT() {
        Contract loan = contract(2L, T1, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2026, 8, 31));
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15)), loan));

        assertError(() -> timeline.recall(2L, date(2026, 9, 1)), ErrorType.NO_ACTIVE_CONTRACT);
    }

    // ── 방출 ──

    @DisplayName("방출은 정식 계약과 진행 중인 임대를 그날 RELEASED 로 끝낸다")
    @Test
    void release_정식계약과_임대를_그날_RELEASED로_종료() {
        Contract parent = player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15));
        Contract loan = contract(2L, T1, Role.PLAYER, Position.MID, ContractType.LOAN,
                date(2026, 6, 1), date(2026, 12, 31));
        PersonTimeline timeline = timeline(List.of(parent, loan));

        timeline.release(date(2026, 11, 20));

        assertThat(parent.getActualEndDate()).isEqualTo(date(2026, 11, 20));
        assertThat(parent.getEndReason()).isEqualTo(EndReason.RELEASED);
        assertThat(loan.getEndReason()).isEqualTo(EndReason.RELEASED);
        assertThat(timeline.endedContracts()).containsExactlyInAnyOrder(parent, loan);
    }

    @DisplayName("진행 중인 계약이 없으면 방출할 수 없다")
    @Test
    void release_계약이_없으면_NO_ACTIVE_CONTRACT() {
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2025, 11, 18), date(2026, 11, 16))));

        assertError(() -> timeline.release(date(2026, 11, 20)), ErrorType.NO_ACTIVE_CONTRACT);
    }

    // ── 은퇴 ──

    @DisplayName("은퇴하면 선수 계약을 그날 RETIRED 로 끝내고 원 계약 팀으로 RETIRE 사건을 남긴다")
    @Test
    void retire_선수계약을_RETIRED로_끝내고_RETIRE사건을_남긴다() {
        Contract parent = player(1L, GEN, date(2026, 1, 1), date(2027, 11, 15));
        PersonTimeline timeline = timeline(List.of(parent));

        CareerEvent retire = timeline.retire(date(2026, 11, 20), "은퇴 발표", "https://example.com/retire");

        assertThat(parent.getActualEndDate()).isEqualTo(date(2026, 11, 20));
        assertThat(parent.getEndReason()).isEqualTo(EndReason.RETIRED);
        assertThat(retire.getEventType()).isEqualTo(CareerEventType.RETIRE);
        assertThat(retire.getEventDate()).isEqualTo(date(2026, 11, 20));
        assertThat(retire.getTeamId()).isEqualTo("gen");
        assertThat(retire.getSourceUrl()).isEqualTo("https://example.com/retire");
        assertThat(timeline.newEvents()).containsExactly(retire);
    }

    @DisplayName("FA 가 은퇴하면 사건만 남는다")
    @Test
    void retire_FA는_사건만_남긴다() {
        PersonTimeline timeline = timeline(List.of(player(1L, GEN, date(2025, 11, 18), date(2026, 11, 16))));

        CareerEvent retire = timeline.retire(date(2026, 11, 20), null, null);

        assertThat(timeline.endedContracts()).isEmpty();
        assertThat(retire.getTeamId()).isNull();
    }

    @DisplayName("이미 은퇴 상태면 다시 은퇴할 수 없다")
    @Test
    void retire_이미_은퇴면_PERSON_RETIRED() {
        PersonTimeline timeline = timeline(List.of(), List.of(event(1L, CareerEventType.RETIRE, date(2026, 1, 10))));

        assertError(() -> timeline.retire(date(2026, 11, 20), null, null), ErrorType.PERSON_RETIRED);
    }

    @DisplayName("스태프 계약 중에는 선수 은퇴를 넣을 수 없다 (스태프가 떠나면 방출이다)")
    @Test
    void retire_스태프계약중이면_NOT_A_PLAYER() {
        PersonTimeline timeline = timeline(List.of(contract(1L, T1, Role.COACH, null, ContractType.SIGN,
                date(2026, 1, 1), date(2027, 11, 15))));

        assertError(() -> timeline.retire(date(2026, 11, 20), null, null), ErrorType.NOT_A_PLAYER);
    }

    // ── 은퇴 번복 ──

    @DisplayName("은퇴 상태가 아니면 은퇴 번복을 넣을 수 없다")
    @Test
    void unretire_은퇴상태가_아니면_PERSON_NOT_RETIRED() {
        PersonTimeline timeline = timeline(List.of());

        assertError(() -> timeline.unretire(date(2026, 11, 20), null, null), ErrorType.PERSON_NOT_RETIRED);
    }

    @DisplayName("은퇴 번복 뒤에는 선수로 입단할 수 있다")
    @Test
    void unretire_후에는_선수로_입단할수있다() {
        PersonTimeline timeline = timeline(List.of(), List.of(event(1L, CareerEventType.RETIRE, date(2026, 1, 10))));

        timeline.unretire(date(2026, 11, 17), null, null);
        Contract signed = timeline.sign(T1, Role.PLAYER, Position.MID, date(2026, 11, 20), date(2027, 11, 15), null);

        assertThat(signed.getRole()).isEqualTo(Role.PLAYER);
        assertThat(timeline.newEvents()).extracting(CareerEvent::getEventType)
                .containsExactly(CareerEventType.UNRETIRE);
    }

    // ── 활동 중단 · 재개 ──

    @DisplayName("활동 중단은 사유와 함께 HIATUS 사건으로 남는다")
    @Test
    void hiatus_사유를_남긴다() {
        PersonTimeline timeline = timeline(List.of(player(1L, T1, date(2026, 1, 1), date(2027, 11, 15))));

        CareerEvent hiatus = timeline.hiatus(date(2026, 11, 20), HiatusReason.MILITARY, "입대", null);

        assertThat(hiatus.getEventType()).isEqualTo(CareerEventType.HIATUS);
        assertThat(hiatus.getReason()).isEqualTo(HiatusReason.MILITARY);
        assertThat(timeline.endedContracts()).isEmpty();
    }

    @DisplayName("이미 활동 중단 상태면 다시 넣을 수 없다")
    @Test
    void hiatus_이미_비활동이면_ALREADY_INACTIVE() {
        PersonTimeline timeline = timeline(List.of(), List.of(event(1L, CareerEventType.HIATUS, date(2026, 6, 1))));

        assertError(() -> timeline.hiatus(date(2026, 11, 20), HiatusReason.REST, null, null),
                ErrorType.ALREADY_INACTIVE);
    }

    @DisplayName("활동 중단 상태가 아니면 활동 재개를 넣을 수 없다")
    @Test
    void comeBack_비활동이_아니면_NOT_INACTIVE() {
        PersonTimeline timeline = timeline(List.of(), List.of(
                event(1L, CareerEventType.HIATUS, date(2026, 6, 1)),
                event(2L, CareerEventType.RETURN, date(2026, 8, 1))));

        assertError(() -> timeline.comeBack(date(2026, 11, 20), null, null), ErrorType.NOT_INACTIVE);
        CareerEvent back = timeline.comeBack(date(2026, 7, 1), null, null);
        assertThat(back.getEventType()).isEqualTo(CareerEventType.RETURN);
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
