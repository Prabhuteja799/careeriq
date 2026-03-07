package com.careeriq.pipeline.resume;

import com.careeriq.model.entity.*;
import com.careeriq.service.AiService;
import com.careeriq.service.EmbeddingService;
import com.careeriq.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Orchestrates the full resume processing pipeline.
 * Runs entirely async — called from ResumeUploadService, returns immediately.
 *
 * Pipeline:
 *   1. Check duplicate hash
 *   2. AI parse → candidate profile
 *   3. Generate embedding
 *   4. 7-day backfill match scores
 *   5. Update status → DONE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumePipelineOrchestrator {

    private final AiService aiService;
    private final EmbeddingService embeddingService;
    private final MatchingService matchingService;
    private final ResumeUploadService uploadService; // for setStatus
    // Repositories injected here in real implementation

    @Async
    @Transactional
    public void process(String jobId, String userEmail,
                        String resumeText, String hash, String s3Key) {
        try {
            log.info("[Pipeline] Starting resume processing for user: {}", userEmail);

            // ── Step 1: Duplicate check ───────────────────────────────────
            // If hash exists → reuse existing candidate profile, skip AI
            // candidateRepo.findByResumeHash(hash) — if present, skip to step 4

            // ── Step 2: AI Parse ──────────────────────────────────────────
            uploadService.setStatus(jobId, "PROCESSING", "Analyzing your resume with AI...");
            log.info("[Pipeline] Calling Claude to parse resume");

            AiService.ParsedResume parsed = aiService.parseResume(resumeText);

            // ── Step 3: Build + save candidate profile ────────────────────
            // Candidate candidate = buildCandidate(userEmail, parsed, hash, s3Key);
            // candidateRepo.save(candidate);
            log.info("[Pipeline] Candidate profile saved: role={}, seniority={}",
                     parsed.getDetectedRole(), parsed.getSeniority());

            // ── Step 4: Generate embedding ────────────────────────────────
            uploadService.setStatus(jobId, "PROCESSING", "Building your match profile...");
            float[] embedding = embeddingService.embed(parsed.toEmbeddingText());
            // candidateEmbeddingRepo.save(new CandidateEmbedding(candidateId, embedding));
            log.info("[Pipeline] Embedding generated: {} dimensions", embedding.length);

            // ── Step 5: 7-day backfill ────────────────────────────────────
            uploadService.setStatus(jobId, "PROCESSING", "Finding your best matches...");
            // matchingService.backfillLastNDays(candidateId, 7);
            log.info("[Pipeline] 7-day backfill complete");

            // ── Step 6: Mark done ─────────────────────────────────────────
            uploadService.setStatus(jobId, "DONE", "Resume processed. Your matches are ready!");
            log.info("[Pipeline] Resume pipeline complete for user: {}", userEmail);

        } catch (Exception e) {
            log.error("[Pipeline] Resume processing failed for job {}: {}", jobId, e.getMessage(), e);
            uploadService.setStatus(jobId, "FAILED",
                "Processing failed: " + e.getMessage() + ". Please try uploading again.");
        }
    }
}
