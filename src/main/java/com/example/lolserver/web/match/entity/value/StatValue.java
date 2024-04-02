package com.example.lolserver.web.match.entity.value;


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

}
