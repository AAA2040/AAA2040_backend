package com.example.miniproj.service;

import com.example.miniproj.ProcessingStatusEnum;
import com.example.miniproj.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalAudioProcessingService {

    private final RestTemplate restTemplate;
    private final Map<String, ProcessingStatus> processingMap = new ConcurrentHashMap<>();

    @Value("${external.service.url}")
    private String externalServiceUrl;

    @Value("${external.callback.base-url}")
    private String callbackBaseUrl;

    public String startProcessing(String youtubeUrl) {
        String taskId = UUID.randomUUID().toString();

        log.info("[시작] Processing 시작: taskId={}, youtubeUrl={}", taskId, youtubeUrl);

        ProcessingStatus status = new ProcessingStatus();
        status.setTaskId(taskId);
        status.setStatus(ProcessingStatusEnum.PENDING);
        status.setProgress(0);
        processingMap.put(taskId, status);

        String callbackUrl = callbackBaseUrl + "/api/callback/" + taskId;

        Map<String, String> body = Map.of(
                "url", youtubeUrl,
                "callbackUrl", callbackUrl
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        log.info("[요청] 외부 서비스 호출: url={}, callbackUrl={}", externalServiceUrl, callbackUrl);

        try {
            restTemplate.postForEntity(externalServiceUrl, request, Void.class);
            log.info("[요청 성공] 외부 서비스 요청 완료: taskId={}", taskId);
        } catch (Exception e) {
            status.setStatus(ProcessingStatusEnum.ERROR);
            status.setError("외부 서비스 요청 실패: " + e.getMessage());
            log.error("[에러] 외부 서비스 요청 실패: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("외부 서비스 요청 실패", e);
        }

        return taskId;
    }

    public ProcessingStatus getStatus(String taskId) {
        ProcessingStatus status = processingMap.get(taskId);
        if (status == null) {
            log.warn("[조회 실패] 존재하지 않는 taskId: {}", taskId);
        } else {
            log.debug("[조회] taskId={} 현재 상태: {}", taskId, status.getStatus());
        }
        return status;
    }

    public void updateStatusFromCallback(String taskId, CallbackResponse response) {
        ProcessingStatus status = processingMap.get(taskId);
        if (status == null) {
            log.warn("[콜백 무시] 등록되지 않은 taskId: {}", taskId);
            return;
        }

        log.info("[콜백 수신] taskId={}, result={}, noVocalsUrl={}, lyricsSize={}",
                taskId, response.getResult(), response.getNoVocalsUrl(),
                response.getLyrics() != null ? response.getLyrics().length() : 0);

        status.setStatus(ProcessingStatusEnum.COMPLETED);
        status.setMrPath(response.getNoVocalsUrl());

        String lyrics = response.getLyrics();
        if (lyrics != null && !lyrics.isBlank()) {
            try {
                String fileName = taskId + ".vtt";
                Path outputDir = Paths.get("data", "subtitles");
                Files.createDirectories(outputDir);
                Path vttPath = outputDir.resolve(fileName);

                log.info("[자막 변환] VTT 변환 시작: taskId={}, 저장 경로={}", taskId, vttPath);

                List<String> vttLines = convertToVtt(lyrics);
                Files.write(vttPath, vttLines, StandardCharsets.UTF_8);

                status.setSubtitlePath("/api/download/" + taskId + "/subtitle");

                log.info("[자막 저장 완료] taskId={}, 라인 수={}", taskId, vttLines.size());
            } catch (IOException e) {
                String errorMsg = "자막 파일 저장 실패: " + e.getMessage();
                status.setError(errorMsg);
                log.error("[에러] " + errorMsg, e);
            }
        } else {
            log.warn("[자막 없음] 콜백에 가사(lyrics)가 포함되지 않음: taskId={}", taskId);
        }

        status.setError(null);
    }

    // ... 생략된 기존 코드 ...

    public CallbackResponse getCallbackStatus(String taskId) {
        ProcessingStatus status = processingMap.get(taskId);
        if (status == null) {
            log.warn("[상태 조회 실패] 존재하지 않는 taskId: {}", taskId);
            return null;
        }

        CallbackResponse response = new CallbackResponse();
        response.setResult(status.getStatus().name());
        response.setNoVocalsUrl(status.getMrPath());
        response.setLyrics(null); // lyrics 파일에서 불러올 수도 있음

        log.info("[상태 반환] taskId={}, result={}, noVocalsUrl={}", taskId, response.getResult(), response.getNoVocalsUrl());
        return response;
    }



    private List<String> convertToVtt(String lyrics) {
        List<String> lines = new ArrayList<>();
        lines.add("WEBVTT\n");

        Pattern pattern = Pattern.compile("\\[(\\d+\\.\\d+) ~ (\\d+\\.\\d+)]\\s*(.+)");
        Matcher matcher = pattern.matcher(lyrics);

        int index = 1;
        while (matcher.find()) {
            double start = Double.parseDouble(matcher.group(1));
            double end = Double.parseDouble(matcher.group(2));
            String text = matcher.group(3).trim();

            lines.add(String.valueOf(index++));
            lines.add(formatTime(start) + " --> " + formatTime(end));
            lines.add(text);
            lines.add("");
        }

        log.debug("[VTT 변환] 총 {}개의 자막 항목 생성됨", index - 1);
        return lines;
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds - (int) seconds) * 1000);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }
}
