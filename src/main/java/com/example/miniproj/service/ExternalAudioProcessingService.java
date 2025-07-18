package com.example.miniproj.service;

import com.example.miniproj.ProcessingStatusEnum;
import com.example.miniproj.model.CallbackResponse;
import com.example.miniproj.model.ProcessingStatus;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalAudioProcessingService {

    private final RestTemplate restTemplate;

    @Value("${external.service.url}")
    private String externalServiceUrl;

    public ProcessingStatus startProcessingAndReturn(String youtubeUrl) {
        log.info("[요청 시작] 외부 음원 분리: {}", youtubeUrl);

        Map<String, String> body = Map.of("url", youtubeUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<CallbackResponse> response = restTemplate.postForEntity(
                    externalServiceUrl,
                    request,
                    CallbackResponse.class
            );

            CallbackResponse callback = response.getBody();
            if (callback == null || !"success".equalsIgnoreCase(callback.getResult())) {
                throw new RuntimeException("외부 서비스 처리 실패 또는 응답 없음");
            }

            String taskId = callback.getUriId();
            String lyrics = callback.getLyrics();

            if (lyrics != null && !lyrics.isBlank()) {
                saveLyricsToVtt(taskId, lyrics);
            }

            ProcessingStatus result = new ProcessingStatus();
            result.setTaskId(taskId);
            result.setStatus(ProcessingStatusEnum.COMPLETED);
            result.setMrPath(callback.getNoVocalsUrl());
            result.setVocalsPath(callback.getVocalsUrl());
            result.setSubtitlePath("/api/download/" + taskId + "/subtitle");

            // ➕ 가사 필드 세팅
            result.setLyrics(lyrics != null ? lyrics : "");

            return result;

        } catch (Exception e) {
            log.error("[에러] 외부 서비스 요청 실패: {}", e.getMessage(), e);
            throw new RuntimeException("외부 서비스 요청 실패", e);
        }
    }

    private void saveLyricsToVtt(String taskId, String lyrics) {
        try {
            List<String> vttLines = convertToVtt(lyrics);
            Path outputDir = Paths.get("data", "subtitles");
            Files.createDirectories(outputDir);
            Path vttPath = outputDir.resolve(taskId + ".vtt");
            Files.write(vttPath, vttLines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("자막 저장 실패: {}", e.getMessage(), e);
        }
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
