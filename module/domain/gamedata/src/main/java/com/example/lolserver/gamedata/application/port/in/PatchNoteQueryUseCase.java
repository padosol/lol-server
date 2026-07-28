package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.readmodel.PatchNoteReadModel;
import com.example.lolserver.gamedata.application.model.readmodel.PatchNoteSummaryReadModel;

import java.util.List;

public interface PatchNoteQueryUseCase {

    List<PatchNoteSummaryReadModel> getAllPatchNotes();

    PatchNoteReadModel getPatchNoteByVersionId(String versionId);
}
