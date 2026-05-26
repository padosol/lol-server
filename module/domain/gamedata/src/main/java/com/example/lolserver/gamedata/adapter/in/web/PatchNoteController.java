package com.example.lolserver.gamedata.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.gamedata.application.port.in.PatchNoteQueryUseCase;
import com.example.lolserver.gamedata.application.model.PatchNoteReadModel;
import com.example.lolserver.gamedata.application.model.PatchNoteSummaryReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PatchNoteController {

    private final PatchNoteQueryUseCase patchNoteService;

    /**
     * 전체 패치노트 목록 조회 API
     * @return 패치노트 요약 목록 (versionId, title)
     */
    @GetMapping("/v1/patch-notes")
    public ResponseEntity<ApiResponse<List<PatchNoteSummaryReadModel>>> getAllPatchNotes() {
        log.info("getAllPatchNotes");
        List<PatchNoteSummaryReadModel> patchNotes = patchNoteService.getAllPatchNotes();
        return ResponseEntity.ok(ApiResponse.success(patchNotes));
    }

    /**
     * 특정 버전 패치노트 상세 조회 API
     * @param versionId 패치노트 버전 ID
     * @return 패치노트 상세 정보 (versionId, title, content)
     */
    @GetMapping("/v1/patch-notes/{versionId}")
    public ResponseEntity<ApiResponse<PatchNoteReadModel>> getPatchNoteByVersionId(
            @PathVariable String versionId
    ) {
        log.info("getPatchNoteByVersionId: {}", versionId);
        PatchNoteReadModel patchNote = patchNoteService.getPatchNoteByVersionId(versionId);
        return ResponseEntity.ok(ApiResponse.success(patchNote));
    }
}
