//package com.example.miniproj.service;
//
//import com.example.miniproj.exception.AudioProcessingException;
//import com.example.miniproj.model.ProcessingStatus;
//import org.apache.commons.exec.*;
//import org.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.io.*;
//import java.nio.file.*;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.*;
//
//@Service
//public class AudioProcessingService {
//
//    private static final Logger log = LoggerFactory.getLogger(AudioProcessingService.class);
//    private static final String TEMP_DIR = "temp_audio";
//    private static final String STATUS_PENDING = "pending";
//    private static final String STATUS_DOWNLOADING = "downloading";
//    private static final String STATUS_PROCESSING = "processing";
//    private static final String STATUS_COMPLETED = "completed";
//    private static final String STATUS_ERROR = "error";
//
//    private final Map<String, ProcessingStatus> processingMap = new ConcurrentHashMap<>();
//    private final ExecutorService executor = Executors.newFixedThreadPool(5);
//
//    public AudioProcessingService() {
//        try {
//            Files.createDirectories(Paths.get(TEMP_DIR));
//        } catch (IOException e) {
//            throw new AudioProcessingException("임시 디렉토리 생성 실패", e);
//        }
//    }
//
//    public String startProcessing(String youtubeUrl) {
//        String taskId = UUID.randomUUID().toString();
//        ProcessingStatus status = new ProcessingStatus(taskId);
//        processingMap.put(taskId, status);
//
//        CompletableFuture.runAsync(() -> process(youtubeUrl, taskId), executor);
//        log.info("처리 시작됨: {}", taskId);
//        return taskId;
//    }
//
//    public ProcessingStatus getStatus(String taskId) {
//        return processingMap.get(taskId);
//    }
//
//    // 보컬 관련 경로, 로직 제거 - MR와 자막만 처리
//    public File getFile(String taskId, String type) {
//        ProcessingStatus status = processingMap.get(taskId);
//        if (status == null || !STATUS_COMPLETED.equals(status.getStatus())) return null;
//
//        String path = null;
//        if ("mr".equals(type)) {
//            path = status.getMrPath();
//        } else {
//            return null;
//        }
//
//        return (path != null && new File(path).exists()) ? new File(path) : null;
//    }
//
//    public File getSubtitleFile(String taskId) {
//        ProcessingStatus status = processingMap.get(taskId);
//        if (status == null || !STATUS_COMPLETED.equals(status.getStatus())) return null;
//
//        String path = status.getSubtitlePath();
//        return (path != null && new File(path).exists()) ? new File(path) : null;
//    }
//
//    public void cleanup(String taskId) {
//        ProcessingStatus status = processingMap.get(taskId);
//        if (status != null) {
//            log.info("정리 시작: {}", taskId);
//            tryDelete(status.getMrPath());
//            tryDelete(status.getSubtitlePath());
//            tryDelete(TEMP_DIR + "/" + taskId + ".wav");
//            try {
//                Files.deleteIfExists(Paths.get(TEMP_DIR + "/" + taskId + "_separated"));
//                Files.deleteIfExists(Paths.get(TEMP_DIR + "/" + taskId + "_subtitles"));
//            } catch (Exception e) {
//                log.warn("임시 디렉토리 삭제 실패: {}", e.getMessage());
//            }
//            processingMap.remove(taskId);
//            log.info("정리 완료: {}", taskId);
//        }
//    }
//
//    private void process(String youtubeUrl, String taskId) {
//        ProcessingStatus status = processingMap.get(taskId);
//        try {
//            status.setStatus(STATUS_DOWNLOADING);
//            status.setProgress(10);
//            log.info("다운로드 시작: {}", youtubeUrl);
//
//            JSONObject videoInfo = getVideoInfo(youtubeUrl);
//            if (videoInfo != null) {
//                status.setVideoTitle(videoInfo.optString("title", "Unknown"));
//            }
//
//            String audioPath = downloadAudio(youtubeUrl, taskId);
//            if (audioPath == null) {
//                throw new AudioProcessingException("오디오 다운로드 실패");
//            }
//
//            // 자막 다운로드 추가
//            String subtitlePath = downloadSubtitles(youtubeUrl, taskId);
//            status.setSubtitlePath(subtitlePath);
//
//            status.setProgress(50);
//            status.setStatus(STATUS_PROCESSING);
//
//            // 보컬 분리 제거, MR 분리만 진행
//            separateMrAudio(audioPath, taskId);
//
//            status.setStatus(STATUS_COMPLETED);
//            status.setProgress(100);
//            log.info("처리 완료: {}", taskId);
//        } catch (Exception e) {
//            log.error("오류 발생: {}", e.getMessage(), e);
//            status.setStatus(STATUS_ERROR);
//            status.setError("처리 중 오류: " + e.getMessage());
//        }
//    }
//
//    private JSONObject getVideoInfo(String url) throws Exception {
//        String command = String.format("yt-dlp -j --no-download \"%s\"", url);
//        return executeAndParseJson(command);
//    }
//
//    private String downloadAudio(String url, String taskId) throws Exception {
//        String out = TEMP_DIR + "/" + taskId + ".%(ext)s";
//        String cmd = String.format("yt-dlp -x --audio-format wav -o \"%s\" \"%s\"", out, url);
//        int exit = execute(cmd);
//        return exit == 0 ? TEMP_DIR + "/" + taskId + ".wav" : null;
//    }
//
//    private String downloadSubtitles(String url, String taskId) {
//        try {
//            String subtitleDir = TEMP_DIR + "/" + taskId + "_subtitles";
//            Files.createDirectories(Paths.get(subtitleDir));
//
//            String autoSubCmd = String.format(
//                    "yt-dlp --skip-download --write-auto-sub --sub-lang en --sub-format vtt --output \"%s/%s.%%(ext)s\" \"%s\"",
//                    subtitleDir, taskId, url);
//            int exitCode = execute(autoSubCmd);
//
//            if (exitCode != 0) {
//                String subCmd = String.format(
//                        "yt-dlp --skip-download --write-sub --sub-lang en --sub-format vtt --output \"%s/%s.%%(ext)s\" \"%s\"",
//                        subtitleDir, taskId, url);
//                exitCode = execute(subCmd);
//                if (exitCode != 0) {
//                    log.warn("자막 다운로드 실패 또는 자막 없음: {}", url);
//                    return null;
//                }
//            }
//
//            String subtitlePath = subtitleDir + "/" + taskId + ".en.vtt";
//            if (Files.exists(Paths.get(subtitlePath))) {
//                return subtitlePath;
//            } else {
//                log.warn("자막 파일을 찾을 수 없습니다: {}", subtitlePath);
//                return null;
//            }
//        } catch (Exception e) {
//            log.error("자막 다운로드 중 오류 발생: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    // 보컬 분리 제거, MR만 분리하는 메서드
//    private void separateMrAudio(String audioPath, String taskId) throws Exception {
//        String outputDir = TEMP_DIR + "/" + taskId + "_separated";
//        Files.createDirectories(Paths.get(outputDir));
//
//        String mr = outputDir + "/mr.wav";
//
//        String mCmd = String.format("ffmpeg -i \"%s\" -af \"pan=mono|c0=0.5*c0+0.5*c1\" \"%s\"", audioPath, mr);
//
//        execute(mCmd);
//
//        ProcessingStatus status = processingMap.get(taskId);
//        status.setMrPath(mr);
//    }
//
//    private int execute(String cmd) throws Exception {
//        CommandLine cl = CommandLine.parse(cmd);
//        DefaultExecutor executor = new DefaultExecutor();
//        return executor.execute(cl);
//    }
//
//    private JSONObject executeAndParseJson(String cmd) throws Exception {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        DefaultExecutor executor = new DefaultExecutor();
//        executor.setStreamHandler(new PumpStreamHandler(out));
//        int exit = executor.execute(CommandLine.parse(cmd));
//        return exit == 0 ? new JSONObject(out.toString()) : null;
//    }
//
//    private void tryDelete(String path) {
//        if (path != null) {
//            try {
//                Files.deleteIfExists(Paths.get(path));
//            } catch (Exception e) {
//                log.warn("파일 삭제 실패: {}", path);
//            }
//        }
//    }
//}
