package com.example.lolserver.repository.champion_stat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "item_metadata")
public class ItemMetadataEntity {

    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "item_category", nullable = false, length = 50)
    private String itemCategory;

    @Column(name = "game_version", length = 20)
    private String gameVersion;
}
