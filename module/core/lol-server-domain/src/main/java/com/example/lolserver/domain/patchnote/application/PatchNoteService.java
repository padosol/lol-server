package com.example.lolserver.domain.patchnote.application;

import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;
import com.example.lolserver.domain.patchnote.application.port.out.PatchNotePersistencePort;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatchNoteService {

    private final PatchNotePersistencePort patchNotePersistencePort;

    public List<PatchNoteSummaryReadModel> getAllPatchNotes() {
        return patchNotePersistencePort.findAllSummary();
    }

    public PatchNoteReadModel getPatchNoteByVersionId(String versionId) {
        return patchNotePersistencePort.findByVersionId(versionId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PATCH_NOTE,
                        "존재하지 않는 패치노트입니다. versionId: " + versionId
                ));
    }
}
