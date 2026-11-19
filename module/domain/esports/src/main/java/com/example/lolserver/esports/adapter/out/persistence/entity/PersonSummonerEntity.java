package com.example.lolserver.esports.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "person_summoner")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSummonerEntity {

    @Id
    @Column(name = "puuid", length = 255)
    private String puuid;

    @Column(name = "person_id", nullable = false, length = 30)
    private String personId;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
