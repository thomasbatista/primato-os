import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { dangerButtonClass, primaryButtonClass, secondaryButtonClass } from '../../components/buttonStyles'
import {
  DAILY_REPORT_ITEM_STATUS_OPTIONS,
  itemRequiresDetails,
} from '../../components/dailyReportStatusOptions'
import { FormField } from '../../components/FormField'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { PhotoGrid } from '../../components/PhotoGrid'
import {
  createDailyReport,
  deleteDailyReportPhoto,
  finalizeDailyReport,
  getDailyReport,
  updateDailyReport,
  uploadDailyReportPhoto,
} from '../../services/dailyReportService'
import { getMyWorkOrder } from '../../services/workOrderService'
import type {
  DailyReportCreateRequest,
  DailyReportItemStatus,
  DailyReportPhotoResponse,
  DailyReportResponse,
  ErrorResponse,
  WorkerSummaryResponse,
  WorkOrderResponse,
} from '../../types'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-background disabled:text-gray-500'

interface ItemFormState {
  activityDescription: string
  status: DailyReportItemStatus | ''
  reason: string
  observation: string
  newExpectedDate: string
}

interface ItemFieldErrors {
  activityDescription?: string
  status?: string
  reason?: string
  observation?: string
  newExpectedDate?: string
}

interface FormState {
  date: string
  teamPresentWorkerIds: number[]
  startTime: string
  endTime: string
  weatherCondition: string
  extraServicesExecuted: string
  problemsFound: string
  pendingIssuesGenerated: string
  materialsUsed: string
  materialsMissing: string
  forecastForNextDay: string
  notes: string
  items: ItemFormState[]
}

interface FieldErrors {
  date?: string
}

const EMPTY_ITEM: ItemFormState = {
  activityDescription: '',
  status: '',
  reason: '',
  observation: '',
  newExpectedDate: '',
}

interface UploadingFile {
  id: string
  fileName: string
  progress: number
  error: string | null
}

function today(): string {
  return new Date().toLocaleDateString('en-CA') // YYYY-MM-DD in local time, no UTC-shift risk
}

