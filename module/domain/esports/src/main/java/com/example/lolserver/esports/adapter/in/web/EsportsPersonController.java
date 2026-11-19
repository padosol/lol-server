package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.PageResponse;
import com.example.lolserver.esports.adapter.in.web.response.PersonCareerResponse;
import com.example.lolserver.esports.adapter.in.web.response.PersonProfileResponse;
import com.example.lolserver.esports.adapter.in.web.response.PersonSummaryResponse;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 공개 — 인물 검색·프로필·커리어, 소환사 → 인물. 소환사 정보(Riot ID·티어)는 화면이 lol-repository 에서 받는다.
 * 인증 없음 (SecurityConfig 의 GET /api/v1/esports/**).
 */
@RestController
@RequestMapping("/api/v1/esports")
@RequiredArgsConstructor
public class EsportsPersonController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EsportsQueryUseCase esportsQueryUseCase;

    /** q 가 없으면 계약·사건 이력이 있는 인물만 나온다. 이력 없는 인물은 q 검색에만, status 없이 나온다. */
    @GetMapping("/persons")
    public ResponseEntity<ApiResponse<PageResponse<PersonSummaryResponse>>> searchPersons(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PersonStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate contractEndingBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 검색창이 비어 있을 때 오는 q= 는 "검색어 없음" 이다 — 그대로 두면 '%%' 가 이력 없는 인물까지 목록에 섞는다
        String keyword = StringUtils.hasText(q) ? q.trim() : null;
        PersonSearchCommand command = new PersonSearchCommand(keyword, status, role, teamId, contractEndingBy,
                page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                esportsQueryUseCase.searchPersons(command).map(PersonSummaryResponse::from))));
    }

    @GetMapping("/persons/{personId}")
    public ResponseEntity<ApiResponse<PersonProfileResponse>> getPersonProfile(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(
                PersonProfileResponse.from(esportsQueryUseCase.getPersonProfile(personId))));
    }

    @GetMapping("/persons/{personId}/career")
    public ResponseEntity<ApiResponse<PersonCareerResponse>> getPersonCareer(@PathVariable String personId) {
        return ResponseEntity.ok(ApiResponse.success(
                PersonCareerResponse.from(esportsQueryUseCase.getPersonCareer(personId))));
    }

    /** 전적 검색 화면의 "이 소환사가 누구인지". 공개 연결이 없으면 404. */
    @GetMapping("/summoners/{puuid}/person")
    public ResponseEntity<ApiResponse<PersonSummaryResponse>> getPersonByPuuid(@PathVariable String puuid) {
        return ResponseEntity.ok(ApiResponse.success(
                PersonSummaryResponse.from(esportsQueryUseCase.getPersonByPuuid(puuid))));
    }
}
