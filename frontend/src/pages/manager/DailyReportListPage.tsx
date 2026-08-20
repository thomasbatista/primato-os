import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fillerName, isFilledByManager } from '../../components/dailyReportFiller'
import { DAILY_REPORT_STATUS_TONE, dailyReportStatusLabel } from '../../components/dailyReportStatusOptions'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { getDailyReportsByWorkOrder } from '../../services/dailyReportService'
import { getWorkOrders } from '../../services/workOrderService'
import type { DailyReportResponse, Page, WorkOrderSummaryResponse } from '../../types'

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export function DailyReportListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const workOrderIdParam = searchParams.get('workOrderId')

  const [workOrders, setWorkOrders] = useState<WorkOrderSummaryResponse[]>([])
  const [reports, setReports] = useState<Page<DailyReportResponse> | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    getWorkOrders({ size: 100 })
      .then((data) => setWorkOrders(data.content))
      .catch(() => setWorkOrders([]))
  }, [])

  useEffect(() => {
    if (!workOrderIdParam) {
      return
    }

    let cancelled = false

    async function loadReports() {
      setIsLoading(true)
      setError(null)

      try {
        const data = await getDailyReportsByWorkOrder({ workOrderId: Number(workOrderIdParam), page })
        if (!cancelled) {
          setReports(data)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar os checklists diários. Tente novamente.')
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
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-foreground">Checklists Diários</h1>

      <div>
        <label htmlFor="work-order-filter" className="mb-1.5 block text-sm font-medium text-foreground">
          Ordem de Serviço
        </label>
        <select
          id="work-order-filter"
          value={workOrderIdParam ?? ''}
          onChange={(event) => handleWorkOrderChange(event.target.value)}
          className="w-full max-w-xs rounded-md border border-muted bg-white px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none"
        >
          <option value="">Selecione uma OS</option>
          {workOrders.map((workOrder) => (
            <option key={workOrder.id} value={workOrder.id}>
              OS Nº {workOrder.orderNumber} — {workOrder.stage}
            </option>
          ))}
        </select>
      </div>

      {!workOrderIdParam ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Selecione uma ordem de serviço para ver os checklists diários.
        </div>
      ) : isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : !reports || reports.content.length === 0 ? (
        <div className="rounded-lg border border-muted bg-white p-8 text-center text-sm text-gray-500">
          Nenhum checklist diário encontrado para esta OS.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-muted bg-white sm:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3">OS</th>
                  <th className="px-4 py-3">Preenchido por</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {reports.content.map((report) => (
                  <tr key={report.id} className="transition hover:bg-background">
                    <td className="px-4 py-3 font-medium text-foreground">
                      <Link to={`/manager/daily-reports/${report.id}`} className="block">
                        {formatDate(report.date)}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-gray-500">OS Nº {report.workOrder.orderNumber}</td>
                    <td className="px-4 py-3 text-gray-500">
                      <span className="inline-flex items-center gap-2">
                        {fillerName(report)}
                        {isFilledByManager(report) && <StatusBadge label="Gestor" tone="info" />}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge label={dailyReportStatusLabel(report.status)} tone={DAILY_REPORT_STATUS_TONE[report.status]} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {reports.content.map((report) => (
              <li key={report.id}>
                <Link
                  to={`/manager/daily-reports/${report.id}`}
                  className="block rounded-lg border border-muted bg-white p-4 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-foreground">{formatDate(report.date)}</span>
                    <StatusBadge label={dailyReportStatusLabel(report.status)} tone={DAILY_REPORT_STATUS_TONE[report.status]} />
                  </div>
                  <p className="mt-1 text-sm text-gray-500">OS Nº {report.workOrder.orderNumber}</p>
                  <p className="mt-2 text-xs text-gray-500">
                    {fillerName(report)}
                    {isFilledByManager(report) && ' (Gestor)'}
                  </p>
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
