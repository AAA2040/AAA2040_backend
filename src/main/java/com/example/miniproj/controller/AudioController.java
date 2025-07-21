package com.example.miniproj.controller;

import com.example.miniproj.model.ProcessRequest;
import com.example.miniproj.model.ProcessingStatus;
import com.example.miniproj.service.AudioProcessingService;
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
public class AudioController{

        private final AudioProcessingService audioService;

        @PostMapping("/process")
        public ResponseEntity<ProcessingStatus> process(@RequestBody ProcessRequest request) {
            ProcessingStatus status = audioService.startProcessing(request.getUrl());
            return ResponseEntity.ok(status);
        }


//    private final HybridProcessingService hybridService;
//
//    @PostMapping("/process")
//    public ResponseEntity<?> process(@RequestBody ProcessRequest request) {
//        if (request.getUrl() == null || request.getUrl().isBlank()) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("result", "error", "message", "유효하지 않은 URL입니다."));
//        }
//
//        try {
//            ProcessingStatus status = hybridService.startProcessing(request.getUrl());
//
//            return ResponseEntity.ok(Map.of(
//                    "result", "success",
//                    "uriId", status.getTaskId(),
//                    "no_vocals_url", status.getMrPath(),
//                    "vocals_url", status.getVocalsPath(),
//                    "lyrics", status.getLyrics()   // ➕ 가사 포함
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(
//                    Map.of("result", "error", "message", e.getMessage()));
//        }
//    }

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
