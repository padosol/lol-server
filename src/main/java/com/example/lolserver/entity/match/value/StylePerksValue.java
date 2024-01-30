package com.example.lolserver.entity.match.value;

import javax.persistence.Embeddable;
import java.util.List;

@Embeddable
public class StylePerksValue {

    private String description;
    private List<String> selections;
}
