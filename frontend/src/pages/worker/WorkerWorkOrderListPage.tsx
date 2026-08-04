import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatDate } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getMyWorkOrders } from '../../services/workOrderService'
import type { Page, WorkOrderResponse } from '../../types'
import { WORK_ORDER_STATUS_TONE, workOrderStatusLabel } from '../manager/workOrderStatusOptions'

export function WorkerWorkOrderListPage() {
  const [workOrders, setWorkOrders] = useState<Page<WorkOrderResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadWorkOrders() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getMyWorkOrders(page)
        if (!cancelled) {
          setWorkOrders(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar suas ordens de serviço. Tente novamente.')
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
  }, [page])

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-foreground">Minhas Ordens de Serviço</h1>

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !workOrders || workOrders.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhuma ordem de serviço atribuída a você.
        </div>
      ) : (
        <>
          <ul className="space-y-3">
            {workOrders.content.map((workOrder) => (
              <li key={workOrder.id}>
                <Link
                  to={`/worker/work-orders/${workOrder.id}`}
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
                    <span>{workOrder.stage}</span>
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
