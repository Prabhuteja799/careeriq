package com.careeriq.controller;

import com.careeriq.model.dto.MatchDto;
import com.careeriq.model.dto.MatchFilterRequest;
import com.careeriq.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /**
     * Get pre-scored matches for the current user.
     * Pure DB read — zero AI. Response < 100ms.
     * Supports V1 structured filters via query params.
     */
    @GetMapping
    public ResponseEntity<Page<MatchDto>> getMatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid MatchFilterRequest filter,
            @PageableDefault(size = 20, sort = "score") Pageable pageable) {

        Page<MatchDto> matches = matchService.getMatches(
            userDetails.getUsername(), filter, pageable);

        return ResponseEntity.ok(matches);
    }

    /**
     * Count of new matches since last visit — drives notification badge.
     * Cheap SELECT COUNT query.
     */
    @GetMapping("/delta")
    public ResponseEntity<Map<String, Long>> getDelta(
            @AuthenticationPrincipal UserDetails userDetails) {
        long newCount = matchService.countNewMatchesSinceLastVisit(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("newMatches", newCount));
    }

    /**
     * Get single match detail with full reasons + skill breakdown.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<MatchDto> getMatchDetail(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        MatchDto match = matchService.getMatchDetail(userDetails.getUsername(), jobId);
        return ResponseEntity.ok(match);
    }
}
