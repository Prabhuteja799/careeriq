package com.careeriq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * All AI API calls go through this service.
 * Every method here represents one AI call type from the architecture.
 * Each is designed to be called ONCE and have its result cached/stored.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    @Value("${ai.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${ai.anthropic.model}")
    private String claudeModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // RESUME PARSE — called once per resume upload
    // ─────────────────────────────────────────────────────────────────────────

    public ParsedResume parseResume(String resumeText) {
        log.info("[AI] Parsing resume — calling Claude");

        String systemPrompt = """
            You are a senior technical recruiter with 15 years of experience.
            Read this resume exactly as a recruiter would.
            Extract a structured profile. Return ONLY valid JSON, no markdown, no preamble.
            """;

        String userPrompt = """
            Parse this resume and return JSON with this exact structure:
            {
              "detected_role": "Primary job title e.g. Software Engineer",
              "primary_spec": "Specialization e.g. Backend Engineering, ML Engineering",
              "seniority": "JUNIOR | MID | SENIOR | STAFF | PRINCIPAL",
              "years_exp": integer,
              "domain": "fintech | healthtech | ecommerce | enterprise | startup | general",
              "core_stack": ["skill1", "skill2"],
              "supporting_skills": ["skill1", "skill2"],
              "possible_titles": ["title1", "title2", "title3", "title4", "title5"],
              "embedding_text": "A dense paragraph summarizing this person's capabilities for semantic search"
            }
            
            RESUME:
            %s
            """.formatted(resumeText);

        String raw = callClaude(systemPrompt, userPrompt, 800);

        try {
            return objectMapper.readValue(cleanJson(raw), ParsedResume.class);
        } catch (Exception e) {
            log.error("[AI] Resume parse JSON deserialization failed: {}", e.getMessage());
            throw new RuntimeException("Resume parsing failed — invalid AI response format");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JD PARSE — called once per job ingestion
    // ─────────────────────────────────────────────────────────────────────────

    public ParsedJob parseJobDescription(String rawJd, String company) {
        log.info("[AI] Parsing JD for company: {} — calling Claude", company);

        String systemPrompt = """
            You are a senior technical recruiter. Read this job description exactly as
            a recruiter would. Extract both the explicit requirements AND the implicit signals.
            Return ONLY valid JSON, no markdown.
            """;

        String userPrompt = """
            Parse this job description. Return JSON:
            {
              "role": "Normalized role name",
              "domain": "fintech | healthtech | ecommerce | enterprise | startup | general",
              "seniority": "JUNIOR | MID | SENIOR | STAFF | PRINCIPAL",
              "exp_years_min": integer or null,
              "must_have": ["required skill 1", "required skill 2"],
              "nice_to_have": ["preferred skill 1"],
              "strong_signals": ["skills mentioned 2+ times"],
              "tech_stack": ["all technologies mentioned"],
              "team_signals": "any hints about team size, remote/hybrid, culture",
              "ats_blueprint": {
                "exact_keywords": ["exact spelling the ATS looks for"],
                "keyword_weights": {"Spring Boot": 10, "Kafka": 9},
                "power_verbs": ["architected", "engineered"],
                "repeated_phrases": ["phrases appearing 2+ times"],
                "required_certs": ["AWS Certified", "PMP"],
                "seniority_signals": ["5+ years", "technical leadership"]
              },
              "embedding_text": "Dense paragraph of this job's requirements for semantic search"
            }
            
            COMPANY: %s
            JOB DESCRIPTION:
            %s
            """.formatted(company, rawJd);

        String raw = callClaude(systemPrompt, userPrompt, 1000);

        try {
            return objectMapper.readValue(cleanJson(raw), ParsedJob.class);
        } catch (Exception e) {
            log.error("[AI] JD parse failed: {}", e.getMessage());
            throw new RuntimeException("JD parsing failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPANY INTEL — called once per new company, cached forever
    // ─────────────────────────────────────────────────────────────────────────

    public CompanyIntelResult buildCompanyIntel(String companyName) {
        log.info("[AI] Building company intel for: {} — calling Claude", companyName);

        String systemPrompt = """
            You are an expert in tech company cultures and hiring practices.
            Based on your knowledge, build an intelligence profile for this company.
            Return ONLY valid JSON.
            """;

        String userPrompt = """
            Build a company intelligence profile for: %s
            
            Return JSON:
            {
              "industry": "Investment Banking | Payments | SaaS | Startup | etc",
              "engineering_culture": "One phrase describing their eng culture",
              "known_for": ["thing 1", "thing 2", "thing 3"],
              "values_in_engineers": ["ownership of production systems", "security-first thinking"],
              "tech_stack_known": ["Java", "Kafka", "Kubernetes"],
              "buzzwords": ["language their engineers use"],
              "what_impresses": "What makes a strong impression in interviews",
              "what_to_avoid": "Common mistakes candidates make for this company"
            }
            """.formatted(companyName);

        String raw = callClaude(systemPrompt, userPrompt, 600);

        try {
            return objectMapper.readValue(cleanJson(raw), CompanyIntelResult.class);
        } catch (Exception e) {
            log.error("[AI] Company intel build failed: {}", e.getMessage());
            throw new RuntimeException("Company intel build failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ATS REWRITE — called once per (candidate, job) pair, cached after
    // ─────────────────────────────────────────────────────────────────────────

    public AtsRewriteResult rewriteForAts(
            String resumeText,
            ParsedJob jobProfile,
            CompanyIntelResult companyIntel,
            String companyName) {

        log.info("[AI] Rewriting resume for {} — calling Claude", companyName);

        String systemPrompt = """
            You are an elite resume writer who deeply understands what specific companies
            look for. You write bullet points that sound natural and specific — not
            keyword-stuffed. Return ONLY valid JSON.
            """;

        String userPrompt = """
            Rewrite this resume for %s. Use three layers of intelligence:
            
            LAYER 1 — JD REQUIREMENTS:
            Must have: %s
            ATS Keywords: %s
            Power Verbs: %s
            
            LAYER 2 — COMPANY INTELLIGENCE:
            Culture: %s
            Values in engineers: %s
            Buzzwords they use: %s
            What impresses them: %s
            What to avoid: %s
            
            LAYER 3 — CANDIDATE'S REAL EXPERIENCE:
            %s
            
            Rules:
            - Bullet points must sound NATURAL — not keyword-stuffed
            - Reframe real experience in the language %s engineers use
            - Every bullet should be specific and quantified where possible
            - Do NOT invent experience the candidate doesn't have
            
            Return JSON:
            {
              "summary": "3 sentence professional summary tailored to %s",
              "experience": [
                {
                  "title": "Job Title",
                  "company": "Company",
                  "period": "2021–Present",
                  "bullets": ["rewritten bullet 1", "bullet 2", "bullet 3", "bullet 4", "bullet 5"]
                }
              ],
              "skills": {
                "core_backend": ["skill"],
                "distributed_systems": ["skill"],
                "cloud_infrastructure": ["skill"],
                "databases": ["skill"]
              },
              "keywords_injected": ["keyword1", "keyword2"],
              "ats_score": integer 0-100,
              "company_alignment_score": integer 0-100,
              "why_this_works": "2 sentences explaining the tailoring"
            }
            """.formatted(
                companyName,
                jobProfile.getMustHave(),
                jobProfile.getAtsBlueprintKeywords(),
                jobProfile.getAtsBlueprintVerbs(),
                companyIntel.getEngineeringCulture(),
                companyIntel.getValuesInEngineers(),
                companyIntel.getBuzzwords(),
                companyIntel.getWhatImpresses(),
                companyIntel.getWhatToAvoid(),
                resumeText,
                companyName,
                companyName
            );

        String raw = callClaude(systemPrompt, userPrompt, 1000);

        try {
            return objectMapper.readValue(cleanJson(raw), AtsRewriteResult.class);
        } catch (Exception e) {
            log.error("[AI] ATS rewrite failed: {}", e.getMessage());
            throw new RuntimeException("ATS rewrite failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE HTTP — Claude API call
    // ─────────────────────────────────────────────────────────────────────────

    private String callClaude(String systemPrompt, String userPrompt, int maxTokens) {
        Map<String, Object> body = Map.of(
            "model",      claudeModel,
            "max_tokens", maxTokens,
            "system",     systemPrompt,
            "messages",   List.of(Map.of("role", "user", "content", userPrompt))
        );

        try {
            Map response = webClient.post()
                .uri("https://api.anthropic.com/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            return (String) content.get(0).get("text");

        } catch (Exception e) {
            log.error("[AI] Claude API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable: " + e.getMessage());
        }
    }

    private String cleanJson(String raw) {
        return raw.replaceAll("```json", "").replaceAll("```", "").trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONSE DTOs
    // ─────────────────────────────────────────────────────────────────────────

    @Data public static class ParsedResume {
        private String detectedRole;
        private String primarySpec;
        private String seniority;
        private Integer yearsExp;
        private String domain;
        private List<String> coreStack;
        private List<String> supportingSkills;
        private List<String> possibleTitles;
        private String embeddingText;

        public String toEmbeddingText() {
            return embeddingText != null ? embeddingText :
                detectedRole + " " + primarySpec + " " + String.join(" ", coreStack);
        }
    }

    @Data public static class ParsedJob {
        private String role;
        private String domain;
        private String seniority;
        private Integer expYearsMin;
        private List<String> mustHave;
        private List<String> niceToHave;
        private List<String> strongSignals;
        private List<String> techStack;
        private String teamSignals;
        private Map<String, Object> atsBlueprint;
        private String embeddingText;

        public List<String> getAtsBlueprintKeywords() {
            if (atsBlueprint == null) return List.of();
            return (List<String>) atsBlueprint.getOrDefault("exact_keywords", List.of());
        }

        public List<String> getAtsBlueprintVerbs() {
            if (atsBlueprint == null) return List.of();
            return (List<String>) atsBlueprint.getOrDefault("power_verbs", List.of());
        }
    }

    @Data public static class CompanyIntelResult {
        private String industry;
        private String engineeringCulture;
        private List<String> knownFor;
        private List<String> valuesInEngineers;
        private List<String> techStackKnown;
        private List<String> buzzwords;
        private String whatImpresses;
        private String whatToAvoid;
    }

    @Data public static class AtsRewriteResult {
        private String summary;
        private List<Map<String, Object>> experience;
        private Map<String, Object> skills;
        private List<String> keywordsInjected;
        private Integer atsScore;
        private Integer companyAlignmentScore;
        private String whyThisWorks;
    }
}
