package com.example.lolserver.repository.patchnote.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "patch_note")
public class PatchNoteEntity {

    @Id
    @Column(name = "version_id")
    private String versionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "patch_url")
    private String patchUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
