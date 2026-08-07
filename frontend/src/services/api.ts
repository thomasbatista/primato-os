import axios from 'axios'
import { clearToken, getToken } from './tokenStorage'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
})

api.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// Guards against firing more than one redirect if several requests happen to
// 401 around the same time (e.g. a page loading multiple resources in parallel
// right when the token expires).
let isRedirectingToLogin = false

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = error.config?.url?.endsWith('/auth/login')

    // The login page's own 401 (wrong password) must stay an inline form error,
    // not trigger the "session expired" redirect — that would immediately bounce
    // the user right back to the page they're already on.
    if (error.response?.status === 401 && !isLoginRequest && !isRedirectingToLogin) {
      isRedirectingToLogin = true
      clearToken()
      window.location.href = '/login?sessionExpired=true'
    }

    return Promise.reject(error)
  },
)

export default api
