package com.example.lolserver.gamedata.adapter.in.web;

import com.example.lolserver.gamedata.adapter.in.web.response.QueueInfoResponse;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.gamedata.application.port.in.QueueTypeUseCase;
import com.example.lolserver.gamedata.domain.QueueInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueTypeController {

    private final QueueTypeUseCase queueTypeUseCase;

    /**
     * 큐 목록. 지금은 탭에 노출되는 큐 목록만 제공하므로 {@code tab=true} 를 요구한다.
     * 옛 경로 {@code /v1/queue-tab} 은 lol-ui 전환 후 MP-156 에서 제거.
     */
    @GetMapping(value = "/v1/game-data/queues", params = "tab=true")
    public ResponseEntity<ApiResponse<List<QueueInfoResponse>>> findQueues() {
        return findAllQueueInfoForTab();
    }

    @GetMapping("/v1/queue-tab")
    public ResponseEntity<ApiResponse<List<QueueInfoResponse>>> findAllQueueInfoForTab() {
        List<QueueInfo> queueInfos = queueTypeUseCase.findAllByIsTabTrue();

        return ResponseEntity.ok(
                ApiResponse.success(queueInfos.stream().map(QueueInfoResponse::of).toList())
        );
    }
}
