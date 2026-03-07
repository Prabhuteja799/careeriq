package com.careeriq.controller;

import com.careeriq.model.dto.CandidateProfileDto;
import com.careeriq.pipeline.resume.ResumeUploadService;
import com.careeriq.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeUploadService resumeUploadService;
    private final CandidateService candidateService;

    /**
     * Upload a resume PDF.
     * Returns immediately with a jobId — AI parsing runs async.
     * Frontend polls /api/resume/status/{jobId} to check completion.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String queueJobId = resumeUploadService.queueUpload(file, userDetails.getUsername());

        return ResponseEntity.accepted().body(Map.of(
            "jobId",   queueJobId,
            "status",  "PROCESSING",
            "message", "Resume uploaded. Parsing in progress — check status endpoint."
        ));
    }

    /**
     * Poll endpoint — frontend calls this every 2s after upload.
     * Returns PROCESSING | DONE | FAILED
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String jobId) {
        return ResponseEntity.ok(resumeUploadService.getJobStatus(jobId));
    }

    /**
     * Get the current user's candidate profile (post-parsing).
     */
    @GetMapping("/profile")
    public ResponseEntity<CandidateProfileDto> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        CandidateProfileDto profile = candidateService.getProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * Delete resume + profile + all match scores for this candidate.
     */
    @DeleteMapping("/profile/{candidateId}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable UUID candidateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        candidateService.deleteProfile(candidateId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
