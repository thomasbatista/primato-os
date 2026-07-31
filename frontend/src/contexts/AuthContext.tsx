import { createContext, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'
import { clearToken, getToken, setToken } from '../services/tokenStorage'
import type { UserRole } from '../types'

export interface AuthUser {
  name: string
  email: string
  role: UserRole
}

export interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
}

interface TokenPayload {
  sub: string
  name: string
  role: UserRole
  exp: number
}

function userFromToken(token: string): AuthUser | null {
  try {
    const payload = jwtDecode<TokenPayload>(token)

    if (payload.exp * 1000 < Date.now()) {
      return null
    }

    return { name: payload.name, email: payload.sub, role: payload.role }
  } catch {
    return null
  }
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = getToken()
    return token ? userFromToken(token) : null
  })

  function login(token: string) {
    setToken(token)
    setUser(userFromToken(token))
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: user !== null, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