export function DailyReportFormPage() {
  const params = useParams<{ workOrderId?: string; id?: string }>()
  const isEditMode = params.id !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>({
    date: today(),
    teamPresentWorkerIds: [],
    startTime: '',
    endTime: '',
    weatherCondition: '',
    extraServicesExecuted: '',
    problemsFound: '',
    pendingIssuesGenerated: '',
    materialsUsed: '',
    materialsMissing: '',
    forecastForNextDay: '',
    notes: '',
    items: [EMPTY_ITEM],
  })
  const [workOrder, setWorkOrder] = useState<WorkOrderResponse | null>(null)
  const [report, setReport] = useState<DailyReportResponse | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [itemErrors, setItemErrors] = useState<(ItemFieldErrors | undefined)[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isFinalizing, setIsFinalizing] = useState(false)
  const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([])
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id, params.workOrderId])

  async function loadData() {
    setIsLoading(true)
    setLoadError(null)

    try {
      if (isEditMode) {
        const reportData = await getDailyReport(Number(params.id))
        const workOrderData = await getMyWorkOrder(reportData.workOrder.id)
        setReport(reportData)
        setWorkOrder(workOrderData)
        setForm({
          date: reportData.date,
          teamPresentWorkerIds: reportData.teamPresent.map((w) => w.id),
          startTime: reportData.startTime ?? '',
          endTime: reportData.endTime ?? '',
          weatherCondition: reportData.weatherCondition ?? '',
          extraServicesExecuted: reportData.extraServicesExecuted ?? '',
          problemsFound: reportData.problemsFound ?? '',
          pendingIssuesGenerated: reportData.pendingIssuesGenerated ?? '',
          materialsUsed: reportData.materialsUsed ?? '',
          materialsMissing: reportData.materialsMissing ?? '',
          forecastForNextDay: reportData.forecastForNextDay ?? '',
          notes: reportData.notes ?? '',
          items:
            reportData.items.length > 0
              ? reportData.items.map((item) => ({
                  activityDescription: item.activityDescription,
                  status: item.status,
                  reason: item.reason ?? '',
                  observation: item.observation ?? '',
                  newExpectedDate: item.newExpectedDate ?? '',
                }))
              : [EMPTY_ITEM],
        })
      } else {
        const workOrderData = await getMyWorkOrder(Number(params.workOrderId))
        setWorkOrder(workOrderData)
      }
    } catch {
      setLoadError('Não foi possível carregar os dados. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  const isReadOnly = isEditMode && report?.status === 'FINALIZED'

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [field]: value }))
    if (field === 'date') {
      setFieldErrors((prev) => ({ ...prev, date: undefined }))
    }
  }

  function toggleTeamMember(workerId: number) {
    setForm((prev) => ({
      ...prev,
      teamPresentWorkerIds: prev.teamPresentWorkerIds.includes(workerId)
        ? prev.teamPresentWorkerIds.filter((id) => id !== workerId)
        : [...prev.teamPresentWorkerIds, workerId],
    }))
  }

  function updateItem<K extends keyof ItemFormState>(index: number, field: K, value: ItemFormState[K]) {
    setForm((prev) => ({
      ...prev,
      items: prev.items.map((item, i) => (i === index ? { ...item, [field]: value } : item)),
    }))
    setItemErrors((prev) => prev.map((err, i) => (i === index ? undefined : err)))
  }

  function addItem() {
    setForm((prev) => ({ ...prev, items: [...prev.items, { ...EMPTY_ITEM }] }))
  }

  function removeItem(index: number) {
    setForm((prev) => ({ ...prev, items: prev.items.filter((_, i) => i !== index) }))
    setItemErrors((prev) => prev.filter((_, i) => i !== index))
  }

  function validate(): boolean {
    const errors: FieldErrors = {}
    if (!form.date) {
      errors.date = 'Data é obrigatória'
    }

    const newItemErrors: (ItemFieldErrors | undefined)[] = form.items.map((item) => {
      const itemErr: ItemFieldErrors = {}
      if (!item.activityDescription.trim()) {
        itemErr.activityDescription = 'Descrição da atividade é obrigatória'
      }
      if (!item.status) {
        itemErr.status = 'Status é obrigatório'
      }
      if (itemRequiresDetails(item.status)) {
        if (!item.reason.trim()) {
          itemErr.reason = 'Motivo é obrigatório'
        }
        if (!item.observation.trim()) {
          itemErr.observation = 'Observação é obrigatória'
        }
        if (!item.newExpectedDate) {
          itemErr.newExpectedDate = 'Nova data prevista é obrigatória'
        }
      }
      return Object.keys(itemErr).length > 0 ? itemErr : undefined
    })

    setFieldErrors(errors)
    setItemErrors(newItemErrors)
    return Object.keys(errors).length === 0 && newItemErrors.every((err) => !err)
  }

  function buildPayload() {
    return {
      date: form.date,
      teamPresentWorkerIds: form.teamPresentWorkerIds,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      weatherCondition: form.weatherCondition.trim() || null,
      extraServicesExecuted: form.extraServicesExecuted.trim() || null,
      problemsFound: form.problemsFound.trim() || null,
      pendingIssuesGenerated: form.pendingIssuesGenerated.trim() || null,
      materialsUsed: form.materialsUsed.trim() || null,
      materialsMissing: form.materialsMissing.trim() || null,
      forecastForNextDay: form.forecastForNextDay.trim() || null,
      notes: form.notes.trim() || null,
      items: form.items.map((item) => ({
        activityDescription: item.activityDescription.trim(),
        status: item.status as DailyReportItemStatus,
        reason: item.reason.trim() || null,
        observation: item.observation.trim() || null,
        newExpectedDate: item.newExpectedDate || null,
      })),
    }
  }

  function extractErrorMessage(error: unknown, fallback: string): string {
    if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
      return error.response.data.message
    }
    return fallback
  }

  async function handleSaveDraft(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    setSuccessMessage(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)

    try {
      if (isEditMode && report) {
        const updated = await updateDailyReport(report.id, buildPayload())
        setReport(updated)
        setSuccessMessage('Rascunho salvo com sucesso.')
      } else {
        const createRequest: DailyReportCreateRequest = {
          workOrderId: Number(params.workOrderId),
          ...buildPayload(),
        }
        const created = await createDailyReport(createRequest)
        navigate(`/worker/daily-reports/${created.id}/edit`, { replace: true })
      }
    } catch (error) {
      setSubmitError(extractErrorMessage(error, 'Não foi possível salvar o checklist. Tente novamente.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleFinalize() {
    if (!report || !validate()) {
      return
    }

    const confirmed = window.confirm(
      'Tem certeza que deseja finalizar este Checklist Diário? Depois de finalizado, você não poderá mais ' +
        'editá-lo nem adicionar ou remover fotos, a menos que um gestor o reabra.',
    )
    if (!confirmed) {
      return
    }

    setSubmitError(null)
    setSuccessMessage(null)
    setIsFinalizing(true)

    try {
      await updateDailyReport(report.id, buildPayload())
      setReport(await finalizeDailyReport(report.id))
    } catch (error) {
      setSubmitError(extractErrorMessage(error, 'Não foi possível finalizar o checklist. Tente novamente.'))
    } finally {
      setIsFinalizing(false)
    }
  }

  function handleFileSelect(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    for (const file of files) {
      const uploadId = `${file.name}-${Date.now()}-${Math.random()}`
      setUploadingFiles((prev) => [...prev, { id: uploadId, fileName: file.name, progress: 0, error: null }])
      startUpload(uploadId, file)
    }
  }

  async function startUpload(uploadId: string, file: File) {
    if (!report) {
      return
    }

    try {
      const photo = await uploadDailyReportPhoto(report.id, file, (percent) => {
        setUploadingFiles((prev) => prev.map((u) => (u.id === uploadId ? { ...u, progress: percent } : u)))
      })
      setReport((prev) => (prev ? { ...prev, photos: [...prev.photos, photo] } : prev))
      setUploadingFiles((prev) => prev.filter((u) => u.id !== uploadId))
    } catch (error) {
      const message = extractErrorMessage(error, 'Falha no envio da foto.')
      setUploadingFiles((prev) => prev.map((u) => (u.id === uploadId ? { ...u, error: message } : u)))
    }
  }

  async function handleDeletePhoto(photo: DailyReportPhotoResponse) {
    if (!report) {
      return
    }

    setDeletingPhotoId(photo.id)

    try {
      await deleteDailyReportPhoto(report.id, photo.id)
      setReport((prev) => (prev ? { ...prev, photos: prev.photos.filter((p) => p.id !== photo.id) } : prev))
    } catch (error) {
      setSubmitError(extractErrorMessage(error, 'Não foi possível remover a foto. Tente novamente.'))
    } finally {
      setDeletingPhotoId(null)
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

  const teamPool: WorkerSummaryResponse[] = workOrder.assignedWorkers
  const backLink = isEditMode ? `/worker/work-orders/${workOrder.id}` : `/worker/work-orders/${workOrder.id}`

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">
          {isEditMode ? 'Checklist Diário' : 'Novo Checklist Diário'} — OS Nº {workOrder.orderNumber}
        </h1>
        <Link to={backLink} className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      {isReadOnly && (
        <div className="rounded-md border border-muted bg-white px-3 py-2.5 text-sm text-gray-500">
          Este checklist foi finalizado e não pode mais ser editado. Peça a um gestor para reabri-lo caso precise
          de alguma alteração.
        </div>
      )}

      <form onSubmit={handleSaveDraft} noValidate className="space-y-5 rounded-lg border border-muted bg-white p-6 shadow-sm">
        {submitError && (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
            {submitError}
          </div>
        )}
        {successMessage && (
          <div role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2.5 text-sm text-green-700">
            {successMessage}
          </div>
        )}

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          <FormField id="date" label="Data" error={fieldErrors.date} required>
            <input
              id="date"
              type="date"
              disabled={isReadOnly}
              value={form.date}
              onChange={(event) => updateField('date', event.target.value)}
              className={inputClass}
            />
          </FormField>
          <FormField id="startTime" label="Horário de início">
            <input
              id="startTime"
              type="time"
              disabled={isReadOnly}
              value={form.startTime}
              onChange={(event) => updateField('startTime', event.target.value)}
              className={inputClass}
            />
          </FormField>
          <FormField id="endTime" label="Horário de fim">
            <input
              id="endTime"
              type="time"
              disabled={isReadOnly}
              value={form.endTime}
              onChange={(event) => updateField('endTime', event.target.value)}
              className={inputClass}
            />
          </FormField>
        </div>

        <FormField id="weatherCondition" label="Condição climática">
          <input
            id="weatherCondition"
            type="text"
            disabled={isReadOnly}
            value={form.weatherCondition}
            onChange={(event) => updateField('weatherCondition', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-foreground">Equipe presente</label>
          {teamPool.length === 0 ? (
            <p className="text-sm text-gray-500">Nenhum colaborador atribuído a esta OS.</p>
          ) : (
            <div className="space-y-2 rounded-md border border-muted p-3">
              {teamPool.map((worker) => (
                <label key={worker.id} className="flex items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    disabled={isReadOnly}
                    checked={form.teamPresentWorkerIds.includes(worker.id)}
                    onChange={() => toggleTeamMember(worker.id)}
                    className="h-4 w-4 rounded border-muted accent-primary focus:ring-primary"
                  />
                  <span>
                    {worker.name}
                    {worker.function && ` — ${worker.function}`}
                  </span>
                </label>
              ))}
            </div>
          )}
        </div>

        <div>
          <div className="mb-1.5 flex items-center justify-between">
            <label className="text-sm font-medium text-foreground">Itens do checklist</label>
            {!isReadOnly && (
              <button type="button" onClick={addItem} className="text-sm font-medium text-accent-dark hover:underline">
                + Adicionar item
              </button>
            )}
          </div>

          <div className="space-y-4">
            {form.items.map((item, index) => {
              const errors = itemErrors[index]
              const requiresDetails = itemRequiresDetails(item.status)

              return (
                <div key={index} className="rounded-md border border-muted p-3">
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-xs font-medium text-gray-500">Item {index + 1}</span>
                    {!isReadOnly && form.items.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeItem(index)}
                        className="text-xs font-medium text-red-700 hover:underline"
                      >
                        Remover
                      </button>
                    )}
                  </div>

                  <div className="mt-2 space-y-3">
                    <FormField
                      id={`item-${index}-activity`}
                      label="Descrição da atividade"
                      error={errors?.activityDescription}
                      required
                    >
                      <input
                        id={`item-${index}-activity`}
                        type="text"
                        disabled={isReadOnly}
                        value={item.activityDescription}
                        onChange={(event) => updateItem(index, 'activityDescription', event.target.value)}
                        className={inputClass}
                      />
                    </FormField>

                    <FormField id={`item-${index}-status`} label="Status" error={errors?.status} required>
                      <select
                        id={`item-${index}-status`}
                        disabled={isReadOnly}
                        value={item.status}
                        onChange={(event) =>
                          updateItem(index, 'status', event.target.value as DailyReportItemStatus)
                        }
                        className={inputClass}
                      >
                        <option value="">Selecione um status</option>
                        {DAILY_REPORT_ITEM_STATUS_OPTIONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </FormField>

                    {requiresDetails && (
                      <>
                        <FormField id={`item-${index}-reason`} label="Motivo" error={errors?.reason} required>
                          <input
                            id={`item-${index}-reason`}
                            type="text"
                            disabled={isReadOnly}
                            value={item.reason}
                            onChange={(event) => updateItem(index, 'reason', event.target.value)}
                            className={inputClass}
                          />
                        </FormField>
                        <FormField
                          id={`item-${index}-observation`}
                          label="Observação"
                          error={errors?.observation}
                          required
                        >
                          <input
                            id={`item-${index}-observation`}
                            type="text"
                            disabled={isReadOnly}
                            value={item.observation}
                            onChange={(event) => updateItem(index, 'observation', event.target.value)}
                            className={inputClass}
                          />
                        </FormField>
                        <FormField
                          id={`item-${index}-newDate`}
                          label="Nova data prevista"
                          error={errors?.newExpectedDate}
                          required
                        >
                          <input
                            id={`item-${index}-newDate`}
                            type="date"
                            disabled={isReadOnly}
                            value={item.newExpectedDate}
                            onChange={(event) => updateItem(index, 'newExpectedDate', event.target.value)}
                            className={inputClass}
                          />
                        </FormField>
                      </>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <FormField id="extraServicesExecuted" label="Serviços extras executados">
          <textarea
            id="extraServicesExecuted"
            disabled={isReadOnly}
            value={form.extraServicesExecuted}
            onChange={(event) => updateField('extraServicesExecuted', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="problemsFound" label="Problemas encontrados">
          <textarea
            id="problemsFound"
            disabled={isReadOnly}
            value={form.problemsFound}
            onChange={(event) => updateField('problemsFound', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="pendingIssuesGenerated" label="Pendências geradas">
          <textarea
            id="pendingIssuesGenerated"
            disabled={isReadOnly}
            value={form.pendingIssuesGenerated}
            onChange={(event) => updateField('pendingIssuesGenerated', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="materialsUsed" label="Materiais utilizados">
          <textarea
            id="materialsUsed"
            disabled={isReadOnly}
            value={form.materialsUsed}
            onChange={(event) => updateField('materialsUsed', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="materialsMissing" label="Materiais em falta">
          <textarea
            id="materialsMissing"
            disabled={isReadOnly}
            value={form.materialsMissing}
            onChange={(event) => updateField('materialsMissing', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="forecastForNextDay" label="Previsão para o próximo dia">
          <textarea
            id="forecastForNextDay"
            disabled={isReadOnly}
            value={form.forecastForNextDay}
            onChange={(event) => updateField('forecastForNextDay', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="notes" label="Observações">
          <textarea
            id="notes"
            disabled={isReadOnly}
            value={form.notes}
            onChange={(event) => updateField('notes', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        {isEditMode && report && (
          <div className="border-t border-muted pt-4">
            <label className="mb-1.5 block text-sm font-medium text-foreground">Fotos</label>

            {!isReadOnly && (
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={handleFileSelect}
                className="mb-3 block w-full text-sm text-gray-500 file:mr-3 file:rounded-md file:border-0 file:bg-background file:px-3 file:py-2 file:text-sm file:font-medium file:text-foreground"
              />
            )}

            {uploadingFiles.length > 0 && (
              <ul className="mb-3 space-y-1.5">
                {uploadingFiles.map((upload) => (
                  <li key={upload.id} className="text-xs">
                    <div className="flex items-center justify-between">
                      <span className="truncate text-gray-500">{upload.fileName}</span>
                      <span className={upload.error ? 'text-red-600' : 'text-gray-500'}>
                        {upload.error ?? `${upload.progress}%`}
                      </span>
                    </div>
                    {!upload.error && (
                      <div className="mt-0.5 h-1.5 overflow-hidden rounded-full bg-background">
                        <div className="h-full bg-primary transition-all" style={{ width: `${upload.progress}%` }} />
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}

            <PhotoGrid
              photos={report.photos}
              onDelete={isReadOnly ? undefined : handleDeletePhoto}
              deletingPhotoId={deletingPhotoId}
            />
          </div>
        )}

        {!isReadOnly && (
          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button type="submit" disabled={isSubmitting} className={primaryButtonClass}>
              {isSubmitting ? 'Salvando...' : 'Salvar Rascunho'}
            </button>
            {isEditMode && (
              <button
                type="button"
                disabled={isFinalizing}
                onClick={handleFinalize}
                className={dangerButtonClass}
              >
                {isFinalizing ? 'Finalizando...' : 'Finalizar'}
              </button>
            )}
            <Link to={backLink} className={secondaryButtonClass}>
              Cancelar
            </Link>
          </div>
        )}
      </form>
    </div>
  )
}
