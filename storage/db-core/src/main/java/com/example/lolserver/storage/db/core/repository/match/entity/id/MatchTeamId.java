package com.example.lolserver.storage.db.core.repository.match.entity.id;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MatchTeamId implements Serializable {

    private String match;

    private	int teamId;
}
