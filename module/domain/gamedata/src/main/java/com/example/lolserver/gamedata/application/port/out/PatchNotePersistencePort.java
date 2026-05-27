package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.application.model.readmodel.PatchNoteReadModel;
import com.example.lolserver.gamedata.application.model.readmodel.PatchNoteSummaryReadModel;

import java.util.List;
import java.util.Optional;

public interface PatchNotePersistencePort {

    List<PatchNoteSummaryReadModel> findAllSummary();

    Optional<PatchNoteReadModel> findByVersionId(String versionId);
}
