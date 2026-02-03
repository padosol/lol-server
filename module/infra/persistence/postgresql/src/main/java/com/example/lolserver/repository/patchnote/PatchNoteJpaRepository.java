package com.example.lolserver.repository.patchnote;

import com.example.lolserver.repository.patchnote.entity.PatchNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchNoteJpaRepository extends JpaRepository<PatchNoteEntity, String> {
}
