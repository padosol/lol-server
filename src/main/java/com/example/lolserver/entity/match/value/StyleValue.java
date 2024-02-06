package com.example.lolserver.entity.match.value;


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
}
