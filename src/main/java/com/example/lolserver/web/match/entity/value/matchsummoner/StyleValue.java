package com.example.lolserver.web.match.entity.value.matchsummoner;


import com.example.lolserver.riot.dto.match.ParticipantDto;
import jakarta.persistence.Embeddable;
import lombok.*;


@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleValue {

    private int primaryRuneId;
    private String primaryRuneIds;

    private int secondaryRuneId;
    private String secondaryRuneIds;

    public StyleValue(ParticipantDto participantDto) {
    }
}
