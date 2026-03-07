// ─── CareerIQ — Shared TypeScript Types ──────────────────────────────────────

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  hasResume: boolean
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface CandidateProfile {
  id: string
  detectedRole: string
  primarySpec: string
  seniority: Seniority
  yearsExp: number
  domain: string
  coreStack: string[]
  supportingSkills: string[]
  possibleTitles: string[]
  createdAt: string
}

export interface Job {
  id: string
  title: string
  company: string
  location: string
  remote: boolean
  salaryMin?: string
  salaryMax?: string
  status: JobStatus
  createdAt: string
  // Profile fields
  role?: string
  seniority?: Seniority
  expYearsMin?: number
  mustHave?: string[]
  niceToHave?: string[]
  techStack?: string[]
  domain?: string
}

export interface Match {
  jobId: string
  jobTitle: string
  company: string
  location: string
  remote: boolean
  salaryMin?: string
  salaryMax?: string
  postedAt: string
  // Match intelligence
  score: number
  skillMatch: number
  expMatch: MatchStrength
  roleMatch: MatchStrength
  seniorityMatch: SeniorityMatch
  matchedSkills: string[]
  missingSkills: string[]
  reasons: string[]
  hasAtsResume: boolean
}

export interface AtsGapReport {
  jobId: string
  company: string
  jobTitle: string
  atsScoreBefore: number
  atsScoreAfter: number
  missingKeywords: string[]
  presentKeywords: string[]
  cultureGaps: string[]
  top3Issues: string[]
}

export interface AtsResume {
  id: string
  jobId: string
  company: string
  summary: string
  experience: ExperienceSection[]
  skills: Record<string, string[]>
  keywordsInjected: string[]
  atsScore: number
  companyAlignmentScore: number
  whyThisWorks: string
}

export interface ExperienceSection {
  title: string
  company: string
  period: string
  bullets: string[]
}

// ── V1 Search Filters ─────────────────────────────────────────────────────────

export interface JobFilterRequest {
  title?: string
  skillsIn?: string       // comma-separated
  skillsEx?: string       // comma-separated
  expMin?: number
  expMax?: number
  seniority?: Seniority
  location?: string
  remote?: boolean
  domain?: string
  postedWithin?: 1 | 3 | 7 | 14 | 30
}

export interface MatchFilterRequest {
  minScore?: number
  skillsIn?: string
  skillsEx?: string
  remote?: boolean
  seniority?: Seniority
  domain?: string
  postedWithin?: 1 | 3 | 7 | 14 | 30
}

// ── Enums ─────────────────────────────────────────────────────────────────────

export type Seniority     = 'JUNIOR' | 'MID' | 'SENIOR' | 'STAFF' | 'PRINCIPAL'
export type JobStatus     = 'ACTIVE' | 'EXPIRED' | 'FILLED' | 'DRAFT'
export type MatchStrength = 'strong' | 'good' | 'partial' | 'weak'
export type SeniorityMatch = 'exact' | 'close' | 'stretch' | 'mismatch'

// ── API Response wrappers ─────────────────────────────────────────────────────

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface ApiError {
  status: number
  message: string
  errors?: string[]
  timestamp: string
}

export interface UploadStatus {
  jobId: string
  status: 'PROCESSING' | 'DONE' | 'FAILED'
  message: string
  candidateId?: string
}
