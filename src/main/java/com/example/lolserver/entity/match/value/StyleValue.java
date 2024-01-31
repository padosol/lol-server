package com.example.lolserver.entity.match.value;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


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
