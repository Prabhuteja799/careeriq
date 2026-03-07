# CareerIQ — V1 Setup Guide

## Stack
| Layer | Tech |
|---|---|
| Backend | Java 17 + Spring Boot 3 + Gradle |
| Frontend | Next.js 14 + TypeScript |
| Database | PostgreSQL (Neon — free tier) |
| Cache / Queues | Redis (local Docker) |
| File Storage | AWS S3 |
| AI | Claude API (Anthropic) + OpenAI Embeddings |

---

## Prerequisites (Mac)

```bash
# 1. Java 17
brew install openjdk@17
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
source ~/.zshrc
java -version   # should show 17

# 2. Gradle
brew install gradle
gradle -v

# 3. Node.js 20
brew install node@20
node -v

# 4. Docker (for local Redis)
brew install --cask docker
open /Applications/Docker.app
```

---

## 1. Clone & Structure

```bash
git clone <your-repo> careeriq
cd careeriq
```

Project structure:
```
careeriq/
├── backend/          ← Spring Boot (Java 17 + Gradle)
└── frontend/         ← Next.js 14 (TypeScript)
```

---

## 2. Database — Neon PostgreSQL (free)

1. Go to https://neon.tech and create a free account
2. Create a new project: `careeriq-dev`
3. Copy the connection string — looks like:
   `postgresql://username:password@ep-xxx.us-east-1.aws.neon.tech/neondb?sslmode=require`
4. Enable pgvector extension in Neon SQL editor:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

---

## 3. Redis — Local Docker

```bash
# Start Redis in Docker
docker run -d --name careeriq-redis -p 6379:6379 redis:7-alpine

# Verify it's running
docker ps | grep redis
```

---

## 4. Backend Setup

```bash
cd backend

# Copy env file
cp .env.example .env
# Edit .env and fill in your values (Neon URL, AWS keys, API keys)
nano .env

# Make gradlew executable
chmod +x gradlew

# Build (downloads dependencies)
./gradlew build -x test

# Run
./gradlew bootRun
```

Backend runs on: **http://localhost:8080**

Health check: http://localhost:8080/actuator/health

### Generate JWT Secret
```bash
openssl rand -base64 64
# Paste output into .env as JWT_SECRET
```

---

## 5. Frontend Setup

```bash
cd ../frontend

# Copy env file
cp .env.example .env.local

# Install dependencies
npm install

# Run dev server
npm run dev
```

Frontend runs on: **http://localhost:3000**

---

## 6. AWS S3 Setup (for resume storage)

```bash
# Create S3 bucket
aws s3 mb s3://careeriq-resumes-dev --region us-east-1

# Set bucket policy (private, only your IAM user)
# In AWS Console: S3 → careeriq-resumes-dev → Permissions → Block all public access ✓
```

Create IAM user with S3 permissions:
1. AWS Console → IAM → Users → Create user: `careeriq-dev`
2. Attach policy: `AmazonS3FullAccess` (or create a scoped policy)
3. Create access keys → add to `.env`

---

## 7. Verify Everything Works

```bash
# 1. Health check
curl http://localhost:8080/actuator/health

# 2. Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","firstName":"Test","lastName":"User"}'

# 3. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
# Copy the accessToken from the response

# 4. Upload a resume (use the token from step 3)
curl -X POST http://localhost:8080/api/resume/upload \
  -H "Authorization: Bearer <your-token>" \
  -F "file=@/path/to/your-resume.pdf"
# Returns {"jobId":"xxx","status":"PROCESSING"}

# 5. Poll status
curl http://localhost:8080/api/resume/status/<jobId> \
  -H "Authorization: Bearer <your-token>"
```

---

## 8. Database Migrations

Flyway runs automatically on startup. Check migration status:

```bash
# Connect to Neon via psql
psql "postgresql://username:password@host/dbname?sslmode=require"

# Check migrations ran
SELECT * FROM flyway_schema_history;

# Check tables exist
\dt
```

---

## Gradle Commands Cheat Sheet

```bash
./gradlew bootRun          # Run the app
./gradlew build            # Build JAR
./gradlew build -x test    # Build, skip tests
./gradlew test             # Run tests
./gradlew clean build      # Clean and rebuild
./gradlew dependencies     # List all dependencies
./gradlew bootJar          # Build production JAR
```

---

## Project File Structure (Backend)

```
backend/src/main/java/com/careeriq/
├── CareerIQApplication.java          ← Entry point
├── config/
│   ├── SecurityConfig.java           ← JWT + CORS config
│   └── AppConfig.java                ← WebClient, thread pools, Jackson
├── controller/
│   ├── AuthController.java           ← POST /api/auth/register, /login
│   ├── ResumeController.java         ← POST /api/resume/upload
│   ├── MatchController.java          ← GET  /api/matches
│   ├── JobController.java            ← GET  /api/jobs/search
│   └── AtsController.java            ← GET  /api/ats/analyze, POST /api/ats/rewrite
├── model/
│   ├── entity/Entities.java          ← All JPA entities
│   └── dto/Dtos.java                 ← All request/response DTOs
├── security/
│   ├── jwt/JwtService.java           ← Token generation + validation
│   └── filter/JwtAuthFilter.java     ← Per-request JWT filter
├── pipeline/
│   ├── resume/
│   │   ├── ResumeUploadService.java  ← Upload + queue + poll status
│   │   └── ResumePipelineOrchestrator.java ← Async AI parse pipeline
│   └── matching/
│       └── DeltaSyncScheduler.java   ← Hourly background sync
├── service/
│   ├── AiService.java                ← All Claude + AI calls
│   ├── EmbeddingService.java         ← OpenAI embeddings + cosine sim
│   └── S3Service.java                ← AWS S3 uploads
└── exception/
    ├── GlobalExceptionHandler.java   ← All error responses
    └── ResourceNotFoundException.java

backend/src/main/resources/
├── application.yml                   ← Full app config
└── db/migration/
    └── V1__init_schema.sql           ← Complete database schema
```

---

## What's Next (Week 1 checklist)

- [ ] Fill in `.env` with real keys
- [ ] Run `./gradlew bootRun` — app starts
- [ ] Hit `/actuator/health` — returns UP
- [ ] Register a user — JWT returned
- [ ] Upload a resume PDF — async parse fires
- [ ] Check `/api/resume/status/{jobId}` — DONE
- [ ] Check `/api/resume/profile` — see extracted profile
- [ ] Add 10 test jobs via POST `/api/jobs/ingest`
- [ ] Check `/api/matches` — see ranked results
