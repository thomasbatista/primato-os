import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { primaryButtonClass } from '../../components/buttonStyles'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge, type StatusTone } from '../../components/StatusBadge'
import { deactivateWorker, getWorkers } from '../../services/workerService'
import type { Page, WorkerResponse } from '../../types'

function activeStatusBadge(active: boolean): { label: string; tone: StatusTone } {
  return active ? { label: 'Ativo', tone: 'success' } : { label: 'Inativo', tone: 'neutral' }
}

export function WorkerListPage() {
  const [workers, setWorkers] = useState<Page<WorkerResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [deactivatingId, setDeactivatingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadWorkers() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getWorkers({ page })
        if (!cancelled) {
          setWorkers(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar os colaboradores. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadWorkers()

    return () => {
      cancelled = true
    }
  }, [page])

  async function handleDeactivate(worker: WorkerResponse) {
    if (!window.confirm(`Tem certeza que deseja desativar ${worker.name}? Esta ação não pode ser desfeita.`)) {
      return
    }

    setActionError(null)
    setDeactivatingId(worker.id)

    try {
      const updated = await deactivateWorker(worker.id)
      setWorkers((prev) =>
        prev ? { ...prev, content: prev.content.map((w) => (w.id === updated.id ? updated : w)) } : prev,
      )
    } catch {
      setActionError('Não foi possível desativar o colaborador. Tente novamente.')
    } finally {
      setDeactivatingId(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-foreground">Colaboradores</h1>
        <Link to="/manager/workers/new" className={primaryButtonClass}>
          Novo Colaborador
        </Link>
      </div>

      {actionError && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
          {actionError}
        </div>
      )}

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !workers || workers.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhum colaborador encontrado.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">Função</th>
                  <th className="px-4 py-3">Telefone</th>
                  <th className="px-4 py-3">Login</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {workers.content.map((worker) => {
                  const status = activeStatusBadge(worker.active)
                  return (
                    <tr key={worker.id}>
                      <td className="px-4 py-3 font-medium text-foreground">{worker.name}</td>
                      <td className="px-4 py-3 text-gray-500">{worker.function ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500">{worker.phone ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500">{worker.user ? worker.user.email : 'Sem login'}</td>
                      <td className="px-4 py-3">
                        <StatusBadge label={status.label} tone={status.tone} />
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-3">
                          <Link
                            to={`/manager/workers/${worker.id}/edit`}
                            className="text-sm font-medium text-accent-dark hover:underline"
                          >
                            Editar
                          </Link>
                          {worker.active && (
                            <button
                              type="button"
                              disabled={deactivatingId === worker.id}
                              onClick={() => handleDeactivate(worker)}
                              className="text-sm font-medium text-red-700 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {deactivatingId === worker.id ? 'Desativando...' : 'Desativar'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {workers.content.map((worker) => {
              const status = activeStatusBadge(worker.active)
              return (
                <li key={worker.id} className="rounded-lg border border-muted bg-white p-4 shadow-sm">
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-foreground">{worker.name}</span>
                    <StatusBadge label={status.label} tone={status.tone} />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">{worker.function ?? 'Sem função definida'}</p>
                  <div className="mt-2 flex items-center justify-between text-xs text-gray-500">
                    <span>{worker.phone ?? '—'}</span>
                    <span>{worker.user ? worker.user.email : 'Sem login'}</span>
                  </div>
                  <div className="mt-3 flex justify-end gap-3">
                    <Link
                      to={`/manager/workers/${worker.id}/edit`}
                      className="text-sm font-medium text-accent-dark hover:underline"
                    >
                      Editar
                    </Link>
                    {worker.active && (
                      <button
                        type="button"
                        disabled={deactivatingId === worker.id}
                        onClick={() => handleDeactivate(worker)}
                        className="text-sm font-medium text-red-700 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {deactivatingId === worker.id ? 'Desativando...' : 'Desativar'}
                      </button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>

          <Pagination page={workers.number} totalPages={workers.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
