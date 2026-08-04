import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getProjects } from '../../services/projectService'
import { getWorkOrders } from '../../services/workOrderService'
import type { Page, ProjectSummaryResponse, WorkOrderResponse, WorkOrderStatus } from '../../types'
import { WORK_ORDER_STATUS_OPTIONS, WORK_ORDER_STATUS_TONE, workOrderStatusLabel } from './workOrderStatusOptions'

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export function WorkOrderListPage() {
  const navigate = useNavigate()
  const [workOrders, setWorkOrders] = useState<Page<WorkOrderResponse> | null>(null)
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [projectFilter, setProjectFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<WorkOrderStatus | ''>('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    getProjects({ size: 100 })
      .then((data) => setProjects(data.content))
      .catch(() => setProjects([]))
  }, [])

  useEffect(() => {
    let cancelled = false

    async function loadWorkOrders() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getWorkOrders({
          projectId: projectFilter ? Number(projectFilter) : undefined,
          status: statusFilter || undefined,
          page,
        })
        if (!cancelled) {
          setWorkOrders(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar as ordens de serviço. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadWorkOrders()

    return () => {
      cancelled = true
    }
  }, [projectFilter, statusFilter, page])

  function handleProjectChange(value: string) {
    setProjectFilter(value)
    setPage(0)
  }

  function handleStatusChange(value: string) {
    setStatusFilter(value as WorkOrderStatus | '')
    setPage(0)
  }

  function goToWorkOrder(id: number) {
    navigate(`/manager/work-orders/${id}`)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-foreground">Ordens de Serviço</h1>
        <Link
          to="/manager/work-orders/new"
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
        >
          Nova Ordem de Serviço
        </Link>
      </div>

      <div className="flex flex-wrap gap-4">
        <div>
          <label htmlFor="project-filter" className="mb-1.5 block text-sm font-medium text-foreground">
            Obra
          </label>
          <select
            id="project-filter"
            value={projectFilter}
            onChange={(event) => handleProjectChange(event.target.value)}
            className="w-full max-w-xs rounded-md border border-muted bg-white px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
          >
            <option value="">Todas as obras</option>
            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
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
            {WORK_ORDER_STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !workOrders || workOrders.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhuma ordem de serviço encontrada.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nº OS</th>
                  <th className="px-4 py-3">Obra</th>
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Responsável</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {workOrders.content.map((workOrder) => (
                  <tr
                    key={workOrder.id}
                    role="link"
                    tabIndex={0}
                    onClick={() => goToWorkOrder(workOrder.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        goToWorkOrder(workOrder.id)
                      }
                    }}
                    className="cursor-pointer transition hover:bg-background focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary"
                  >
                    <td className="px-4 py-3 font-medium text-foreground">{workOrder.orderNumber}</td>
                    <td className="px-4 py-3 text-gray-500">{workOrder.project.name}</td>
                    <td className="px-4 py-3 text-gray-500">{formatDate(workOrder.date)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        label={workOrderStatusLabel(workOrder.status)}
                        tone={WORK_ORDER_STATUS_TONE[workOrder.status]}
                      />
                    </td>
                    <td className="px-4 py-3 text-gray-500">{workOrder.responsibleUser.name}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {workOrders.content.map((workOrder) => (
              <li key={workOrder.id}>
                <Link
                  to={`/manager/work-orders/${workOrder.id}`}
                  className="block rounded-lg border border-muted bg-white p-4 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-foreground">OS Nº {workOrder.orderNumber}</span>
                    <StatusBadge
                      label={workOrderStatusLabel(workOrder.status)}
                      tone={WORK_ORDER_STATUS_TONE[workOrder.status]}
                    />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">{workOrder.project.name}</p>
                  <div className="mt-2 flex justify-between text-xs text-gray-500">
                    <span>{workOrder.responsibleUser.name}</span>
                    <span>{formatDate(workOrder.date)}</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>

          <Pagination page={workOrders.number} totalPages={workOrders.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
