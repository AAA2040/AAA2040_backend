package com.example.miniproj.service;

import com.example.miniproj.model.ProcessingStatus;
import com.example.miniproj.model.CallbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioProcessingService {

    private final RestTemplate restTemplate;

    @Value("${external.service.url}")
    private String externalServiceUrl;

    public ProcessingStatus startProcessing(String youtubeUrl) {
        log.info("[요청 시작] 외부 음원 분리: {}", youtubeUrl);
        // 유효성 검사
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 URL입니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("url", youtubeUrl), headers);

        CallbackResponse callback = restTemplate.postForEntity(
                externalServiceUrl,
                request,
                CallbackResponse.class
        ).getBody();

        if (callback == null || !"success".equalsIgnoreCase(callback.getResult())) {
            log.error("외부 서비스 처리 실패: {}", callback);
            throw new IllegalStateException("외부 서비스 요청 실패");
        }

        // DTO 매핑
        ProcessingStatus status = new ProcessingStatus();
        status.setTaskId(callback.getUriId());
        status.setMrPath(callback.getNoVocalsUrl());
        status.setVocalsPath(callback.getVocalsUrl());
        status.setLyrics(callback.getLyrics() == null ? "" : callback.getLyrics());
        log.info("[요청 완료] taskId={}", status.getTaskId());

        return status;
    }
}