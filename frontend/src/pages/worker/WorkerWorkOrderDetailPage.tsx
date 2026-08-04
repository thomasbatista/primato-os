import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { primaryButtonClass } from '../../components/buttonStyles'
import {
  DAILY_REPORT_STATUS_TONE,
  dailyReportStatusLabel,
} from '../../components/dailyReportStatusOptions'
import { DetailField } from '../../components/DetailField'
import { formatDate, formatTime } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { StatusBadge } from '../../components/StatusBadge'
import { getMyDailyReports } from '../../services/dailyReportService'
import { getMyWorkOrder } from '../../services/workOrderService'
import type { DailyReportResponse, WorkOrderResponse } from '../../types'
import { WORK_ORDER_STATUS_TONE, workOrderStatusLabel } from '../manager/workOrderStatusOptions'

const REPORTABLE_STATUSES = ['RELEASED', 'IN_PROGRESS']

export function WorkerWorkOrderDetailPage() {
  const { id } = useParams<{ id: string }>()

  const [workOrder, setWorkOrder] = useState<WorkOrderResponse | null>(null)
  const [reports, setReports] = useState<DailyReportResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadData() {
    setIsLoading(true)
    setLoadError(null)

    try {
      const [workOrderData, reportsPage] = await Promise.all([
        getMyWorkOrder(Number(id)),
        getMyDailyReports({ workOrderId: Number(id), size: 50 }),
      ])
      setWorkOrder(workOrderData)
      setReports(reportsPage.content)
    } catch {
      setLoadError('Não foi possível carregar a ordem de serviço. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError || !workOrder) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError ?? 'Ordem de serviço não encontrada.'}</p>
        <button type="button" onClick={loadData} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  const canReport = REPORTABLE_STATUSES.includes(workOrder.status)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">OS Nº {workOrder.orderNumber}</h1>
        <Link to="/worker" className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <div className="rounded-lg border border-muted bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-muted pb-4">
          <StatusBadge label={workOrderStatusLabel(workOrder.status)} tone={WORK_ORDER_STATUS_TONE[workOrder.status]} />

          {canReport ? (
            <Link to={`/worker/daily-reports/new/${workOrder.id}`} className={primaryButtonClass}>
              Preencher RDO
            </Link>
          ) : (
            <button
              type="button"
              disabled
              title="Só é possível preencher o Checklist Diário de uma OS liberada ou em andamento"
              className={primaryButtonClass}
            >
              Preencher RDO
            </button>
          )}
        </div>

        <dl className="mt-4 grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
          <DetailField label="Obra">{workOrder.project.name}</DetailField>
          <DetailField label="Data">{formatDate(workOrder.date)}</DetailField>
          <DetailField label="Etapa">{workOrder.stage}</DetailField>
          <DetailField label="Local">{workOrder.location}</DetailField>
          <DetailField label="Horário planejado">
            {workOrder.plannedStartTime || workOrder.plannedEndTime
              ? `${formatTime(workOrder.plannedStartTime) ?? '?'} às ${formatTime(workOrder.plannedEndTime) ?? '?'}`
              : null}
          </DetailField>
        </dl>

        <dl className="mt-6 space-y-4 border-t border-muted pt-4">
          <DetailField label="Descrição">{workOrder.description}</DetailField>
          <DetailField label="Meta diária">{workOrder.dailyGoal}</DetailField>
          <DetailField label="Materiais necessários">{workOrder.materialsNeeded}</DetailField>
          <DetailField label="Ferramentas">{workOrder.tools}</DetailField>
          <DetailField label="Diretrizes de segurança">{workOrder.safetyGuidelines}</DetailField>
          <DetailField label="Critérios de qualidade">{workOrder.qualityCriteria}</DetailField>
        </dl>
      </div>

      <div>
        <h2 className="mb-2 text-sm font-medium text-foreground">Meus checklists diários nesta OS</h2>
        {reports.length === 0 ? (
          <div className="rounded-lg border border-muted bg-white p-6 text-center text-sm text-gray-500">
            Você ainda não preencheu nenhum checklist para esta OS.
          </div>
        ) : (
          <ul className="space-y-3">
            {reports.map((report) => (
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
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
