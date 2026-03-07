package com.careeriq.controller;

import com.careeriq.model.dto.AtsGapReportDto;
import com.careeriq.model.dto.AtsResumeDto;
import com.careeriq.service.AtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ats")
@RequiredArgsConstructor
public class AtsController {

    private final AtsService atsService;

    /**
     * Step 1 — Gap analysis.
     * Shows current ATS score, missing keywords, culture gaps.
     * Fast — compares stored profiles, minimal AI.
     */
    @GetMapping("/analyze/{jobId}")
    public ResponseEntity<AtsGapReportDto> analyze(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        AtsGapReportDto report = atsService.analyzeGap(userDetails.getUsername(), jobId);
        return ResponseEntity.ok(report);
    }

    /**
     * Step 2 — Full 3-layer intelligent rewrite.
     * Uses JD intelligence + company intelligence + candidate intelligence.
     * Result cached in ats_resumes — same (candidate, job) pair served instantly after.
     */
    @PostMapping("/rewrite/{jobId}")
    public ResponseEntity<AtsResumeDto> rewrite(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        AtsResumeDto resume = atsService.rewrite(userDetails.getUsername(), jobId);
        return ResponseEntity.ok(resume);
    }

    /**
     * Get previously generated ATS resume (from cache).
     */
    @GetMapping("/resume/{jobId}")
    public ResponseEntity<AtsResumeDto> getCached(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        AtsResumeDto resume = atsService.getCachedResume(userDetails.getUsername(), jobId);
        return ResponseEntity.ok(resume);
    }
}
