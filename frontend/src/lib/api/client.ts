import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// ── Request interceptor — attach JWT ──────────────────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor — handle 401, refresh token ─────────────────────────
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) throw new Error('No refresh token')

        const { data } = await axios.post(`${BASE_URL}/api/auth/refresh`, null, {
          headers: { 'X-Refresh-Token': refreshToken },
        })

        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)

        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return api(originalRequest)
      } catch {
        // Refresh failed — clear tokens and redirect to login
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/auth/login'
      }
    }

    return Promise.reject(error)
  }
)

// ── API Methods ───────────────────────────────────────────────────────────────

export const authApi = {
  register: (data: { email: string; password: string; firstName: string; lastName: string }) =>
    api.post('/api/auth/register', data).then(r => r.data),

  login: (data: { email: string; password: string }) =>
    api.post('/api/auth/login', data).then(r => r.data),
}

export const resumeApi = {
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/api/resume/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },

  pollStatus: (jobId: string) =>
    api.get(`/api/resume/status/${jobId}`).then(r => r.data),

  getProfile: () =>
    api.get('/api/resume/profile').then(r => r.data),
}

export const matchApi = {
  getMatches: (params?: Record<string, unknown>) =>
    api.get('/api/matches', { params }).then(r => r.data),

  getDelta: () =>
    api.get('/api/matches/delta').then(r => r.data),

  getMatchDetail: (jobId: string) =>
    api.get(`/api/matches/${jobId}`).then(r => r.data),
}

export const jobApi = {
  search: (filters: Record<string, unknown>) =>
    api.get('/api/jobs/search', { params: filters }).then(r => r.data),

  getJob: (id: string) =>
    api.get(`/api/jobs/${id}`).then(r => r.data),
}

export const atsApi = {
  analyze: (jobId: string) =>
    api.get(`/api/ats/analyze/${jobId}`).then(r => r.data),

  rewrite: (jobId: string) =>
    api.post(`/api/ats/rewrite/${jobId}`).then(r => r.data),

  getCached: (jobId: string) =>
    api.get(`/api/ats/resume/${jobId}`).then(r => r.data),
}
