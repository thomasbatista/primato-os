import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getProjects } from '../../services/projectService'
import type { Page, ProjectResponse, ProjectStatus } from '../../types'
import { PROJECT_STATUS_OPTIONS, PROJECT_STATUS_TONE, projectStatusLabel } from './projectStatusOptions'

function formatDate(isoDate: string | null): string {
  if (!isoDate) {
    return '—'
  }

  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export function ProjectListPage() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Page<ProjectResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | ''>('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadProjects() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getProjects({ status: statusFilter || undefined, page })
        if (!cancelled) {
          setProjects(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar as obras. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadProjects()

    return () => {
      cancelled = true
    }
  }, [statusFilter, page])

  function handleStatusChange(value: string) {
    setStatusFilter(value as ProjectStatus | '')
    setPage(0)
  }

  function goToProject(id: number) {
    navigate(`/manager/projects/${id}/edit`)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-foreground">Obras</h1>
        <Link
          to="/manager/projects/new"
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
        >
          Nova Obra
        </Link>
      </div>

      <div>
        <label htmlFor="status-filter" className="mb-1.5 block text-sm font-medium text-foreground">
          Status
        </label>
        <select
          id="status-filter"
          value={statusFilter}
          onChange={(event) => handleStatusChange(event.target.value)}
          className="w-full max-w-xs rounded-md border border-muted bg-white px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
        >
          <option value="">Todos os status</option>
          {PROJECT_STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !projects || projects.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhuma obra encontrada.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">Cliente</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Responsável</th>
                  <th className="px-4 py-3">Prazo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {projects.content.map((project) => (
                  <tr
                    key={project.id}
                    role="link"
                    tabIndex={0}
                    onClick={() => goToProject(project.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        goToProject(project.id)
                      }
                    }}
                    className="cursor-pointer transition hover:bg-background focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary"
                  >
                    <td className="px-4 py-3 font-medium text-foreground">{project.name}</td>
                    <td className="px-4 py-3 text-gray-500">{project.client}</td>
                    <td className="px-4 py-3">
                      <StatusBadge label={projectStatusLabel(project.status)} tone={PROJECT_STATUS_TONE[project.status]} />
                    </td>
                    <td className="px-4 py-3 text-gray-500">{project.responsibleUser.name}</td>
                    <td className="px-4 py-3 text-gray-500">{formatDate(project.expectedDeadline)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {projects.content.map((project) => (
              <li key={project.id}>
                <Link
                  to={`/manager/projects/${project.id}/edit`}
                  className="block rounded-lg border border-muted bg-white p-4 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-foreground">{project.name}</span>
                    <StatusBadge label={projectStatusLabel(project.status)} tone={PROJECT_STATUS_TONE[project.status]} />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">{project.client}</p>
                  <div className="mt-2 flex justify-between text-xs text-gray-500">
                    <span>{project.responsibleUser.name}</span>
                    <span>Prazo: {formatDate(project.expectedDeadline)}</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>

          <Pagination page={projects.number} totalPages={projects.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
