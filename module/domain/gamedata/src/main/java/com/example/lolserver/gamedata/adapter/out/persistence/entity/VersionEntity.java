package com.example.lolserver.gamedata.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "patch_version")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "season_id")
    private Long seasonId;

    @Column(name = "version_value", nullable = false, length = 20)
    private String versionValue;

    /**
     * Data Dragon 정적 데이터 버전 (예: 16.16.1).
     * {@link #versionValue}(예: 16.16)에 마지막 자리를 붙인 형태이나, 핫픽스 때
     * 그 자리가 달라질 수 있어 파생하지 않고 저장한다.
     * 이미 쌓인 행은 아직 값이 없을 수 있어 null 을 허용한다.
     */
    @Column(name = "patch_version_data", length = 20)
    private String patchVersionData;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public VersionEntity(String versionValue) {
        this(versionValue, null);
    }

    public VersionEntity(String versionValue, String patchVersionData) {
        this.versionValue = versionValue;
        this.patchVersionData = patchVersionData;
    }
}
