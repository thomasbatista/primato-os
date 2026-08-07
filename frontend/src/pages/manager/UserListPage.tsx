import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { primaryButtonClass } from '../../components/buttonStyles'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getUsers } from '../../services/userService'
import type { Page, UserResponse } from '../../types'
import { USER_ROLE_TONE, userRoleLabel } from './userRoleOptions'

export function UserListPage() {
  const [users, setUsers] = useState<Page<UserResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

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
              </li>
            ))}
          </ul>

          <Pagination page={users.number} totalPages={users.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
