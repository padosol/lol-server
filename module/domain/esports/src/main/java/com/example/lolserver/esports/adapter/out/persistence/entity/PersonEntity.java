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
@Table(name = "person")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonEntity {

    @Id
    @Column(name = "person_id", length = 30)
    private String personId;

    @Column(name = "nick_name", nullable = false, length = 100)
    private String nickName;

    @Column(name = "real_name", length = 100)
    private String realName;

    @Column(name = "real_name_eng", length = 100)
    private String realNameEng;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nationality", length = 2)
    private String nationality;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
