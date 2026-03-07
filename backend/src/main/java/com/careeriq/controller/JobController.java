package com.careeriq.controller;

import com.careeriq.model.dto.JobDto;
import com.careeriq.model.dto.JobFilterRequest;
import com.careeriq.model.dto.JobIngestRequest;
import com.careeriq.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * V1 SEARCH — Pure structured filters, zero AI.
     * All filters are optional — omit any to skip that filter.
     *
     * Query params:
     *   title        — partial match on job title
     *   skillsIn     — comma-separated required skills e.g. "Java,Spring Boot"
     *   skillsEx     — comma-separated excluded skills e.g. "Android,.NET"
     *   expMin       — minimum years experience (integer)
     *   expMax       — maximum years experience (integer)
     *   seniority    — JUNIOR | MID | SENIOR | STAFF
     *   location     — text match on location field
     *   remote       — true | false
     *   domain       — fintech | healthtech | startup | enterprise
     *   postedWithin — 1 | 3 | 7 | 14 | 30 (days)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<JobDto>> search(
            @Valid JobFilterRequest filter,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobDto> results = jobService.search(filter, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get single job detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    /**
     * Ingest a new job — admin/internal use in V1.
     * Queues AI parse pipeline async.
     */
    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> ingest(
            @Valid @RequestBody JobIngestRequest request) {
        String jobId = jobService.ingest(request);
        return ResponseEntity.accepted().body(Map.of(
            "jobId",   jobId,
            "status",  "QUEUED",
            "message", "Job queued for AI parsing"
        ));
    }
}
