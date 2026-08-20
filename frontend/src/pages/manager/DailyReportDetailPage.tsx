import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import { primaryButtonClass } from '../../components/buttonStyles'
import {
  DAILY_REPORT_ITEM_STATUS_TONE,
  DAILY_REPORT_STATUS_TONE,
  dailyReportItemStatusLabel,
  dailyReportStatusLabel,
} from '../../components/dailyReportStatusOptions'
import { DetailField } from '../../components/DetailField'
import { fillerName, isFilledByManager } from '../../components/dailyReportFiller'
import { formatDate, formatTime } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { PhotoGrid } from '../../components/PhotoGrid'
import { StatusBadge } from '../../components/StatusBadge'
import { getDailyReport, reopenDailyReport } from '../../services/dailyReportService'
import type { DailyReportResponse, ErrorResponse } from '../../types'

export function DailyReportDetailPage() {
  const { id } = useParams<{ id: string }>()

  const [report, setReport] = useState<DailyReportResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isReopening, setIsReopening] = useState(false)

  useEffect(() => {
    loadReport()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadReport() {
    setIsLoading(true)
    setLoadError(null)

    try {
      setReport(await getDailyReport(Number(id)))
    } catch {
      setLoadError('Não foi possível carregar o checklist diário. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  async function handleReopen() {
    if (!report) {
      return
    }

    const confirmed = window.confirm(
      'Tem certeza que deseja reabrir este Checklist Diário? O colaborador poderá editá-lo novamente.',
    )
    if (!confirmed) {
      return
    }

    setActionError(null)
    setIsReopening(true)

    try {
      setReport(await reopenDailyReport(report.id))
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setActionError(error.response.data.message)
      } else {
        setActionError('Não foi possível reabrir o checklist. Tente novamente.')
      }
    } finally {
      setIsReopening(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError || !report) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError ?? 'Checklist diário não encontrado.'}</p>
        <button type="button" onClick={loadReport} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">
          Checklist Diário — {formatDate(report.date)}
        </h1>
        <Link to="/manager/daily-reports" className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <div className="rounded-lg border border-muted bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-muted pb-4">
          <StatusBadge label={dailyReportStatusLabel(report.status)} tone={DAILY_REPORT_STATUS_TONE[report.status]} />

          {report.status === 'FINALIZED' ? (
            <button type="button" disabled={isReopening} onClick={handleReopen} className={primaryButtonClass}>
              {isReopening ? 'Reabrindo...' : 'Reabrir'}
            </button>
          ) : (
            <Link to={`/manager/daily-reports/${report.id}/edit`} className={primaryButtonClass}>
              Editar
            </Link>
          )}
        </div>

        {actionError && (
          <div role="alert" className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
            {actionError}
          </div>
        )}

        <dl className="mt-4 grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
          <DetailField label="Ordem de Serviço">
            <Link to={`/manager/work-orders/${report.workOrder.id}`} className="text-accent-dark hover:underline">
              OS Nº {report.workOrder.orderNumber} — {report.workOrder.stage}
            </Link>
          </DetailField>
          <DetailField label="Preenchido por">
            <span className="inline-flex items-center gap-2">
              {fillerName(report)}
              {isFilledByManager(report) && <StatusBadge label="Gestor" tone="info" />}
            </span>
          </DetailField>
          <DetailField label="Horário">
            {report.startTime || report.endTime
              ? `${formatTime(report.startTime) ?? '?'} às ${formatTime(report.endTime) ?? '?'}`
              : null}
          </DetailField>
          <DetailField label="Condição climática">{report.weatherCondition}</DetailField>
        </dl>

        <div className="mt-6 border-t border-muted pt-4">
          <dt className="text-xs font-medium text-gray-500">Equipe presente</dt>
          {report.teamPresent.length === 0 ? (
            <p className="mt-1.5 text-sm text-gray-500">Nenhum colaborador informado.</p>
          ) : (
            <ul className="mt-1.5 flex flex-wrap gap-2">
              {report.teamPresent.map((worker) => (
                <li
                  key={worker.id}
                  className="rounded-full border border-muted bg-background px-3 py-1 text-sm text-foreground"
                >
                  {worker.name}
                  {worker.function && <span className="text-gray-500"> — {worker.function}</span>}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="mt-6 border-t border-muted pt-4">
          <dt className="mb-2 text-xs font-medium text-gray-500">Itens do checklist</dt>
          {report.items.length === 0 ? (
            <p className="text-sm text-gray-500">Nenhum item registrado.</p>
          ) : (
            <ul className="space-y-3">
              {report.items.map((item) => (
                <li key={item.id} className="rounded-md border border-muted p-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-sm font-medium text-foreground">{item.activityDescription}</span>
                    <StatusBadge
                      label={dailyReportItemStatusLabel(item.status)}
                      tone={DAILY_REPORT_ITEM_STATUS_TONE[item.status]}
                    />
                  </div>
                  {(item.reason || item.observation || item.newExpectedDate) && (
                    <dl className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3">
                      <DetailField label="Motivo">{item.reason}</DetailField>
                      <DetailField label="Observação">{item.observation}</DetailField>
                      <DetailField label="Nova data prevista">
                        {item.newExpectedDate ? formatDate(item.newExpectedDate) : null}
                      </DetailField>
                    </dl>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <dl className="mt-6 space-y-4 border-t border-muted pt-4">
          <DetailField label="Serviços extras executados">{report.extraServicesExecuted}</DetailField>
          <DetailField label="Problemas encontrados">{report.problemsFound}</DetailField>
          <DetailField label="Pendências geradas">{report.pendingIssuesGenerated}</DetailField>
          <DetailField label="Materiais utilizados">{report.materialsUsed}</DetailField>
          <DetailField label="Materiais em falta">{report.materialsMissing}</DetailField>
          <DetailField label="Previsão para o próximo dia">{report.forecastForNextDay}</DetailField>
          <DetailField label="Observações">{report.notes}</DetailField>
        </dl>

        <div className="mt-6 border-t border-muted pt-4">
          <dt className="mb-2 text-xs font-medium text-gray-500">Fotos</dt>
          <PhotoGrid photos={report.photos} />
        </div>
      </div>
    </div>
  )
}
