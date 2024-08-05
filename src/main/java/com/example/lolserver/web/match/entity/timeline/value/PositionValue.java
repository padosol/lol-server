package com.example.lolserver.web.match.entity.timeline.value;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PositionValue {
    private int x;
    private int y;
}
