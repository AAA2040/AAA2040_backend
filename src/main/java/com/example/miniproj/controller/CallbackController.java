package com.example.miniproj.controller;

import com.example.miniproj.model.CallbackResponse;
import com.example.miniproj.service.ExternalAudioProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
@Slf4j
public class CallbackController {

    private final ExternalAudioProcessingService externalService;

    @PostMapping("/{taskId}")
    public ResponseEntity<Void> receiveCallback(@PathVariable String taskId,
                                                @RequestBody CallbackResponse response) {
        log.info("Callback received for taskId = {}, response = {}", taskId, response);
        externalService.updateStatusFromCallback(taskId, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CallbackResponse> getCallbackStatus(@PathVariable String taskId) {
        CallbackResponse response = externalService.getCallbackStatus(taskId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}

