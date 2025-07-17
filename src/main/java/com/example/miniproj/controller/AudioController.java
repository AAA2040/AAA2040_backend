package com.example.miniproj.controller;

import com.example.miniproj.model.ProcessRequest;
import com.example.miniproj.model.ProcessingStatus;
import com.example.miniproj.service.HybridProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AudioController {

    private final HybridProcessingService hybridService;

    // POST /api/process
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody ProcessRequest request) {
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("result", "error", "message", "유효하지 않은 URL입니다."));
        }

        String taskId = hybridService.startProcessing(request.getUrl());

        // 실제 처리 완료 시점에 생성될 파일 이름 기준 URL 생성 (프론트 스펙에 맞춰)
        String noVocalsUrl = "/data/audio/" + taskId + "_no_vocals.mp3";
        String vocalsUrl = "/data/audio/" + taskId + "_vocals.mp3";

        // 가사 필드는 실제 작업 후 별도 API에서 가져오므로 초기엔 빈 문자열로 두거나 임시 메시지
        return ResponseEntity.ok(Map.of(
                "result", "success",
                "uriId", taskId,
                "no_vocals_url", noVocalsUrl,
                "vocals_url", vocalsUrl,
                "lyrics", ""
        ));
    }

    // GET /api/status/{taskId}
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ProcessingStatus> getStatus(@PathVariable String taskId) {
        ProcessingStatus status = hybridService.getStatus(taskId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    // GET /api/result/{taskId}/lyrics
    @GetMapping("/result/{taskId}/lyrics")
    public ResponseEntity<String> getLyrics(@PathVariable String taskId) {
        try {
            Path filePath = Paths.get("data", "subtitles", taskId + ".vtt");
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String content = Files.readString(filePath);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("가사 읽기 실패: " + e.getMessage());
        }
    }

    // GET /api/download/{taskId}/subtitle
    @GetMapping("/download/{taskId}/subtitle")
    public ResponseEntity<UrlResource> downloadSubtitle(@PathVariable String taskId) {
        try {
            Path filePath = Paths.get("data", "subtitles", taskId + ".vtt");
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            UrlResource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/vtt"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
