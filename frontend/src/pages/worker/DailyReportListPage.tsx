import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { DAILY_REPORT_STATUS_TONE, dailyReportStatusLabel } from '../../components/dailyReportStatusOptions'
import { formatDate } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getMyDailyReports } from '../../services/dailyReportService'
import { getMyWorkOrders } from '../../services/workOrderService'
import type { DailyReportResponse, Page, WorkOrderSummaryResponse } from '../../types'

export function DailyReportListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const workOrderIdParam = searchParams.get('workOrderId')

  const [workOrders, setWorkOrders] = useState<WorkOrderSummaryResponse[]>([])
  const [reports, setReports] = useState<Page<DailyReportResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    getMyWorkOrders(0, 100)
      .then((data) => setWorkOrders(data.content))
      .catch(() => setWorkOrders([]))
  }, [])

  useEffect(() => {
    let cancelled = false

    async function loadReports() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getMyDailyReports({
          workOrderId: workOrderIdParam ? Number(workOrderIdParam) : undefined,
          page,
        })
        if (!cancelled) {
          setReports(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar seus checklists diários. Tente novamente.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadReports()

    return () => {
      cancelled = true
    }
  }, [workOrderIdParam, page])

  function handleWorkOrderChange(value: string) {
    setPage(0)
    setSearchParams(value ? { workOrderId: value } : {})
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-foreground">Meus Checklists Diários</h1>

      <div>
        <label htmlFor="work-order-filter" className="mb-1.5 block text-sm font-medium text-foreground">
          Ordem de Serviço
        </label>
        <select
          id="work-order-filter"
          value={workOrderIdParam ?? ''}
          onChange={(event) => handleWorkOrderChange(event.target.value)}
          className="w-full rounded-md border border-muted bg-white px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
        >
          <option value="">Todas as OS</option>
          {workOrders.map((workOrder) => (
            <option key={workOrder.id} value={workOrder.id}>
              OS Nº {workOrder.orderNumber} — {workOrder.stage}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !reports || reports.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhum checklist diário encontrado.
        </div>
      ) : (
        <>
          <ul className="space-y-3">
            {reports.content.map((report) => (
              <li key={report.id}>
                <Link
                  to={`/worker/daily-reports/${report.id}/edit`}
                  className="block rounded-lg border border-muted bg-white p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-foreground">{formatDate(report.date)}</span>
                    <StatusBadge
                      label={dailyReportStatusLabel(report.status)}
                      tone={DAILY_REPORT_STATUS_TONE[report.status]}
                    />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">OS Nº {report.workOrder.orderNumber}</p>
                </Link>
              </li>
            ))}
          </ul>

          <Pagination page={reports.number} totalPages={reports.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
