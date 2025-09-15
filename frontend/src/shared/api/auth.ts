interface LoginRequest {
  email: string
  password: string
}

interface RegisterRequest {
  username: string
  password: string
  email: string
}

interface UserResponse {
  id: number
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  approvedAt?: string
}

class AuthAPI {
  private readonly baseURL = '/api/auth'

  async login(data: LoginRequest): Promise<UserResponse> {
    const response = await fetch(`${this.baseURL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 중요: 세션 쿠키 포함
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.')
      }
      throw new Error('로그인 중 오류가 발생했습니다.')
    }

    return response.json()
  }

  async register(data: RegisterRequest): Promise<string> {
    const response = await fetch(`${this.baseURL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || '회원가입 중 오류가 발생했습니다.')
    }

    return response.text()
  }

  async logout(): Promise<string> {
    const response = await fetch(`${this.baseURL}/logout`, {
      method: 'POST',
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error('로그아웃 중 오류가 발생했습니다.')
    }

    return response.text()
  }

  async getCurrentUser(): Promise<UserResponse> {
    const response = await fetch(`${this.baseURL}/me`, {
      method: 'GET',
      credentials: 'include',
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증되지 않은 사용자입니다.')
      }
      throw new Error('사용자 정보를 가져오는데 실패했습니다.')
    }

    return response.json()
  }
}

export const authAPI = new AuthAPI()
export type { LoginRequest, RegisterRequest, UserResponse }