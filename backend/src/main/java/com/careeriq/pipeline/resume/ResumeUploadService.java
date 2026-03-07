package com.careeriq.pipeline.resume;

import com.careeriq.service.S3Service;
import com.careeriq.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeUploadService {

    private final S3Service s3Service;
    private final AiService aiService;
    private final ResumePipelineOrchestrator orchestrator;
    private final RedissonClient redisson;

    private static final String STATUS_KEY_PREFIX = "resume:status:";

    /**
     * Step 1 — Called from controller. Returns immediately.
     * Stores file in S3, queues async processing, returns jobId.
     */
    public String queueUpload(MultipartFile file, String userEmail) {
        validateFile(file);

        String jobId = UUID.randomUUID().toString();
        setStatus(jobId, "PROCESSING", null);

        try {
            // Extract text here so we can compute hash synchronously
            String resumeText = extractText(file);
            String hash = sha256(resumeText);

            // Upload original PDF to S3
            String s3Key = s3Service.uploadResume(file, userEmail);

            // Kick off async pipeline — returns immediately
            orchestrator.process(jobId, userEmail, resumeText, hash, s3Key);

        } catch (Exception e) {
            log.error("Resume upload failed for {}: {}", userEmail, e.getMessage());
            setStatus(jobId, "FAILED", e.getMessage());
        }

        return jobId;
    }

    /**
     * Polling endpoint — frontend calls every 2 seconds post-upload.
     */
    public Map<String, Object> getJobStatus(String jobId) {
        RMap<String, String> statusMap = redisson.getMap(STATUS_KEY_PREFIX + jobId);
        String status  = statusMap.getOrDefault("status",  "NOT_FOUND");
        String message = statusMap.getOrDefault("message", "");
        String candidateId = statusMap.getOrDefault("candidateId", null);

        return Map.of(
            "jobId",       jobId,
            "status",      status,
            "message",     message,
            "candidateId", candidateId != null ? candidateId : ""
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String extractText(MultipartFile file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be under 10MB");
        }
    }

    void setStatus(String jobId, String status, String message) {
        RMap<String, String> map = redisson.getMap(STATUS_KEY_PREFIX + jobId);
        map.put("status", status);
        if (message != null) map.put("message", message);
        map.expire(java.time.Duration.ofHours(1));
    }
}
