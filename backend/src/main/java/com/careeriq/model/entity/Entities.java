package com.careeriq.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// USER
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String firstName;
    private String lastName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant lastLoginAt;
}

// ─────────────────────────────────────────────────────────────────────────────
// CANDIDATE  (one row per resume version)
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "candidates", indexes = {
    @Index(name = "idx_candidate_user",         columnList = "user_id"),
    @Index(name = "idx_candidate_last_matched", columnList = "last_matched_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String resumeHash;          // SHA-256 — prevents reprocessing identical resumes

    private String s3Key;               // Where the original PDF lives in S3

    // ── AI-extracted profile ──────────────────────────────────────────────
    private String detectedRole;        // "Software Engineer", "Data Analyst"
    private String primarySpec;         // "Backend Engineering", "ML Engineering"

    @Enumerated(EnumType.STRING)
    private SeniorityLevel seniority;

    private Integer yearsExp;
    private String domain;              // fintech, healthtech, startup, etc.

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> coreStack;     // ["Java", "Spring Boot", "Kafka"]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> supportingSkills; // ["AWS", "Docker", "PostgreSQL"]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> possibleTitles;   // 5 titles this candidate qualifies for

    // ── Sync cursor ───────────────────────────────────────────────────────
    private Instant lastMatchedAt;      // Delta sync reads this to find new jobs

    @Column(nullable = false)
    private Integer version = 1;        // Incremented on each resume re-upload

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}

// ─────────────────────────────────────────────────────────────────────────────
// JOB
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_job_created",        columnList = "created_at DESC"),
    @Index(name = "idx_job_status_created", columnList = "status, created_at"),
    @Index(name = "idx_job_company",        columnList = "company")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;
    private Boolean remote;
    private String salaryMin;
    private String salaryMax;

    @Column(columnDefinition = "TEXT")
    private String rawJd;               // Original JD text — always preserved

    private String source;              // linkedin, indeed, adzuna, manual

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant expiresAt;
    private Instant parsedAt;           // When AI parsed this job
}

// ─────────────────────────────────────────────────────────────────────────────
// JOB PROFILE  (AI-extracted intelligence)
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "job_profiles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class JobProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID jobId;

    private String role;                // Normalized role name
    private String domain;

    @Enumerated(EnumType.STRING)
    private SeniorityLevel seniority;

    private Integer expYearsMin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> mustHave;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> niceToHave;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> strongSignals;  // Skills mentioned 2+ times

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> techStack;

    private String teamSignals;          // Remote/hybrid, team size hints
}

// ─────────────────────────────────────────────────────────────────────────────
// ATS BLUEPRINT  (ATS optimization layer per job)
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "ats_blueprints")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class AtsBlueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> exactKeywords;   // Exact spellings ATS scanner looks for

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object keywordWeights;        // {"Spring Boot": 10, "Kafka": 9}

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> powerVerbs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> repeatedPhrases;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> requiredCerts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> senioritySignals;
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPANY INTEL  (researched once, reused forever)
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "company_intel")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CompanyIntel {

    @Id
    private String companyName;         // Normalized name — PK

    private String industry;
    private String engineeringCulture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> knownFor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> valuesInEngineers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> techStackKnown;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> buzzwords;

    @Column(columnDefinition = "TEXT")
    private String whatImpresses;

    @Column(columnDefinition = "TEXT")
    private String whatToAvoid;

    private Instant lastResearched = Instant.now();
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH SCORES  (heart of the platform — pre-computed)
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "match_scores", indexes = {
    @Index(name = "idx_match_candidate_score",  columnList = "candidate_id, score DESC"),
    @Index(name = "idx_match_candidate_scored", columnList = "candidate_id, scored_at DESC")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@IdClass(MatchScoreId.class)
class MatchScore {

    @Id
    private UUID candidateId;

    @Id
    private UUID jobId;

    @Column(nullable = false)
    private Double score;               // Final weighted score 0–100

    private Double skillMatch;          // 0–100
    private String expMatch;            // strong | good | partial | weak
    private String roleMatch;           // strong | good | partial | weak
    private String seniorityMatch;      // exact | close | stretch | mismatch

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> matchedSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> missingSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> reasons;       // Human-readable match explanations

    @Column(nullable = false)
    private Instant scoredAt = Instant.now();

    @Column(nullable = false)
    private Integer scoreVersion = 1;
}

// ─────────────────────────────────────────────────────────────────────────────
// ENUMS
// ─────────────────────────────────────────────────────────────────────────────
enum SeniorityLevel { JUNIOR, MID, SENIOR, STAFF, PRINCIPAL }
enum JobStatus      { ACTIVE, EXPIRED, FILLED, DRAFT }
