package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 지나간 팀 이름. 기간은 양 끝 포함 [validFrom, validTo]. 현재 이름은 {@link Team} 에 있다. */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamNameHistory {

    private Long historyId;
    private String teamId;
    private String teamCode;
    private String name;
    private String nameEng;
    private String imageUrl;
    private String darkImageUrl;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String note;

    /** 잘못 넣은 이력을 고친다. 기간이 뒤집히면 거부한다 (다른 이력과의 겹침은 DB 제약이 막는다). */
    public void correct(TeamName teamName, LocalDate validFrom, LocalDate validTo, String note) {
        if (validFrom == null || validTo == null || validTo.isBefore(validFrom)) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        this.teamCode = teamName.teamCode();
        this.name = teamName.name();
        this.nameEng = teamName.nameEng();
        this.imageUrl = teamName.imageUrl();
        this.darkImageUrl = teamName.darkImageUrl();
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.note = note;
    }
}
