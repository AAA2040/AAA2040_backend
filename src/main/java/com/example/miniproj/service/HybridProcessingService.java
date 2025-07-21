package com.example.miniproj.service;

import com.example.miniproj.model.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridProcessingService {

    private final ExternalAudioProcessingService externalService;

    public ProcessingStatus startProcessing(String youtubeUrl) {
        return externalService.startProcessingAndReturn(youtubeUrl);
    }
}
