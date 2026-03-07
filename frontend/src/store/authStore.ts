import { create } from 'zustand'
import { User } from '@/lib/types'
import { authApi } from '@/lib/api/client'

interface AuthState {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean

  login:    (email: string, password: string) => Promise<void>
  register: (data: { email: string; password: string; firstName: string; lastName: string }) => Promise<void>
  logout:   () => void
  setUser:  (user: User) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: false,
  isAuthenticated: false,

  login: async (email, password) => {
    set({ isLoading: true })
    try {
      const data = await authApi.login({ email, password })
      localStorage.setItem('accessToken',  data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      set({ user: data.user, isAuthenticated: true, isLoading: false })
    } catch (error) {
      set({ isLoading: false })
      throw error
    }
  },

  register: async (formData) => {
    set({ isLoading: true })
    try {
      const data = await authApi.register(formData)
      localStorage.setItem('accessToken',  data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      set({ user: data.user, isAuthenticated: true, isLoading: false })
    } catch (error) {
      set({ isLoading: false })
      throw error
    }
  },

  logout: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    set({ user: null, isAuthenticated: false })
    window.location.href = '/auth/login'
  },

  setUser: (user) => set({ user, isAuthenticated: true }),
}))
