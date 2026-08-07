import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { primaryButtonClass, secondaryButtonClass } from '../../components/buttonStyles'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getUsers, resetUserPassword } from '../../services/userService'
import type { ErrorResponse, Page, UserResponse } from '../../types'
import { USER_ROLE_TONE, userRoleLabel } from './userRoleOptions'

const MIN_PASSWORD_LENGTH = 8

export function UserListPage() {
  const [users, setUsers] = useState<Page<UserResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [resetPasswordUser, setResetPasswordUser] = useState<UserResponse | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadUsers() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getUsers({ page })
        if (!cancelled) {
          setUsers(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar os usuários. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadUsers()

    return () => {
      cancelled = true
    }
  }, [page])

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-foreground">Usuários</h1>
        <Link to="/manager/users/new" className={primaryButtonClass}>
          Novo Usuário
        </Link>
      </div>

      {successMessage && (
        <div role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2.5 text-sm text-green-700">
          {successMessage}
        </div>
      )}

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !users || users.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhum usuário encontrado.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Papel</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {users.content.map((user) => (
                  <tr key={user.id}>
                    <td className="px-4 py-3 font-medium text-foreground">{user.name}</td>
                    <td className="px-4 py-3 text-gray-500">{user.email}</td>
                    <td className="px-4 py-3">
                      <StatusBadge label={userRoleLabel(user.role)} tone={USER_ROLE_TONE[user.role]} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => {
                          setSuccessMessage(null)
                          setResetPasswordUser(user)
                        }}
                        className="text-sm font-medium text-accent-dark hover:underline"
                      >
                        Redefinir Senha
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {users.content.map((user) => (
              <li key={user.id} className="rounded-lg border border-muted bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-2">
                  <span className="font-medium text-foreground">{user.name}</span>
                  <StatusBadge label={userRoleLabel(user.role)} tone={USER_ROLE_TONE[user.role]} />
                </div>
                <p className="mt-1 text-sm text-gray-500">{user.email}</p>
                <div className="mt-3 flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setSuccessMessage(null)
                      setResetPasswordUser(user)
                    }}
                    className="text-sm font-medium text-accent-dark hover:underline"
                  >
                    Redefinir Senha
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <Pagination page={users.number} totalPages={users.totalPages} onPageChange={setPage} />
        </>
      )}

      {resetPasswordUser && (
        <ResetPasswordModal
          user={resetPasswordUser}
          onClose={() => setResetPasswordUser(null)}
          onReset={() => {
            setResetPasswordUser(null)
            setSuccessMessage(`Senha de ${resetPasswordUser.name} redefinida com sucesso.`)
          }}
        />
      )}
    </div>
  )
}

interface ResetPasswordModalProps {
  user: UserResponse
  onClose: () => void
  onReset: () => void
}

function ResetPasswordModal({ user, onClose, onReset }: ResetPasswordModalProps) {
  const [password, setPassword] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const passwordLength = password.length
  const passwordMeetsMinimum = passwordLength >= MIN_PASSWORD_LENGTH
  const passwordHintClass = passwordLength === 0
    ? 'text-gray-500'
    : passwordMeetsMinimum
      ? 'text-green-700'
      : 'text-red-600'

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    if (!passwordMeetsMinimum) {
      setSubmitError(`Senha deve ter no mínimo ${MIN_PASSWORD_LENGTH} caracteres`)
      return
    }

    setIsSubmitting(true)

    try {
      await resetUserPassword(user.id, password)
      onReset()
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível redefinir a senha. Tente novamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-lg font-semibold text-foreground">Redefinir Senha</h2>
        <p className="mt-1 text-sm text-gray-500">
          Nova senha para <strong>{user.name}</strong> ({user.email}).
        </p>

        <form onSubmit={handleSubmit} noValidate className="mt-4 space-y-4">
          {submitError && (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
              {submitError}
            </div>
          )}

          <div>
            <label htmlFor="reset-password" className="mb-1.5 block text-sm font-medium text-foreground">
              Nova senha
            </label>
            <input
              id="reset-password"
              type="password"
              autoComplete="new-password"
              autoFocus
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
            />
            <p className={`mt-1.5 text-xs ${passwordHintClass}`}>
              {passwordMeetsMinimum ? '✓ ' : ''}
              {passwordLength}/{MIN_PASSWORD_LENGTH} caracteres mínimos
            </p>
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button type="submit" disabled={isSubmitting} className={primaryButtonClass}>
              {isSubmitting ? 'Salvando...' : 'Redefinir'}
            </button>
            <button type="button" onClick={onClose} className={secondaryButtonClass}>
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
