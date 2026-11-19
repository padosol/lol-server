package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.admin.request.CorrectContractRequest;
import com.example.lolserver.esports.adapter.in.web.response.ContractResponse;
import com.example.lolserver.esports.adapter.in.web.response.RosterIssueResponse;
import com.example.lolserver.esports.application.port.in.ContractCorrectionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 — 오입력 정정·삭제와 검증 결과. 정정은 사건 규칙을 거치지 않는다. 어긋나면 roster-issues 가 보여주고,
 * 기간이 겹치면 DB 제약이 막아 409 가 난다. {@code /api/v1/admin/**} 는 ADMIN 만 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/esports")
@RequiredArgsConstructor
public class AdminCorrectionController {

    private final ContractCorrectionUseCase contractCorrectionUseCase;

    @GetMapping("/persons/{personId}/contracts")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getContracts(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(contractCorrectionUseCase.getContracts(personId).stream()
                .map(ContractResponse::from)
                .toList()));
    }

    @PatchMapping("/contracts/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> correctContract(
            @PathVariable Long contractId, @Valid @RequestBody CorrectContractRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ContractResponse.from(contractCorrectionUseCase.correctContract(contractId, request.toCommand()))));
    }

    @DeleteMapping("/contracts/{contractId}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long contractId) {
        contractCorrectionUseCase.deleteContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/career-events/{eventId}")
    public ResponseEntity<Void> deleteCareerEvent(@PathVariable Long eventId) {
        contractCorrectionUseCase.deleteCareerEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    /** v_esports_roster_invalid. 0행이어야 정상이다. */
    @GetMapping("/roster-issues")
    public ResponseEntity<ApiResponse<List<RosterIssueResponse>>> getRosterIssues() {
        return ResponseEntity.ok(ApiResponse.success(contractCorrectionUseCase.getRosterIssues().stream()
                .map(RosterIssueResponse::from)
                .toList()));
    }
}
