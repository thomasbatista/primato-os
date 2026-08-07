import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useSearchParams, type Location } from 'react-router-dom'
import axios from 'axios'
import { jwtDecode } from 'jwt-decode'
import { useAuth } from '../hooks/useAuth'
import { login as loginRequest } from '../services/authService'
import type { ErrorResponse, UserRole } from '../types'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

interface FieldErrors {
  email?: string
  password?: string
}

interface LocationState {
  from?: Location
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const sessionExpired = searchParams.get('sessionExpired') === 'true'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function validate(): boolean {
    const errors: FieldErrors = {}

    if (!email.trim()) {
      errors.email = 'Informe seu email'
    } else if (!EMAIL_PATTERN.test(email.trim())) {
      errors.email = 'Informe um email válido'
    }

    if (!password) {
      errors.password = 'Informe sua senha'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)

    try {
      const response = await loginRequest({ email: email.trim(), password })
      login(response.token)

      const { role } = jwtDecode<{ role: UserRole }>(response.token)
      const homePath = role === 'MANAGER' ? '/manager' : '/worker'
      const from = (location.state as LocationState | null)?.from

      // Only honor the pre-login redirect target if it belongs to this role's
      // section — otherwise a manager logging out from a manager-only page and
      // logging back in as a worker (or vice versa) would bounce to /unauthorized.
      navigate(from && from.pathname.startsWith(homePath) ? from : homePath, { replace: true })
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível conectar ao servidor. Tente novamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="bg-primary py-5 text-center">
        <h1 className="text-xl font-semibold tracking-wide text-white">Primato OS</h1>
      </header>

      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm">
          <form
            noValidate
            onSubmit={handleSubmit}
            className="rounded-lg border border-muted bg-white p-6 shadow-sm sm:p-8"
          >
            {submitError ? (
              <div
                role="alert"
                className="mb-5 rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700"
              >
                {submitError}
              </div>
            ) : (
              sessionExpired && (
                <div
                  role="status"
                  className="mb-5 rounded-md border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800"
                >
                  Sua sessão expirou, entre novamente.
                </div>
              )
            )}

            <div className="mb-4">
              <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-foreground">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value)
                  setFieldErrors((prev) => ({ ...prev, email: undefined }))
                }}
                aria-invalid={fieldErrors.email ? true : undefined}
                className="w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
              />
              {fieldErrors.email && (
                <p className="mt-1.5 text-sm text-red-600">{fieldErrors.email}</p>
              )}
            </div>

            <div className="mb-6">
              <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-foreground">
                Senha
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value)
                  setFieldErrors((prev) => ({ ...prev, password: undefined }))
                }}
                aria-invalid={fieldErrors.password ? true : undefined}
                className="w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
              />
              {fieldErrors.password && (
                <p className="mt-1.5 text-sm text-red-600">{fieldErrors.password}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-md bg-accent px-4 py-2.5 text-base font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
