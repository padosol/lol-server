package com.example.lolserver.domain.patchnote.application.port.out;

import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;

import java.util.List;
import java.util.Optional;

public interface PatchNotePersistencePort {

    List<PatchNoteSummaryReadModel> findAllSummary();

    Optional<PatchNoteReadModel> findByVersionId(String versionId);
}
