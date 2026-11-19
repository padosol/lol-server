package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.admin.request.CorrectTeamNameRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.CreateTeamRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.DisbandTeamRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.RenameTeamRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.TeamNameHistoryRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.UpdateTeamRequest;
import com.example.lolserver.esports.adapter.in.web.admin.response.TeamNameHistoryResponse;
import com.example.lolserver.esports.adapter.in.web.admin.response.TeamResponse;
import com.example.lolserver.esports.application.port.in.EsportsMasterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 — 팀 등록·수정·개명·이름 정정·해체. {@code /api/v1/admin/**} 는 ADMIN 만 (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/esports/teams")
@RequiredArgsConstructor
public class AdminTeamController {

    private final EsportsMasterUseCase esportsMasterUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                TeamResponse.from(esportsMasterUseCase.createTeam(request.toCommand()))));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable String teamId, @Valid @RequestBody UpdateTeamRequest request) {
        return ok(TeamResponse.from(esportsMasterUseCase.updateTeam(teamId, request.toCommand())));
    }

    @PostMapping("/{teamId}/renames")
    public ResponseEntity<ApiResponse<TeamResponse>> renameTeam(
            @PathVariable String teamId, @Valid @RequestBody RenameTeamRequest request) {
        return ok(TeamResponse.from(esportsMasterUseCase.renameTeam(teamId, request.toCommand())));
    }

    @PatchMapping("/{teamId}/current-name")
    public ResponseEntity<ApiResponse<TeamResponse>> correctCurrentName(
            @PathVariable String teamId, @Valid @RequestBody CorrectTeamNameRequest request) {
        return ok(TeamResponse.from(esportsMasterUseCase.correctCurrentName(teamId, request.toCommand())));
    }

    @PutMapping("/{teamId}/name-history/{historyId}")
    public ResponseEntity<ApiResponse<TeamNameHistoryResponse>> updateNameHistory(
            @PathVariable String teamId, @PathVariable Long historyId,
            @Valid @RequestBody TeamNameHistoryRequest request) {
        return ok(TeamNameHistoryResponse.from(
                esportsMasterUseCase.updateNameHistory(teamId, historyId, request.toCommand())));
    }

    @DeleteMapping("/{teamId}/name-history/{historyId}")
    public ResponseEntity<Void> deleteNameHistory(@PathVariable String teamId, @PathVariable Long historyId) {
        esportsMasterUseCase.deleteNameHistory(teamId, historyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/disbandment")
    public ResponseEntity<ApiResponse<TeamResponse>> disbandTeam(
            @PathVariable String teamId, @Valid @RequestBody DisbandTeamRequest request) {
        return ok(TeamResponse.from(esportsMasterUseCase.disbandTeam(teamId, request.disbandedAt())));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T body) {
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}
