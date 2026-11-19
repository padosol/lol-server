package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.admin.request.CreatePersonRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.LinkSummonerRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.UpdatePersonRequest;
import com.example.lolserver.esports.adapter.in.web.admin.response.PersonResponse;
import com.example.lolserver.esports.adapter.in.web.admin.response.SummonerLinkResponse;
import com.example.lolserver.esports.application.port.in.PersonManagementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 — 인물과 소환사 연결. puuid 는 화면이 lol-repository({@code /api/v1/summoners/by-riot-id/...})에서
 * 얻어 넘긴다. {@code /api/v1/admin/**} 는 ADMIN 만 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/esports/persons")
@RequiredArgsConstructor
public class AdminPersonController {

    private final PersonManagementUseCase personManagementUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<PersonResponse>> createPerson(@Valid @RequestBody CreatePersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                PersonResponse.from(personManagementUseCase.createPerson(request.toCommand()))));
    }

    @PutMapping("/{personId}")
    public ResponseEntity<ApiResponse<PersonResponse>> updatePerson(
            @PathVariable String personId, @Valid @RequestBody UpdatePersonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                PersonResponse.from(personManagementUseCase.updatePerson(personId, request.toCommand()))));
    }

    @DeleteMapping("/{personId}")
    public ResponseEntity<Void> deletePerson(@PathVariable String personId) {
        personManagementUseCase.deletePerson(personId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{personId}/summoners")
    public ResponseEntity<ApiResponse<List<SummonerLinkResponse>>> getSummoners(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(personManagementUseCase.getSummoners(personId).stream()
                .map(SummonerLinkResponse::from)
                .toList()));
    }

    @PutMapping("/{personId}/summoners/{puuid}")
    public ResponseEntity<ApiResponse<SummonerLinkResponse>> linkSummoner(
            @PathVariable String personId, @PathVariable String puuid,
            @Valid @RequestBody LinkSummonerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(SummonerLinkResponse.from(
                personManagementUseCase.linkSummoner(personId, puuid, request.toCommand()))));
    }

    @DeleteMapping("/{personId}/summoners/{puuid}")
    public ResponseEntity<Void> unlinkSummoner(@PathVariable String personId, @PathVariable String puuid) {
        personManagementUseCase.unlinkSummoner(personId, puuid);
        return ResponseEntity.noContent().build();
    }
}
