package com.careeriq.pipeline.matching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Delta Sync — runs every hour.
 * Finds all candidates whose last_matched_at is stale,
 * scores them against new jobs using cosine similarity only.
 * Zero AI calls. Pure math + DB.
 *
 * This is what keeps every user's dashboard fresh without
 * running AI per request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeltaSyncScheduler {

    @Value("${careeriq.delta-sync.batch-size:100}")
    private int batchSize;

    // Repositories injected here in real impl

    /**
     * Fires every hour.
     * fixedDelayString reads from application.yml.
     */
    @Scheduled(fixedDelayString = "${careeriq.delta-sync.interval-ms:3600000}")
    public void runDeltaSync() {
        Instant syncStart = Instant.now();
        log.info("[DeltaSync] Starting delta sync at {}", syncStart);

        try {
            int totalProcessed = processBatches();
            long elapsed = Instant.now().toEpochMilli() - syncStart.toEpochMilli();
            log.info("[DeltaSync] Complete — {} candidates updated in {}ms", totalProcessed, elapsed);

        } catch (Exception e) {
            log.error("[DeltaSync] Delta sync failed: {}", e.getMessage(), e);
        }
    }

    private int processBatches() {
        int totalProcessed = 0;
        int page = 0;

        while (true) {
            // 1. Find candidates whose last_matched_at is older than 1 hour
            //    SELECT * FROM candidates WHERE last_matched_at < NOW() - INTERVAL '1 hour'
            //    ORDER BY last_matched_at ASC LIMIT batchSize OFFSET page*batchSize
            //    List<Candidate> batch = candidateRepo.findStale(page, batchSize);

            // Simulating batch logic — replace with real repo calls
            int batchCount = processBatch(page);
            totalProcessed += batchCount;

            if (batchCount < batchSize) break; // Last batch — done
            page++;
        }

        return totalProcessed;
    }

    private int processBatch(int page) {
        // For each candidate in batch:
        // 1. Find new jobs: SELECT * FROM jobs WHERE created_at > candidate.last_matched_at
        //                   AND status = 'ACTIVE'
        // 2. For each new job: score = cosineSimilarity(candidate_embedding, job_embedding)
        //                      weighted_score = computeWeightedScore(candidate, job, score)
        // 3. Bulk INSERT into match_scores
        // 4. UPDATE candidates SET last_matched_at = NOW() WHERE id = candidateId
        // 5. Invalidate Redis cache: del matches:{candidateId}
        log.debug("[DeltaSync] Processing batch page={}", page);
        return 0; // Return actual count in real impl
    }

    /**
     * Weighted scoring formula from the architecture.
     * skill×0.40 + role×0.25 + exp×0.20 + seniority×0.15 + recency_bonus
     * Pure math — zero AI.
     */
    public double computeWeightedScore(
            double skillMatch,      // 0–100
            double roleMatch,       // 0–100 (cosine sim × 100)
            double expMatch,        // 0–100
            double seniorityMatch,  // 0–100
            boolean isRecent        // posted < 24h
    ) {
        double base = (skillMatch    * 0.40)
                    + (roleMatch     * 0.25)
                    + (expMatch      * 0.20)
                    + (seniorityMatch * 0.15);

        double recencyBonus = isRecent ? 5.0 : 0.0;

        return Math.min(100.0, base + recencyBonus);
    }

    /**
     * Convert years-experience comparison to a 0-100 score.
     */
    public double scoreExperience(int candidateYears, int jobMinYears) {
        int diff = candidateYears - jobMinYears;
        if (diff >= 0  && diff <= 1) return 100.0; // Exact or slightly above
        if (diff == 2)               return 85.0;
        if (diff == -1)              return 75.0;   // 1 year under — often fine
        if (diff == -2)              return 55.0;
        if (diff > 2)                return 80.0;   // Overqualified
        return 30.0;                                // Significantly under
    }

    /**
     * Convert seniority comparison to a 0-100 score.
     */
    public double scoreSeniority(String candidateSeniority, String jobSeniority) {
        if (candidateSeniority == null || jobSeniority == null) return 50.0;
        if (candidateSeniority.equals(jobSeniority))           return 100.0;

        int candidateLevel = seniorityLevel(candidateSeniority);
        int jobLevel       = seniorityLevel(jobSeniority);
        int diff           = Math.abs(candidateLevel - jobLevel);

        return switch (diff) {
            case 1  -> 70.0;
            case 2  -> 40.0;
            default -> 10.0;
        };
    }

    private int seniorityLevel(String seniority) {
        return switch (seniority.toUpperCase()) {
            case "JUNIOR"    -> 1;
            case "MID"       -> 2;
            case "SENIOR"    -> 3;
            case "STAFF"     -> 4;
            case "PRINCIPAL" -> 5;
            default          -> 2;
        };
    }
}
