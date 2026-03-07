-- CareerIQ V1 Schema
-- Flyway migration: V1__init_schema.sql

-- ── Enable extensions ─────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";          -- pgvector for embeddings

-- ── USERS ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    first_name      TEXT,
    last_name       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ
);

-- ── CANDIDATES ────────────────────────────────────────────────────────────────
CREATE TABLE candidates (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_hash         TEXT NOT NULL UNIQUE,
    s3_key              TEXT,
    detected_role       TEXT,
    primary_spec        TEXT,
    seniority           TEXT,
    years_exp           INTEGER,
    domain              TEXT,
    core_stack          JSONB,
    supporting_skills   JSONB,
    possible_titles     JSONB,
    last_matched_at     TIMESTAMPTZ,               -- Delta sync cursor
    version             INTEGER NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_user         ON candidates(user_id);
CREATE INDEX idx_candidate_last_matched ON candidates(last_matched_at);

-- ── JOBS ──────────────────────────────────────────────────────────────────────
CREATE TABLE jobs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       TEXT NOT NULL,
    company     TEXT NOT NULL,
    location    TEXT,
    remote      BOOLEAN DEFAULT FALSE,
    salary_min  TEXT,
    salary_max  TEXT,
    raw_jd      TEXT,
    source      TEXT,
    status      TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    parsed_at   TIMESTAMPTZ
);

CREATE INDEX idx_job_created        ON jobs(created_at DESC);
CREATE INDEX idx_job_status_created ON jobs(status, created_at);
CREATE INDEX idx_job_company        ON jobs(company);

-- ── JOB PROFILES ──────────────────────────────────────────────────────────────
CREATE TABLE job_profiles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id          UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
    role            TEXT,
    domain          TEXT,
    seniority       TEXT,
    exp_years_min   INTEGER,
    must_have       JSONB,
    nice_to_have    JSONB,
    strong_signals  JSONB,
    tech_stack      JSONB,
    team_signals    TEXT
);

-- ── ATS BLUEPRINTS ────────────────────────────────────────────────────────────
CREATE TABLE ats_blueprints (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id              UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
    exact_keywords      JSONB,
    keyword_weights     JSONB,
    power_verbs         JSONB,
    repeated_phrases    JSONB,
    required_certs      JSONB,
    seniority_signals   JSONB
);

-- ── COMPANY INTEL ─────────────────────────────────────────────────────────────
CREATE TABLE company_intel (
    company_name            TEXT PRIMARY KEY,
    industry                TEXT,
    engineering_culture     TEXT,
    known_for               JSONB,
    values_in_engineers     JSONB,
    tech_stack_known        JSONB,
    buzzwords               JSONB,
    what_impresses          TEXT,
    what_to_avoid           TEXT,
    last_researched         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── JOB EMBEDDINGS ────────────────────────────────────────────────────────────
CREATE TABLE job_embeddings (
    job_id      UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    embedding   vector(1536) NOT NULL,
    model       TEXT NOT NULL DEFAULT 'text-embedding-3-small',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- HNSW index for fast approximate nearest-neighbor search
CREATE INDEX idx_job_embedding_hnsw
    ON job_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ── CANDIDATE EMBEDDINGS ──────────────────────────────────────────────────────
CREATE TABLE candidate_embeddings (
    candidate_id    UUID PRIMARY KEY REFERENCES candidates(id) ON DELETE CASCADE,
    embedding       vector(1536) NOT NULL,
    model           TEXT NOT NULL DEFAULT 'text-embedding-3-small',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_embedding_hnsw
    ON candidate_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ── MATCH SCORES ──────────────────────────────────────────────────────────────
CREATE TABLE match_scores (
    candidate_id    UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    score           FLOAT NOT NULL,
    skill_match     FLOAT,
    exp_match       TEXT,
    role_match      TEXT,
    seniority_match TEXT,
    matched_skills  JSONB,
    missing_skills  JSONB,
    reasons         JSONB,
    scored_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    score_version   INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (candidate_id, job_id)
);

-- These two indexes power 99% of dashboard queries
CREATE INDEX idx_match_candidate_score  ON match_scores(candidate_id, score DESC);
CREATE INDEX idx_match_candidate_scored ON match_scores(candidate_id, scored_at DESC);

-- ── ATS RESUMES (cached rewrites) ─────────────────────────────────────────────
CREATE TABLE ats_resumes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id    UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    resume_json     JSONB NOT NULL,
    ats_score       FLOAT,
    keywords_injected JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(candidate_id, job_id)
);

-- ── SAVED JOBS ────────────────────────────────────────────────────────────────
CREATE TABLE saved_jobs (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id      UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, job_id)
);

-- ── APPLICATIONS ──────────────────────────────────────────────────────────────
CREATE TABLE applications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES jobs(id),
    status          TEXT NOT NULL DEFAULT 'APPLIED',
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes           TEXT,
    UNIQUE(user_id, job_id)
);
