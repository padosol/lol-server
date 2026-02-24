package com.example.lolserver.controller.spectator;

import com.example.lolserver.controller.spectator.response.QueueInfoResponse;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.queue_type.application.QueueTypeService;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.domain.spectator.application.SpectatorService;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SpectatorController {

    private final QueueTypeService queueTypeService;
    private final SpectatorService spectatorService;

    @GetMapping("/v1/queue-tab")
    public ResponseEntity<ApiResponse<List<QueueInfoResponse>>> findAllQueueInfoForTab() {
        List<QueueInfo> queueInfos = queueTypeService.findAllByIsTabTrue();

        return ResponseEntity.ok(
                ApiResponse.success(queueInfos.stream().map(QueueInfoResponse::of).toList())
        );
    }

    @GetMapping("/v1/{platformId}/spectator/active-games/by-puuid/{puuid}")
    public ResponseEntity<ApiResponse<CurrentGameInfoReadModel>> getCurrentGameInfo(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid
    ) {
        CurrentGameInfoReadModel gameInfo = spectatorService.getCurrentGameInfo(puuid, platformId);
        return ResponseEntity.ok(ApiResponse.success(gameInfo));
    }

}
