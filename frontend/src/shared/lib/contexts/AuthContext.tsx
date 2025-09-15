import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authAPI, type UserResponse } from '../../api/auth'

export interface User {
  id: number
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  approvedAt?: string
}

interface LoginData {
  email: string
  password: string
}

interface SignupData {
  username: string
  password: string
  email: string
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean

  // Actions
  login: (data: LoginData) => Promise<void>
  signup: (data: SignupData) => Promise<void>
  logout: () => Promise<void>
  checkAuthStatus: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user && user.status === 'APPROVED'

  // 로그인 함수
  const login = async (data: LoginData): Promise<void> => {
    setIsLoading(true)

    try {
      const response = await authAPI.login(data)

      const user: User = {
        id: response.id,
        username: response.username,
        email: response.email,
        role: response.role,
        status: response.status,
        createdAt: response.createdAt,
        approvedAt: response.approvedAt
      }

      setUser(user)

    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  // 회원가입 함수
  const signup = async (data: SignupData): Promise<void> => {
    setIsLoading(true)

    try {
      const message = await authAPI.register(data)
      console.log('Registration successful:', message)

    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  // 로그아웃 함수
  const logout = async (): Promise<void> => {
    try {
      await authAPI.logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      setUser(null)
    }
  }

  // 인증 상태 확인
  const checkAuthStatus = async (): Promise<void> => {
    setIsLoading(true)

    try {
      const response = await authAPI.getCurrentUser()

      const user: User = {
        id: response.id,
        username: response.username,
        email: response.email,
        role: response.role,
        status: response.status,
        createdAt: response.createdAt,
        approvedAt: response.approvedAt
      }

      setUser(user)
    } catch (error) {
      console.error('Auth check failed:', error)
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  // 앱 시작 시 인증 상태 확인
  useEffect(() => {
    checkAuthStatus()
  }, [])

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated,
    login,
    signup,
    logout,
    checkAuthStatus
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}