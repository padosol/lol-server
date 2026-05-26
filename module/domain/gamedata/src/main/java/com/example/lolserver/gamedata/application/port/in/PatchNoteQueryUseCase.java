package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.PatchNoteReadModel;
import com.example.lolserver.gamedata.application.model.PatchNoteSummaryReadModel;

import java.util.List;

public interface PatchNoteQueryUseCase {

    List<PatchNoteSummaryReadModel> getAllPatchNotes();

    PatchNoteReadModel getPatchNoteByVersionId(String versionId);
}
