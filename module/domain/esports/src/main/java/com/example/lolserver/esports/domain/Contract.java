package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 계약 한 건. 기간은 양 끝 포함 [startDate, lastDay()].
 * {@code endDate} 는 계약서상 만료일(GCD), {@code actualEndDate}·{@code endReason} 은 조기 종료일 때만 있다.
 * {@code organizationId} 는 저장하지 않는 파생값(팀의 조직 ID)으로, 어댑터가 채운다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Contract {

    private Long contractId;
    private String personId;
    private String teamId;
    private String organizationId;
    private Role role;
    private Position position;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualEndDate;
    private EndReason endReason;
    private String note;

    public static Contract open(String personId, Team team, Role role, Position position, ContractType type,
                                LocalDate startDate, LocalDate endDate, String note) {
        if (type == null) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        validateTerms(role, position, startDate, endDate);
        if (type == ContractType.LOAN && role != Role.PLAYER) {
            throw new CoreException(ErrorType.NOT_A_PLAYER);
        }
        return Contract.builder()
                .personId(personId)
                .teamId(team.getTeamId())
                .organizationId(team.organizationId())
                .role(role)
                .position(position)
                .contractType(type)
                .startDate(startDate)
                .endDate(endDate)
                .note(note)
                .build();
    }

    /** 마지막 소속일(포함). null 이면 끝이 정해지지 않았다. */
    public LocalDate lastDay() {
        return actualEndDate != null ? actualEndDate : endDate;
    }

    public boolean isEffectiveOn(LocalDate date) {
        LocalDate last = lastDay();
        return !date.isBefore(startDate) && (last == null || !date.isAfter(last));
    }

    /** [start, last] 와 겹치는가. last 가 null 이면 끝이 없다. */
    public boolean overlaps(LocalDate start, LocalDate last) {
        LocalDate mine = lastDay();
        boolean startsBeforeOtherEnds = last == null || !startDate.isAfter(last);
        boolean endsAfterOtherStarts = mine == null || !mine.isBefore(start);
        return startsBeforeOtherEnds && endsAfterOtherStarts;
    }

    public boolean isLoan() {
        return contractType == ContractType.LOAN;
    }

    /** 만료일 전에 끝낸다. 이미 종료 사유가 있거나 기간 밖의 날이면 거부한다. */
    public void endEarly(LocalDate lastDay, EndReason reason) {
        if (endReason != null) {
            throw new CoreException(ErrorType.CONTRACT_PERIOD_CONFLICT);
        }
        if (lastDay.isBefore(startDate) || (endDate != null && lastDay.isAfter(endDate))) {
            throw new CoreException(ErrorType.NO_ACTIVE_CONTRACT);
        }
        this.actualEndDate = lastDay;
        this.endReason = reason;
    }

    /** 오입력 정정. 사건 규칙을 거치지 않으므로 다른 계약과의 겹침은 DB 제약이 최종 방어한다. */
    public void correct(LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, EndReason endReason,
                        Position position, String note) {
        validateTerms(role, position, startDate, endDate);
        if ((actualEndDate == null) != (endReason == null) || endReason == EndReason.EXPIRED) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        if (actualEndDate != null && (actualEndDate.isBefore(startDate)
                || (endDate != null && actualEndDate.isAfter(endDate)))) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        if (endReason == EndReason.RECALLED && !isLoan()) {
            throw new CoreException(ErrorType.NOT_A_LOAN);
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.actualEndDate = actualEndDate;
        this.endReason = endReason;
        this.position = position;
        this.note = note;
    }

    /** 영속화 어댑터가 INSERT 후 생성된 ID 를 넣는다. 한 번만. */
    public void assignId(Long id) {
        if (contractId != null) {
            throw new IllegalStateException("contractId already assigned: " + contractId);
        }
        this.contractId = id;
    }

    private static void validateTerms(Role role, Position position, LocalDate startDate, LocalDate endDate) {
        if (role == null || startDate == null) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        boolean positionRuleBroken = role == Role.PLAYER ? position == null : position != null;
        if (positionRuleBroken || (endDate != null && endDate.isBefore(startDate))) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
    }
}
