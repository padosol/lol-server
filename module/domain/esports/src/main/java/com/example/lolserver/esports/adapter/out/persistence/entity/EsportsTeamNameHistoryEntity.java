package com.example.lolserver.esports.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "esports_team_name_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsportsTeamNameHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "team_id", nullable = false, length = 30)
    private String teamId;

    @Column(name = "team_code", nullable = false, length = 20)
    private String teamCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_eng", length = 100)
    private String nameEng;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "dark_image_url", length = 500)
    private String darkImageUrl;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "note", length = 200)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
