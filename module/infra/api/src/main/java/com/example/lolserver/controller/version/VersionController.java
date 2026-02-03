package com.example.lolserver.controller.version;

import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.version.application.VersionService;
import com.example.lolserver.domain.version.application.model.VersionReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<VersionReadModel>> getLatestVersion() {
        VersionReadModel latestVersion = versionService.getLatestVersion();
        return ResponseEntity.ok(ApiResponse.success(latestVersion));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VersionReadModel>>> getAllVersions() {
        List<VersionReadModel> versions = versionService.getAllVersions();
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<ApiResponse<VersionReadModel>> getVersionById(
            @PathVariable("versionId") Long versionId
    ) {
        VersionReadModel version = versionService.getVersionById(versionId);
        return ResponseEntity.ok(ApiResponse.success(version));
    }
}
