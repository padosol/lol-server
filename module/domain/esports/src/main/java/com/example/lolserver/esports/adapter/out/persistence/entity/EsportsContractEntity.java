package com.example.lolserver.esports.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "esports_contract")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsportsContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "person_id", nullable = false, length = 30)
    private String personId;

    @Column(name = "team_id", nullable = false, length = 30)
    private String teamId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "position", length = 10)
    private String position;

    @Column(name = "contract_type", nullable = false, length = 20)
    private String contractType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "end_reason", length = 20)
    private String endReason;

    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
