package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.adapter.out.persistence.entity.PatchNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchNoteJpaRepository extends JpaRepository<PatchNoteEntity, String> {
}
