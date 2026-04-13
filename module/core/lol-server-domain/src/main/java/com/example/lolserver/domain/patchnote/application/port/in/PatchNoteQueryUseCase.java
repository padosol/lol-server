package com.example.lolserver.domain.patchnote.application.port.in;

import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;

import java.util.List;

public interface PatchNoteQueryUseCase {

    List<PatchNoteSummaryReadModel> getAllPatchNotes();

    PatchNoteReadModel getPatchNoteByVersionId(String versionId);
}
