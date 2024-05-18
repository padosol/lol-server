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
public class StatValue {

    private int defense;
    private int flex;
    private int offense;

    public StatValue(ParticipantDto participantDto) {
        this.defense = participantDto.getPerks().getStatPerks().getDefense();
        this.flex = participantDto.getPerks().getStatPerks().getFlex();
        this.offense = participantDto.getPerks().getStatPerks().getOffense();
    }
}
