package com.careeriq.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─── AUTH ─────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDto user;
}

// ─── USER ─────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean hasResume;
}

// ─── CANDIDATE ────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CandidateProfileDto {
    private UUID id;
    private String detectedRole;
    private String primarySpec;
    private String seniority;
    private Integer yearsExp;
    private String domain;
    private List<String> coreStack;
    private List<String> supportingSkills;
    private List<String> possibleTitles;
    private Instant createdAt;
}

// ─── JOB ─────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobDto {
    private UUID id;
    private String title;
    private String company;
    private String location;
    private Boolean remote;
    private String salaryMin;
    private String salaryMax;
    private String status;
    private Instant createdAt;
    // Profile fields joined
    private String role;
    private String seniority;
    private Integer expYearsMin;
    private List<String> mustHave;
    private List<String> niceToHave;
    private List<String> techStack;
    private String domain;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class JobIngestRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String company;
    private String location;
    private Boolean remote;
    @NotBlank
    private String rawJd;
    private String source;
    private String salaryMin;
    private String salaryMax;
}

// ─── FILTERS ──────────────────────────────────────────────────────────────────

/**
 * V1 structured search filters for /api/jobs/search
 * All fields are optional — null = no filter applied
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class JobFilterRequest {
    private String title;           // Partial match on job title
    private String skillsIn;        // Comma-separated: "Java,Spring Boot"
    private String skillsEx;        // Comma-separated: "Android,.NET"
    private Integer expMin;         // Min years experience
    private Integer expMax;         // Max years experience
    private String seniority;       // JUNIOR | MID | SENIOR | STAFF
    private String location;        // Partial match
    private Boolean remote;         // true = remote only
    private String domain;          // fintech | healthtech | etc
    private Integer postedWithin;   // Days: 1 | 3 | 7 | 14 | 30
}

/**
 * Filters for /api/matches — filters the pre-scored match pool
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class MatchFilterRequest {
    private Integer minScore;       // e.g. 70 = only show matches above 70
    private String skillsIn;        // Only jobs that have these skills
    private String skillsEx;        // Exclude jobs with these skills
    private Boolean remote;
    private String seniority;
    private String domain;
    private Integer postedWithin;   // Days
}

// ─── MATCH ────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchDto {
    private UUID jobId;
    private String jobTitle;
    private String company;
    private String location;
    private Boolean remote;
    private String salaryMin;
    private String salaryMax;
    private Instant postedAt;

    // Match intelligence
    private Double score;
    private Double skillMatch;
    private String expMatch;
    private String roleMatch;
    private String seniorityMatch;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> reasons;

    // ATS
    private boolean hasAtsResume;   // Whether user already generated ATS resume for this job
}

// ─── ATS ──────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AtsGapReportDto {
    private UUID jobId;
    private String company;
    private String jobTitle;

    private Integer atsScoreBefore;
    private Integer atsScoreAfter;   // Projected score after rewrite

    private List<String> missingKeywords;
    private List<String> presentKeywords;
    private List<String> cultureGaps;
    private List<String> top3Issues;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AtsResumeDto {
    private UUID id;
    private UUID jobId;
    private String company;

    private String summary;
    private List<ExperienceSection> experience;
    private Object skills;
    private List<String> keywordsInjected;
    private Integer atsScore;
    private Integer companyAlignmentScore;
    private String whyThisWorks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceSection {
        private String title;
        private String company;
        private String period;
        private List<String> bullets;
    }
}
