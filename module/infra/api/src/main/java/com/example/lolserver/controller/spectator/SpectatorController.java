package com.example.lolserver.controller.spectator;

import com.example.lolserver.controller.spectator.response.QueueInfoResponse;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.queue_type.QueueTypeService;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SpectatorController {

    private final QueueTypeService queueTypeService;

    @GetMapping("/api/v1/queue-tab")
    public ResponseEntity<ApiResponse<List<QueueInfoResponse>>> findAllQueueInfoForTab() {
        List<QueueInfo> queueInfos = queueTypeService.findAllByIsTabTrue();

        return ResponseEntity.ok(
                ApiResponse.success(queueInfos.stream().map(QueueInfoResponse::of).toList())
        );
    }

}
