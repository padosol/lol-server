package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 시즌·대회 회차 (lck_2026). 시즌 로스터는 계약 기간과 이 기간의 겹침으로 유도한다. */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Season {

    private String seasonId;
    private String leagueId;
    private String name;
    private String nameAcronym;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private int sortOrder;

    /** 기간이 뒤집히면 시즌 로스터 계산(daterange)이 깨지므로 거부한다. */
    public void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
    }
}
