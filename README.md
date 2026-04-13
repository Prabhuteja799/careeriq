# CareerIQ

A full-stack career intelligence platform that analyzes your resume against job descriptions, generates match scores, and surfaces skill gaps — powered by Claude AI and OpenAI embeddings.

Built with Spring Boot 3 on the backend and Next.js 14 on the frontend.

---

## What it does

- Upload your resume (PDF) and a job description
- Claude analyzes both and returns a fit score, matched skills, missing skills, and tailored recommendations
- Semantic search via OpenAI embeddings surfaces the most relevant job matches
- Cover letter generation grounded in the specific JD and your resume
- Job history tracked in PostgreSQL, cached in Redis

---

## Architecture

```
Next.js 14 (TypeScript)
    │  REST
    ▼
Spring Boot 3 (Java 17)
    ├── Resume parsing endpoint
    ├── JD analysis (Claude API)
    ├── Embeddings (OpenAI)
    ├── PostgreSQL (Neon) — job + resume storage
    └── Redis — caching + queues
         │
         └── AWS S3 — resume PDF storage
```

---

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3, Gradle |
| Frontend | Next.js 14, TypeScript |
| AI | Claude API (Anthropic) — analysis + generation |
| Embeddings | OpenAI `text-embedding-ada-002` |
| Database | PostgreSQL (Neon) |
| Cache | Redis (Docker) |
| File storage | AWS S3 |

---

## Setup

### Prerequisites

- Java 17 (`brew install openjdk@17`)
- Node.js 18+
- Docker (for Redis)
- PostgreSQL database (Neon free tier works)
- AWS account with an S3 bucket
- Anthropic API key
- OpenAI API key

### Backend

```bash
cd backend
cp .env.example .env
# Fill in DB URL, API keys, S3 config
./gradlew bootRun
# → http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
cp .env.local.example .env.local
# Set NEXT_PUBLIC_API_URL=http://localhost:8080
npm run dev
# → http://localhost:3000
```

### Redis (Docker)

```bash
docker run -d -p 6379:6379 redis:alpine
```

---

## Environment variables

**Backend `.env`**

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string |
| `ANTHROPIC_API_KEY` | Claude API key |
| `OPENAI_API_KEY` | OpenAI embeddings key |
| `AWS_ACCESS_KEY_ID` | S3 access key |
| `AWS_SECRET_ACCESS_KEY` | S3 secret key |
| `AWS_S3_BUCKET` | S3 bucket name |
| `REDIS_URL` | Redis connection string |

---

## Future improvements

- LinkedIn job scraping integration
- Batch resume analysis for multiple JDs
- Interview prep questions generated from gap analysis
- Email digest of new matching jobs
