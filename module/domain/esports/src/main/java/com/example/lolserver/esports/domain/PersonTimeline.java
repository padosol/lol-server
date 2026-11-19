package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 인물 한 명의 계약·커리어 사건 묶음. 사건 규칙을 효력 발생일({@code date}) 기준으로 전체 타임라인에 대해 검사한다.
 * 오늘이 아니라 효력일 기준이므로 지난 이력 입력(과거 날짜)과 발표 후 효력 전 입력(미래 날짜)을 모두 받는다.
 *
 * <p>"정식 계약" = 임대가 아닌 계약. 정식 계약끼리, 임대끼리는 기간이 겹칠 수 없다.
 * 사건이 바꾼 것은 {@link #endedContracts()}(조기 종료된 기존 계약 — 먼저 UPDATE),
 * {@link #newContracts()}, {@link #newEvents()} 로 꺼낸다.
 */
public class PersonTimeline {

    @Getter
    private final String personId;
    private final List<Contract> contracts;
    private final List<CareerEvent> events;
    private final List<Contract> endedContracts = new ArrayList<>();
    private final List<Contract> newContracts = new ArrayList<>();
    private final List<CareerEvent> newEvents = new ArrayList<>();

    public PersonTimeline(String personId, List<Contract> contracts, List<CareerEvent> events) {
        this.personId = personId;
        this.contracts = new ArrayList<>(contracts);
        this.events = new ArrayList<>(events);
    }

    public List<Contract> getContracts() {
        return Collections.unmodifiableList(contracts);
    }

    public List<CareerEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Contract> endedContracts() {
        return Collections.unmodifiableList(endedContracts);
    }

    public List<Contract> newContracts() {
        return Collections.unmodifiableList(newContracts);
    }

    public List<CareerEvent> newEvents() {
        return Collections.unmodifiableList(newEvents);
    }

    /** 신규 입단 (FA 영입·신인·스태프 합류). 은퇴 상태면 선수로는 입단할 수 없다. */
    public Contract sign(Team team, Role role, Position position, LocalDate date, LocalDate endDate, String note) {
        if (role == Role.PLAYER && retiredOn(date)) {
            throw new CoreException(ErrorType.PERSON_RETIRED);
        }
        return add(Contract.open(personId, team, role, position, ContractType.SIGN, date, endDate, note));
    }

    /**
     * 재계약. {@code date} 전날 유효한 같은 조직 정식 계약을 잇는다.
     * 그 계약이 전날 끝나면 그대로 이어 붙이고(만료 후 재계약), 더 뒤에 끝나면 전날 RENEWED 로 조기 종료한다(만료 전 연장).
     */
    public Contract renew(Team team, Role role, Position position, LocalDate date, LocalDate endDate, String note) {
        LocalDate dayBefore = date.minusDays(1);
        Contract prev = contracts.stream()
                .filter(c -> !c.isLoan() && c.isEffectiveOn(dayBefore) && c.getEndReason() == null)
                .filter(c -> c.getOrganizationId().equals(team.organizationId()))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_A_CONTINUATION));
        validateSameRoleGroup(prev, role);
        Contract renewal = Contract.open(personId, team, role, position, ContractType.RENEWAL, date, endDate, note);
        if (prev.lastDay() == null || prev.lastDay().isAfter(dayBefore)) {
            end(prev, dayBefore, EndReason.RENEWED);
        }
        return add(renewal);
    }

    /**
     * 이적(트레이드). 효력일에도 이어지던 정식 계약을 전날 TRANSFERRED 로 끝내고 다른 조직에 TRANSFER 계약을 만든다.
     * 임차팀으로 이적하면(구매 선택권) 임대도 함께 끝난다. 다른 팀에 임대 중이면 이적시킬 수 없다.
     */
    public Contract transfer(Team team, Role role, Position position, LocalDate date, LocalDate endDate,
                             String note) {
        Contract prev = runningContract(date);
        if (prev.getOrganizationId().equals(team.organizationId())) {
            throw new CoreException(ErrorType.SAME_ORGANIZATION);
        }
        validateSameRoleGroup(prev, role);
        Contract transfer = Contract.open(personId, team, role, position, ContractType.TRANSFER, date, endDate, note);
        Optional<Contract> loan = loanOn(date);
        if (loan.isPresent() && !loan.get().getOrganizationId().equals(team.organizationId())) {
            throw new CoreException(ErrorType.ALREADY_ON_LOAN);
        }
        LocalDate dayBefore = date.minusDays(1);
        end(prev, dayBefore, EndReason.TRANSFERRED);
        loan.ifPresent(l -> end(l, dayBefore, EndReason.TRANSFERRED));
        return add(transfer);
    }

    /**
     * 팀 내 이동 (콜업·샌드다운·스태프 역할 변경·선수 포지션 전향). 계약서는 하나이므로
     * 현재 계약을 전날 MOVED 로 끝내고 같은 조직에 MOVE 계약을 만들어 end_date 를 물려준다.
     */
    public Contract move(Team team, Role role, Position position, LocalDate date, String note) {
        Contract prev = runningContract(date);
        if (!prev.getOrganizationId().equals(team.organizationId())) {
            throw new CoreException(ErrorType.DIFFERENT_ORGANIZATION);
        }
        boolean unchanged = prev.getTeamId().equals(team.getTeamId()) && prev.getRole() == role
                && Objects.equals(prev.getPosition(), position);
        if (unchanged) {
            throw new CoreException(ErrorType.NOTHING_CHANGED);
        }
        validateSameRoleGroup(prev, role);
        if (loanOn(date).isPresent()) {
            throw new CoreException(ErrorType.ALREADY_ON_LOAN);
        }
        Contract moved = Contract.open(personId, team, role, position, ContractType.MOVE, date,
                prev.getEndDate(), note);
        end(prev, date.minusDays(1), EndReason.MOVED);
        return add(moved);
    }

    /**
     * 임대. 원 계약은 그대로 두고 다른 조직의 임차팀에 LOAN 계약을 만든다.
     * 임대는 원 계약 기간을 넘을 수 없고, 임대끼리는 겹칠 수 없다. 포지션을 빼면 원 계약 포지션을 쓴다.
     */
    public Contract loan(Team team, Position position, LocalDate date, LocalDate endDate, String note) {
        Contract parent = mainContractOn(date)
                .orElseThrow(() -> new CoreException(ErrorType.NO_ACTIVE_CONTRACT));
        if (parent.getRole() != Role.PLAYER) {
            throw new CoreException(ErrorType.NOT_A_PLAYER);
        }
        if (parent.getOrganizationId().equals(team.organizationId())) {
            throw new CoreException(ErrorType.SAME_ORGANIZATION);
        }
        if (endDate == null) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        if (parent.lastDay() != null && endDate.isAfter(parent.lastDay())) {
            throw new CoreException(ErrorType.LOAN_EXCEEDS_CONTRACT);
        }
        boolean loanOverlaps = contracts.stream().anyMatch(c -> c.isLoan() && c.overlaps(date, endDate));
        if (loanOverlaps) {
            throw new CoreException(ErrorType.ALREADY_ON_LOAN);
        }
        Position loanPosition = position != null ? position : parent.getPosition();
        return add(Contract.open(personId, team, Role.PLAYER, loanPosition, ContractType.LOAN, date, endDate, note));
    }

    /** 임대 조기 복귀. 임대를 그날 RECALLED 로 끝낸다. */
    public void recall(Long loanContractId, LocalDate date) {
        Contract loan = contracts.stream()
                .filter(c -> c.isLoan() && Objects.equals(c.getContractId(), loanContractId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_A_LOAN));
        if (!loan.isEffectiveOn(date)) {
            throw new CoreException(ErrorType.NO_ACTIVE_CONTRACT);
        }
        end(loan, date, EndReason.RECALLED);
    }

    /** 방출·상호 해지. 정식 계약과 진행 중인 임대를 그날 RELEASED 로 끝낸다. */
    public void release(LocalDate date) {
        Contract main = mainContractOn(date)
                .orElseThrow(() -> new CoreException(ErrorType.NO_ACTIVE_CONTRACT));
        end(main, date, EndReason.RELEASED);
        loanOn(date).ifPresent(loan -> end(loan, date, EndReason.RELEASED));
    }

    /**
     * 선수 은퇴. 진행 중인 선수 계약(과 임대)을 그날 RETIRED 로 끝내고 RETIRE 사건을 남긴다.
     * FA 은퇴는 사건만 남는다. 스태프 계약 중이면 은퇴가 아니라 방출로 넣는다.
     */
    public CareerEvent retire(LocalDate date, String note, String sourceUrl) {
        if (retiredOn(date)) {
            throw new CoreException(ErrorType.PERSON_RETIRED);
        }
        Optional<Contract> main = mainContractOn(date);
        if (main.isPresent() && main.get().getRole().isStaff()) {
            throw new CoreException(ErrorType.NOT_A_PLAYER);
        }
        main.ifPresent(contract -> {
            end(contract, date, EndReason.RETIRED);
            loanOn(date).ifPresent(loan -> end(loan, date, EndReason.RETIRED));
        });
        String teamId = main.map(Contract::getTeamId).orElse(null);
        return record(CareerEvent.of(personId, CareerEventType.RETIRE, date, teamId, null, note, sourceUrl));
    }

    /** 은퇴 번복. 은퇴 상태일 때만. */
    public CareerEvent unretire(LocalDate date, String note, String sourceUrl) {
        if (!retiredOn(date)) {
            throw new CoreException(ErrorType.PERSON_NOT_RETIRED);
        }
        return record(CareerEvent.of(personId, CareerEventType.UNRETIRE, date, null, null, note, sourceUrl));
    }

    /** 활동 중단 (휴식·출장정지·군 입대·개인 사유). 계약은 그대로다. */
    public CareerEvent hiatus(LocalDate date, HiatusReason reason, String note, String sourceUrl) {
        if (inactiveOn(date)) {
            throw new CoreException(ErrorType.ALREADY_INACTIVE);
        }
        String teamId = mainContractOn(date).map(Contract::getTeamId).orElse(null);
        return record(CareerEvent.of(personId, CareerEventType.HIATUS, date, teamId, reason, note, sourceUrl));
    }

    /** 활동 재개. 활동 중단 상태일 때만. ({@code return} 은 예약어라 이름이 다르다) */
    public CareerEvent comeBack(LocalDate date, String note, String sourceUrl) {
        if (!inactiveOn(date)) {
            throw new CoreException(ErrorType.NOT_INACTIVE);
        }
        return record(CareerEvent.of(personId, CareerEventType.RETURN, date, null, null, note, sourceUrl));
    }

    // ── 조회 도우미 ──

    /** 효력일 전날부터 효력일까지 이어지는 정식 계약 — 이적·이동으로 끝낼 수 있는 계약. */
    Contract runningContract(LocalDate date) {
        LocalDate dayBefore = date.minusDays(1);
        return mainContractOn(date)
                .filter(c -> c.isEffectiveOn(dayBefore))
                .orElseThrow(() -> new CoreException(ErrorType.NO_ACTIVE_CONTRACT));
    }

    Optional<Contract> mainContractOn(LocalDate date) {
        return contracts.stream().filter(c -> !c.isLoan() && c.isEffectiveOn(date)).findFirst();
    }

    Optional<Contract> loanOn(LocalDate date) {
        return contracts.stream().filter(c -> c.isLoan() && c.isEffectiveOn(date)).findFirst();
    }

    /** {@code date} 이하 마지막 RETIRE/UNRETIRE 가 RETIRE 인가. */
    boolean retiredOn(LocalDate date) {
        return lastEventOn(date, Set.of(CareerEventType.RETIRE, CareerEventType.UNRETIRE))
                .map(e -> e.getEventType() == CareerEventType.RETIRE)
                .orElse(false);
    }

    /** {@code date} 이하 마지막 HIATUS/RETURN 이 HIATUS 인가. */
    boolean inactiveOn(LocalDate date) {
        return lastEventOn(date, Set.of(CareerEventType.HIATUS, CareerEventType.RETURN))
                .map(e -> e.getEventType() == CareerEventType.HIATUS)
                .orElse(false);
    }

    // ── 변경 도우미 ──

    Contract add(Contract candidate) {
        boolean overlaps = contracts.stream()
                .filter(c -> c.isLoan() == candidate.isLoan())
                .anyMatch(c -> c.overlaps(candidate.getStartDate(), candidate.lastDay()));
        if (overlaps) {
            throw new CoreException(ErrorType.CONTRACT_PERIOD_CONFLICT);
        }
        contracts.add(candidate);
        newContracts.add(candidate);
        return candidate;
    }

    void end(Contract contract, LocalDate lastDay, EndReason reason) {
        contract.endEarly(lastDay, reason);
        if (!endedContracts.contains(contract) && !newContracts.contains(contract)) {
            endedContracts.add(contract);
        }
    }

    CareerEvent record(CareerEvent event) {
        events.add(event);
        newEvents.add(event);
        return event;
    }

    private Optional<CareerEvent> lastEventOn(LocalDate date, Set<CareerEventType> types) {
        CareerEvent last = null;
        for (CareerEvent e : events) {
            if (types.contains(e.getEventType()) && !e.getEventDate().isAfter(date)
                    && (last == null || !e.getEventDate().isBefore(last.getEventDate()))) {
                last = e;
            }
        }
        return Optional.ofNullable(last);
    }

    private static void validateSameRoleGroup(Contract prev, Role role) {
        if (role != null && prev.getRole().isStaff() != role.isStaff()) {
            throw new CoreException(ErrorType.PLAYER_STAFF_SWITCH);
        }
    }
}
