import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { primaryButtonClass } from '../../components/buttonStyles'
import { formatDate } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getMaterialRequests } from '../../services/materialRequestService'
import { getProjects } from '../../services/projectService'
import { getWorkOrders } from '../../services/workOrderService'
import type { MaterialRequestResponse, MaterialRequestStatus, Page, ProjectSummaryResponse, WorkOrderSummaryResponse } from '../../types'
import {
  MATERIAL_REQUEST_STATUS_OPTIONS,
  MATERIAL_REQUEST_STATUS_TONE,
  MATERIAL_REQUEST_PRIORITY_TONE,
  materialRequestPriorityLabel,
  materialRequestStatusLabel,
} from './materialRequestOptions'

export function MaterialRequestListPage() {
  const navigate = useNavigate()
  const [materialRequests, setMaterialRequests] = useState<Page<MaterialRequestResponse> | null>(null)
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([])
  const [workOrders, setWorkOrders] = useState<WorkOrderSummaryResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [projectFilter, setProjectFilter] = useState('')
  const [workOrderFilter, setWorkOrderFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<MaterialRequestStatus | ''>('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    getProjects({ size: 100 })
      .then((data) => setProjects(data.content))
      .catch(() => setProjects([]))
    getWorkOrders({ size: 100 })
      .then((data) => setWorkOrders(data.content))
      .catch(() => setWorkOrders([]))
  }, [])

  useEffect(() => {
    let cancelled = false

    async function loadMaterialRequests() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getMaterialRequests({
          projectId: projectFilter ? Number(projectFilter) : undefined,
          workOrderId: workOrderFilter ? Number(workOrderFilter) : undefined,
          status: statusFilter || undefined,
          page,
        })
        if (!cancelled) {
          setMaterialRequests(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar os pedidos de materiais. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadMaterialRequests()

    return () => {
      cancelled = true
    }
  }, [projectFilter, workOrderFilter, statusFilter, page])

  function handleProjectChange(value: string) {
    setProjectFilter(value)
    setPage(0)
  }

  function handleWorkOrderChange(value: string) {
    setWorkOrderFilter(value)
    setPage(0)
  }

  function handleStatusChange(value: string) {
    setStatusFilter(value as MaterialRequestStatus | '')
    setPage(0)
  }

  function goToMaterialRequest(id: number) {
    navigate(`/manager/material-requests/${id}`)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-foreground">Pedidos de Materiais</h1>
        <Link to="/manager/material-requests/new" className={primaryButtonClass}>
          Novo Pedido
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
          <label htmlFor="work-order-filter" className="mb-1.5 block text-sm font-medium text-foreground">
            Ordem de Serviço
          </label>
          <select
            id="work-order-filter"
            value={workOrderFilter}
            onChange={(event) => handleWorkOrderChange(event.target.value)}
            className="w-full max-w-xs rounded-md border border-muted bg-white px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
          >
            <option value="">Todas as OS</option>
            {workOrders.map((workOrder) => (
              <option key={workOrder.id} value={workOrder.id}>
                OS Nº {workOrder.orderNumber} — {workOrder.stage}
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
            {MATERIAL_REQUEST_STATUS_OPTIONS.map((option) => (
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
      ) : !materialRequests || materialRequests.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhum pedido de materiais encontrado.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nº Pedido</th>
                  <th className="px-4 py-3">Obra</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Prioridade</th>
                  <th className="px-4 py-3">Necessário até</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {materialRequests.content.map((materialRequest) => (
                  <tr
                    key={materialRequest.id}
                    role="link"
                    tabIndex={0}
                    onClick={() => goToMaterialRequest(materialRequest.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        goToMaterialRequest(materialRequest.id)
                      }
                    }}
                    className="cursor-pointer transition hover:bg-background focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary"
                  >
                    <td className="px-4 py-3 font-medium text-foreground">{materialRequest.requestNumber}</td>
                    <td className="px-4 py-3 text-gray-500">{materialRequest.project.name}</td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        label={materialRequestStatusLabel(materialRequest.status)}
                        tone={MATERIAL_REQUEST_STATUS_TONE[materialRequest.status]}
                      />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        label={materialRequestPriorityLabel(materialRequest.priority)}
                        tone={MATERIAL_REQUEST_PRIORITY_TONE[materialRequest.priority]}
                      />
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {materialRequest.neededByDate ? formatDate(materialRequest.neededByDate) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {materialRequests.content.map((materialRequest) => (
              <li key={materialRequest.id}>
                <Link
                  to={`/manager/material-requests/${materialRequest.id}`}
                  className="block rounded-lg border border-muted bg-white p-4 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-foreground">Pedido Nº {materialRequest.requestNumber}</span>
                    <StatusBadge
                      label={materialRequestStatusLabel(materialRequest.status)}
                      tone={MATERIAL_REQUEST_STATUS_TONE[materialRequest.status]}
                    />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">{materialRequest.project.name}</p>
                  <div className="mt-2 flex items-center justify-between">
                    <StatusBadge
                      label={materialRequestPriorityLabel(materialRequest.priority)}
                      tone={MATERIAL_REQUEST_PRIORITY_TONE[materialRequest.priority]}
                    />
                    <span className="text-xs text-gray-500">
                      {materialRequest.neededByDate ? formatDate(materialRequest.neededByDate) : '—'}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>

          <Pagination
            page={materialRequests.number}
            totalPages={materialRequests.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
