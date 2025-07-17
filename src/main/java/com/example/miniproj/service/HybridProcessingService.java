package com.example.miniproj.service;

import com.example.miniproj.model.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridProcessingService {

    private final ExternalAudioProcessingService externalService;

    public String startProcessing(String youtubeUrl) {
        return externalService.startProcessing(youtubeUrl);
    }

    public ProcessingStatus getStatus(String taskId) {
        return externalService.getStatus(taskId);
    }
}
