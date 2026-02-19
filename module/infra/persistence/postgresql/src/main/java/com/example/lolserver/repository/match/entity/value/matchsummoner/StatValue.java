package com.example.lolserver.repository.match.entity.value.matchsummoner;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

//    public StatValue(ParticipantDto participantDto) {
//        this.defense = participantDto.getPerks().getStatPerks().getDefense();
//        this.flex = participantDto.getPerks().getStatPerks().getFlex();
//        this.offense = participantDto.getPerks().getStatPerks().getOffense();
//    }
}
