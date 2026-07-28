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

    @GetMapping("/v1/queue-tab")
    public ResponseEntity<ApiResponse<List<QueueInfoResponse>>> findAllQueueInfoForTab() {
        List<QueueInfo> queueInfos = queueTypeUseCase.findAllByIsTabTrue();

        return ResponseEntity.ok(
                ApiResponse.success(queueInfos.stream().map(QueueInfoResponse::of).toList())
        );
    }
}
