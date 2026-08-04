import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { StatusBadge } from '../../components/StatusBadge'
import {
  cancelWorkOrder,
  completeWorkOrder,
  downloadWorkOrderPdf,
  duplicateWorkOrder,
  getWorkOrder,
  releaseWorkOrder,
  startWorkOrder,
} from '../../services/workOrderService'
import type { ErrorResponse, WorkOrderResponse } from '../../types'
import { WORK_ORDER_STATUS_TONE, workOrderStatusLabel } from './workOrderStatusOptions'

const primaryButtonClass =
  'rounded-md bg-accent px-4 py-2 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60'
const dangerButtonClass =
  'rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60'
const secondaryButtonClass =
  'rounded-md border border-muted px-4 py-2 text-sm font-medium text-foreground transition hover:bg-background disabled:cursor-not-allowed disabled:opacity-50'

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

function formatTime(isoTime: string | null): string | null {
  return isoTime ? isoTime.slice(0, 5) : null
}

interface DetailFieldProps {
  label: string
  children: ReactNode
}

function DetailField({ label, children }: DetailFieldProps) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-500">{label}</dt>
      <dd className="mt-0.5 text-sm text-foreground">{children || '—'}</dd>
    </div>
  )
}

export function WorkOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [workOrder, setWorkOrder] = useState<WorkOrderResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [isDuplicating, setIsDuplicating] = useState(false)
  const [isDownloading, setIsDownloading] = useState(false)

  useEffect(() => {
    loadWorkOrder()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadWorkOrder() {
    setIsLoading(true)
    setLoadError(null)

    try {
      setWorkOrder(await getWorkOrder(Number(id)))
    } catch {
      setLoadError('Não foi possível carregar a ordem de serviço. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  async function handleTransition(action: (id: number) => Promise<WorkOrderResponse>) {
    if (!workOrder) {
      return
    }

    setActionError(null)
    setIsTransitioning(true)

    try {
      setWorkOrder(await action(workOrder.id))
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setActionError(error.response.data.message)
      } else {
        setActionError('Não foi possível atualizar o status. Tente novamente.')
      }
    } finally {
      setIsTransitioning(false)
    }
  }

  function handleCancel() {
    if (window.confirm('Tem certeza que deseja cancelar esta Ordem de Serviço? Esta ação não pode ser desfeita.')) {
      handleTransition(cancelWorkOrder)
    }
  }

  async function handleDuplicate() {
    if (!workOrder) {
      return
    }

    setActionError(null)
    setIsDuplicating(true)

    try {
      const copy = await duplicateWorkOrder(workOrder.id)
      navigate(`/manager/work-orders/${copy.id}`)
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setActionError(error.response.data.message)
      } else {
        setActionError('Não foi possível duplicar a ordem de serviço. Tente novamente.')
      }
      setIsDuplicating(false)
    }
  }

  async function handleDownloadPdf() {
    if (!workOrder) {
      return
    }

    setActionError(null)
    setIsDownloading(true)

    try {
      await downloadWorkOrderPdf(workOrder.id, workOrder.orderNumber)
    } catch {
      setActionError('Não foi possível baixar o PDF. Tente novamente.')
    } finally {
      setIsDownloading(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError || !workOrder) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError ?? 'Ordem de serviço não encontrada.'}</p>
        <button type="button" onClick={loadWorkOrder} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  const isEditable = workOrder.status === 'DRAFT'

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">OS Nº {workOrder.orderNumber}</h1>
        <Link to="/manager/work-orders" className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <div className="rounded-lg border border-muted bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-muted pb-4">
          <StatusBadge label={workOrderStatusLabel(workOrder.status)} tone={WORK_ORDER_STATUS_TONE[workOrder.status]} />

          <div className="flex flex-wrap gap-2">
            {workOrder.status === 'DRAFT' && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => handleTransition(releaseWorkOrder)}
                  className={primaryButtonClass}
                >
                  Liberar
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}
            {workOrder.status === 'RELEASED' && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => handleTransition(startWorkOrder)}
                  className={primaryButtonClass}
                >
                  Iniciar
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}
            {workOrder.status === 'IN_PROGRESS' && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => handleTransition(completeWorkOrder)}
                  className={primaryButtonClass}
                >
                  Concluir
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}

            {isEditable ? (
              <Link to={`/manager/work-orders/${workOrder.id}/edit`} className={secondaryButtonClass}>
                Editar
              </Link>
            ) : (
              <button
                type="button"
                disabled
                title="Só é possível editar uma OS em rascunho"
                className={secondaryButtonClass}
              >
                Editar
              </button>
            )}
            <button type="button" disabled={isDuplicating} onClick={handleDuplicate} className={secondaryButtonClass}>
              {isDuplicating ? 'Duplicando...' : 'Duplicar'}
            </button>
            <button
              type="button"
              disabled={isDownloading}
              onClick={handleDownloadPdf}
              className={secondaryButtonClass}
            >
              {isDownloading ? 'Baixando...' : 'Baixar PDF'}
            </button>
          </div>
        </div>

        {actionError && (
          <div role="alert" className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
            {actionError}
          </div>
        )}

        <dl className="mt-4 grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
          <DetailField label="Obra">{workOrder.project.name}</DetailField>
          <DetailField label="Data">{formatDate(workOrder.date)}</DetailField>
          <DetailField label="Responsável">{workOrder.responsibleUser.name}</DetailField>
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
          <DetailField label="Observações">{workOrder.notes}</DetailField>
        </dl>

        <div className="mt-6 border-t border-muted pt-4">
          <dt className="text-xs font-medium text-gray-500">Equipe atribuída</dt>
          {workOrder.assignedWorkers.length === 0 ? (
            <p className="mt-1.5 text-sm text-gray-500">Nenhum colaborador atribuído.</p>
          ) : (
            <ul className="mt-1.5 flex flex-wrap gap-2">
              {workOrder.assignedWorkers.map((worker) => (
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
      </div>
    </div>
  )
}
