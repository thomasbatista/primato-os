import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { dangerButtonClass, primaryButtonClass, secondaryButtonClass } from '../../components/buttonStyles'
import { DetailField } from '../../components/DetailField'
import { formatDate } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { StatusBadge } from '../../components/StatusBadge'
import {
  approveMaterialRequest,
  cancelMaterialRequest,
  downloadMaterialRequestPdf,
  duplicateMaterialRequest,
  getMaterialRequest,
  markMaterialRequestPurchased,
  registerMaterialRequestDelivery,
  submitMaterialRequest,
} from '../../services/materialRequestService'
import type { ErrorResponse, MaterialRequestResponse } from '../../types'
import {
  MATERIAL_REQUEST_PRIORITY_TONE,
  MATERIAL_REQUEST_STATUS_TONE,
  materialRequestPriorityLabel,
  materialRequestStatusLabel,
  materialRequestUnitLabel,
} from './materialRequestOptions'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-background disabled:text-gray-500'

export function MaterialRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [materialRequest, setMaterialRequest] = useState<MaterialRequestResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [isDuplicating, setIsDuplicating] = useState(false)
  const [isDownloading, setIsDownloading] = useState(false)
  const [isDeliveryModalOpen, setIsDeliveryModalOpen] = useState(false)

  useEffect(() => {
    loadMaterialRequest()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadMaterialRequest() {
    setIsLoading(true)
    setLoadError(null)

    try {
      setMaterialRequest(await getMaterialRequest(Number(id)))
    } catch {
      setLoadError('Não foi possível carregar o pedido de materiais. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  function extractErrorMessage(error: unknown, fallback: string): string {
    if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
      return error.response.data.message
    }
    return fallback
  }

  async function handleTransition(action: (id: number) => Promise<MaterialRequestResponse>) {
    if (!materialRequest) {
      return
    }

    setActionError(null)
    setIsTransitioning(true)

    try {
      setMaterialRequest(await action(materialRequest.id))
    } catch (error) {
      setActionError(extractErrorMessage(error, 'Não foi possível atualizar o status. Tente novamente.'))
    } finally {
      setIsTransitioning(false)
    }
  }

  function handleCancel() {
    if (window.confirm('Tem certeza que deseja cancelar este pedido de materiais? Esta ação não pode ser desfeita.')) {
      handleTransition(cancelMaterialRequest)
    }
  }

  async function handleDuplicate() {
    if (!materialRequest) {
      return
    }

    setActionError(null)
    setIsDuplicating(true)

    try {
      const copy = await duplicateMaterialRequest(materialRequest.id)
      navigate(`/manager/material-requests/${copy.id}`)
    } catch (error) {
      setActionError(extractErrorMessage(error, 'Não foi possível duplicar o pedido de materiais. Tente novamente.'))
      setIsDuplicating(false)
    }
  }

  async function handleDownloadPdf() {
    if (!materialRequest) {
      return
    }

    setActionError(null)
    setIsDownloading(true)

    try {
      await downloadMaterialRequestPdf(materialRequest.id, materialRequest.requestNumber)
    } catch {
      setActionError('Não foi possível baixar o PDF. Tente novamente.')
    } finally {
      setIsDownloading(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError || !materialRequest) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError ?? 'Pedido de materiais não encontrado.'}</p>
        <button type="button" onClick={loadMaterialRequest} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  const isEditable = materialRequest.status === 'DRAFT'
  const canRegisterDelivery = materialRequest.status === 'PURCHASED' || materialRequest.status === 'PARTIALLY_DELIVERED'

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">Pedido Nº {materialRequest.requestNumber}</h1>
        <Link to="/manager/material-requests" className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <div className="rounded-lg border border-muted bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-muted pb-4">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge
              label={materialRequestStatusLabel(materialRequest.status)}
              tone={MATERIAL_REQUEST_STATUS_TONE[materialRequest.status]}
            />
            <StatusBadge
              label={materialRequestPriorityLabel(materialRequest.priority)}
              tone={MATERIAL_REQUEST_PRIORITY_TONE[materialRequest.priority]}
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {materialRequest.status === 'DRAFT' && (
              <button
                type="button"
                disabled={isTransitioning}
                onClick={() => handleTransition(submitMaterialRequest)}
                className={primaryButtonClass}
              >
                Enviar
              </button>
            )}
            {materialRequest.status === 'REQUESTED' && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => handleTransition(approveMaterialRequest)}
                  className={primaryButtonClass}
                >
                  Aprovar
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}
            {materialRequest.status === 'APPROVED' && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => handleTransition(markMaterialRequestPurchased)}
                  className={primaryButtonClass}
                >
                  Marcar como Comprado
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}
            {canRegisterDelivery && (
              <>
                <button
                  type="button"
                  disabled={isTransitioning}
                  onClick={() => setIsDeliveryModalOpen(true)}
                  className={primaryButtonClass}
                >
                  Registrar Entrega
                </button>
                <button type="button" disabled={isTransitioning} onClick={handleCancel} className={dangerButtonClass}>
                  Cancelar
                </button>
              </>
            )}

            {isEditable ? (
              <Link to={`/manager/material-requests/${materialRequest.id}/edit`} className={secondaryButtonClass}>
                Editar
              </Link>
            ) : (
              <button
                type="button"
                disabled
                title="Só é possível editar um pedido em rascunho"
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
          <DetailField label="Obra">{materialRequest.project.name}</DetailField>
          <DetailField label="Ordem de Serviço">
            {materialRequest.workOrder ? `OS Nº ${materialRequest.workOrder.orderNumber}` : null}
          </DetailField>
          <DetailField label="Data do pedido">{formatDate(materialRequest.requestDate)}</DetailField>
          <DetailField label="Necessário até">
            {materialRequest.neededByDate ? formatDate(materialRequest.neededByDate) : null}
          </DetailField>
          <DetailField label="Solicitante">{materialRequest.requester.name}</DetailField>
          <DetailField label="Local de entrega">{materialRequest.deliveryLocation}</DetailField>
        </dl>

        <dl className="mt-6 space-y-4 border-t border-muted pt-4">
          <DetailField label="Justificativa">{materialRequest.justification}</DetailField>
          <DetailField label="Observações">{materialRequest.notes}</DetailField>
        </dl>

        <div className="mt-6 border-t border-muted pt-4">
          <dt className="mb-2 text-xs font-medium text-gray-500">Itens</dt>
          <div className="overflow-hidden overflow-x-auto rounded-lg border border-muted">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-muted bg-background text-xs font-medium text-gray-500">
                <tr>
                  <th className="px-4 py-3">Material</th>
                  <th className="px-4 py-3">Quantidade</th>
                  <th className="px-4 py-3">Unidade</th>
                  <th className="px-4 py-3">Entregue</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-muted/40">
                {materialRequest.items.map((item) => (
                  <tr key={item.id}>
                    <td className="px-4 py-3 font-medium text-foreground">{item.name}</td>
                    <td className="px-4 py-3 text-gray-500">{item.quantity}</td>
                    <td className="px-4 py-3 text-gray-500">{materialRequestUnitLabel(item.unit)}</td>
                    <td className="px-4 py-3 text-gray-500">{item.quantityDelivered}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {isDeliveryModalOpen && (
        <DeliveryRegistrationModal
          materialRequest={materialRequest}
          onClose={() => setIsDeliveryModalOpen(false)}
          onRegistered={(updated) => {
            setMaterialRequest(updated)
            setIsDeliveryModalOpen(false)
          }}
        />
      )}
    </div>
  )
}

interface DeliveryRegistrationModalProps {
  materialRequest: MaterialRequestResponse
  onClose: () => void
  onRegistered: (updated: MaterialRequestResponse) => void
}

function DeliveryRegistrationModal({ materialRequest, onClose, onRegistered }: DeliveryRegistrationModalProps) {
  const [quantities, setQuantities] = useState<Record<number, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const deliverableItems = materialRequest.items.map((item) => ({
    ...item,
    remaining: item.quantity - item.quantityDelivered,
  }))

  function updateQuantity(itemId: number, value: string) {
    setQuantities((prev) => ({ ...prev, [itemId]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    const entries = deliverableItems
      .map((item) => ({ item, value: quantities[item.id]?.trim() }))
      .filter((entry) => entry.value)

    if (entries.length === 0) {
      setSubmitError('Informe a quantidade entregue de ao menos um item.')
      return
    }

    for (const { item, value } of entries) {
      const quantity = Number(value)
      if (!(quantity > 0)) {
        setSubmitError(`Quantidade inválida para ${item.name}.`)
        return
      }
      if (quantity > item.remaining) {
        setSubmitError(`Quantidade entregue de ${item.name} não pode exceder o restante (${item.remaining}).`)
        return
      }
    }

    setIsSubmitting(true)

    try {
      const updated = await registerMaterialRequestDelivery(materialRequest.id, {
        items: entries.map(({ item, value }) => ({
          materialRequestItemId: item.id,
          quantityDelivered: Number(value),
        })),
      })
      onRegistered(updated)
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível registrar a entrega. Tente novamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    >
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-lg font-semibold text-foreground">Registrar Entrega</h2>
        <p className="mt-1 text-sm text-gray-500">
          Informe a quantidade recebida agora para cada item. O restante já entregue não pode ser alterado.
        </p>

        <form onSubmit={handleSubmit} noValidate className="mt-4 space-y-4">
          {submitError && (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
              {submitError}
            </div>
          )}

          <div className="space-y-3">
            {deliverableItems.map((item) => (
              <div key={item.id} className="rounded-md border border-muted p-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-medium text-foreground">{item.name}</span>
                  <span className="text-xs text-gray-500">
                    {item.quantityDelivered} / {item.quantity} {materialRequestUnitLabel(item.unit)}
                  </span>
                </div>
                {item.remaining <= 0 ? (
                  <p className="mt-1.5 text-xs text-gray-500">Item já totalmente entregue.</p>
                ) : (
                  <label className="mt-1.5 block">
                    <span className="mb-1 block text-xs text-gray-500">
                      Entregar agora (restam {item.remaining} {materialRequestUnitLabel(item.unit)})
                    </span>
                    <input
                      type="number"
                      min="0"
                      max={item.remaining}
                      step="0.01"
                      value={quantities[item.id] ?? ''}
                      onChange={(event) => updateQuantity(item.id, event.target.value)}
                      className={inputClass}
                    />
                  </label>
                )}
              </div>
            ))}
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button type="submit" disabled={isSubmitting} className={primaryButtonClass}>
              {isSubmitting ? 'Registrando...' : 'Confirmar Entrega'}
            </button>
            <button type="button" onClick={onClose} className={secondaryButtonClass}>
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
