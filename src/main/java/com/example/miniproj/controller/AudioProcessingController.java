//package com.example.miniproj.controller;
//
//import com.example.miniproj.model.*;
//import com.example.miniproj.service.AudioProcessingService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.io.*;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.File;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//public class AudioProcessingController {
//
//    private final AudioProcessingService service;
//
//    @PostMapping("/process")
//    public ResponseEntity<Map<String, String>> process(@Valid @RequestBody ProcessRequest request) {
//        String taskId = service.startProcessing(request.getYoutubeUrl());
//        return ResponseEntity.ok(Map.of("taskId", taskId, "message", "처리가 시작되었습니다."));
//    }
//
//    @GetMapping("/status/{taskId}")
//    public ResponseEntity<ProcessingStatus> getStatus(@PathVariable String taskId) {
//        ProcessingStatus status = service.getStatus(taskId);
//        return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
//    }
//
//    @GetMapping("/download/{taskId}/{type}")
//    public ResponseEntity<Resource> download(@PathVariable String taskId, @PathVariable String type) {
//        // 보컬(vocal) 제외, MR만 다운로드 가능하도록 제한
//        if (!"mr".equalsIgnoreCase(type)) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        File file = service.getFile(taskId, type.toLowerCase());
//        if (file == null || !file.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        Resource resource = new FileSystemResource(file);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + ".wav\"")
//                .body(resource);
//    }
//
//    @GetMapping("/download/{taskId}/subtitle")
//    public ResponseEntity<Resource> downloadSubtitle(@PathVariable String taskId) {
//        File file = service.getSubtitleFile(taskId);
//        if (file == null || !file.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//        Resource resource = new FileSystemResource(file);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.TEXT_PLAIN)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subtitle.vtt\"")
//                .body(resource);
//    }
//
//    @DeleteMapping("/cleanup/{taskId}")
//    public ResponseEntity<Map<String, String>> cleanup(@PathVariable String taskId) {
//        service.cleanup(taskId);
//        return ResponseEntity.ok(Map.of("message", "처리 결과 및 임시 파일이 삭제되었습니다."));
//    }
//}
