package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 계약으로 표현되지 않는 커리어 사건 (은퇴·은퇴 번복·활동 중단·활동 재개). */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CareerEvent {

    private Long eventId;
    private String personId;
    private CareerEventType eventType;
    private LocalDate eventDate;
    private String teamId;
    private HiatusReason reason;
    private String note;
    private String sourceUrl;

    public static CareerEvent of(String personId, CareerEventType type, LocalDate date, String teamId,
                                 HiatusReason reason, String note, String sourceUrl) {
        if (type == null || date == null || (reason != null && type != CareerEventType.HIATUS)) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        return CareerEvent.builder()
                .personId(personId)
                .eventType(type)
                .eventDate(date)
                .teamId(teamId)
                .reason(reason)
                .note(note)
                .sourceUrl(sourceUrl)
                .build();
    }

    /** 영속화 어댑터가 INSERT 후 생성된 ID 를 넣는다. 한 번만. */
    public void assignId(Long id) {
        if (eventId != null) {
            throw new IllegalStateException("eventId already assigned: " + eventId);
        }
        this.eventId = id;
    }
}
