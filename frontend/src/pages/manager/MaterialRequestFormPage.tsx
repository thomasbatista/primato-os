import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { FormField } from '../../components/FormField'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { useAuth } from '../../hooks/useAuth'
import {
  createMaterialRequest,
  createMaterialRequestFromWorkOrder,
  getMaterialRequest,
  updateMaterialRequest,
} from '../../services/materialRequestService'
import { getProjects } from '../../services/projectService'
import { getManagers } from '../../services/userService'
import { getWorkOrder, getWorkOrders } from '../../services/workOrderService'
import type {
  ErrorResponse,
  MaterialRequestPriority,
  MaterialRequestUnit,
  ProjectSummaryResponse,
  UserResponse,
  WorkOrderResponse,
} from '../../types'
import { MATERIAL_REQUEST_PRIORITY_OPTIONS, MATERIAL_REQUEST_UNIT_OPTIONS } from './materialRequestOptions'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none'

interface ItemFormState {
  name: string
  description: string
  quantity: string
  unit: MaterialRequestUnit | ''
  brand: string
  notes: string
}

interface ItemFieldErrors {
  name?: string
  quantity?: string
  unit?: string
}

interface FormState {
  projectId: string
  workOrderId: string
  requesterId: string
  neededByDate: string
  priority: MaterialRequestPriority | ''
  justification: string
  notes: string
  deliveryLocation: string
  items: ItemFormState[]
}

interface FieldErrors {
  projectId?: string
  requesterId?: string
  priority?: string
}

const EMPTY_ITEM: ItemFormState = { name: '', description: '', quantity: '', unit: '', brand: '', notes: '' }

const EMPTY_FORM: FormState = {
  projectId: '',
  workOrderId: '',
  requesterId: '',
  neededByDate: '',
  priority: '',
  justification: '',
  notes: '',
  deliveryLocation: '',
  items: [EMPTY_ITEM],
}

function today(): string {
  return new Date().toLocaleDateString('en-CA')
}

export function MaterialRequestFormPage() {
  const params = useParams<{ id?: string; workOrderId?: string }>()
  const isEditMode = params.id !== undefined
  const isFromWorkOrderMode = params.workOrderId !== undefined
  const navigate = useNavigate()
  const { user } = useAuth()

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [requestDate, setRequestDate] = useState(today())
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([])
  const [managers, setManagers] = useState<UserResponse[]>([])
  const [workOrders, setWorkOrders] = useState<WorkOrderResponse[]>([])
  const [workOrderContext, setWorkOrderContext] = useState<WorkOrderResponse | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [itemErrors, setItemErrors] = useState<(ItemFieldErrors | undefined)[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id, params.workOrderId])

  async function loadData() {
    setIsLoading(true)
    setLoadError(null)

    try {
      if (isFromWorkOrderMode) {
        const workOrder = await getWorkOrder(Number(params.workOrderId))
        setWorkOrderContext(workOrder)
        setForm(EMPTY_FORM)
      } else if (isEditMode) {
        const [projectsList, managersList, workOrdersList] = await Promise.all([
          getProjects({ size: 100 }).then((page) => page.content),
          getManagers(),
          getWorkOrders({ size: 100 }).then((page) => page.content),
        ])
        setProjects(projectsList)
        setManagers(managersList)
        setWorkOrders(workOrdersList)

        const materialRequest = await getMaterialRequest(Number(params.id))
        setRequestDate(materialRequest.requestDate)
        setForm({
          projectId: String(materialRequest.project.id),
          workOrderId: materialRequest.workOrder ? String(materialRequest.workOrder.id) : '',
          requesterId: String(materialRequest.requester.id),
          neededByDate: materialRequest.neededByDate ?? '',
          priority: materialRequest.priority,
          justification: materialRequest.justification ?? '',
          notes: materialRequest.notes ?? '',
          deliveryLocation: materialRequest.deliveryLocation ?? '',
          items: materialRequest.items.map((item) => ({
            name: item.name,
            description: item.description ?? '',
            quantity: String(item.quantity),
            unit: item.unit,
            brand: item.brand ?? '',
            notes: item.notes ?? '',
          })),
        })
      } else {
        const [projectsList, managersList, workOrdersList] = await Promise.all([
          getProjects({ size: 100 }).then((page) => page.content),
          getManagers(),
          getWorkOrders({ size: 100 }).then((page) => page.content),
        ])
        setProjects(projectsList)
        setManagers(managersList)
        setWorkOrders(workOrdersList)

        const currentManager = managersList.find((manager) => manager.email === user?.email)
        setForm({ ...EMPTY_FORM, requesterId: currentManager ? String(currentManager.id) : '' })
      }
    } catch {
      setLoadError('Não foi possível carregar os dados. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }))
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

    if (!isFromWorkOrderMode) {
      if (!form.projectId) {
        errors.projectId = 'Obra é obrigatória'
      }
      if (!form.requesterId) {
        errors.requesterId = 'Solicitante é obrigatório'
      }
    }
    if (!form.priority) {
      errors.priority = 'Prioridade é obrigatória'
    }

    const newItemErrors: (ItemFieldErrors | undefined)[] = form.items.map((item) => {
      const itemErr: ItemFieldErrors = {}
      if (!item.name.trim()) {
        itemErr.name = 'Nome do material é obrigatório'
      }
      const quantity = Number(item.quantity)
      if (!item.quantity || !(quantity > 0)) {
        itemErr.quantity = 'Quantidade deve ser maior que zero'
      }
      if (!item.unit) {
        itemErr.unit = 'Unidade é obrigatória'
      }
      return Object.keys(itemErr).length > 0 ? itemErr : undefined
    })

    setFieldErrors(errors)
    setItemErrors(newItemErrors)
    return Object.keys(errors).length === 0 && newItemErrors.every((err) => !err)
  }

  function buildItemsPayload() {
    return form.items.map((item) => ({
      name: item.name.trim(),
      description: item.description.trim() || null,
      quantity: Number(item.quantity),
      unit: item.unit as MaterialRequestUnit,
      brand: item.brand.trim() || null,
      notes: item.notes.trim() || null,
    }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)

    try {
      if (isFromWorkOrderMode) {
        const created = await createMaterialRequestFromWorkOrder(Number(params.workOrderId), {
          neededByDate: form.neededByDate || null,
          priority: form.priority as MaterialRequestPriority,
          justification: form.justification.trim() || null,
          notes: form.notes.trim() || null,
          deliveryLocation: form.deliveryLocation.trim() || null,
          items: buildItemsPayload(),
        })
        navigate(`/manager/material-requests/${created.id}`)
        return
      }

      const payload = {
        projectId: Number(form.projectId),
        workOrderId: form.workOrderId ? Number(form.workOrderId) : null,
        requestDate,
        neededByDate: form.neededByDate || null,
        requesterId: Number(form.requesterId),
        priority: form.priority as MaterialRequestPriority,
        justification: form.justification.trim() || null,
        notes: form.notes.trim() || null,
        deliveryLocation: form.deliveryLocation.trim() || null,
        items: buildItemsPayload(),
      }

      if (isEditMode) {
        await updateMaterialRequest(Number(params.id), payload)
        navigate(`/manager/material-requests/${params.id}`)
      } else {
        const created = await createMaterialRequest(payload)
        navigate(`/manager/material-requests/${created.id}`)
      }
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível salvar o pedido de materiais. Tente novamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError}</p>
        <button type="button" onClick={loadData} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  const availableWorkOrders = form.projectId
    ? workOrders.filter((workOrder) => workOrder.project.id === Number(form.projectId))
    : workOrders
  const backLink = isEditMode ? `/manager/material-requests/${params.id}` : '/manager/material-requests'

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">
          {isEditMode ? 'Editar Pedido de Materiais' : 'Novo Pedido de Materiais'}
        </h1>
        <Link to={backLink} className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <form
        onSubmit={handleSubmit}
        noValidate
        className="space-y-5 rounded-lg border border-muted bg-white p-6 shadow-sm"
      >
        {submitError && (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
            {submitError}
          </div>
        )}

        {isFromWorkOrderMode && workOrderContext && (
          <div className="rounded-md border border-muted bg-background px-3 py-2.5 text-sm text-foreground">
            Gerando pedido para <strong>OS Nº {workOrderContext.orderNumber}</strong> — {workOrderContext.project.name}
          </div>
        )}

        {!isFromWorkOrderMode && (
          <>
            <FormField id="projectId" label="Obra" error={fieldErrors.projectId} required>
              <select
                id="projectId"
                value={form.projectId}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, projectId: event.target.value, workOrderId: '' }))
                }
                className={inputClass}
              >
                <option value="">Selecione uma obra</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField id="workOrderId" label="Ordem de Serviço">
              <select
                id="workOrderId"
                value={form.workOrderId}
                onChange={(event) => updateField('workOrderId', event.target.value)}
                className={inputClass}
              >
                <option value="">Nenhuma</option>
                {availableWorkOrders.map((workOrder) => (
                  <option key={workOrder.id} value={workOrder.id}>
                    OS Nº {workOrder.orderNumber} — {workOrder.stage}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField id="requesterId" label="Solicitante" error={fieldErrors.requesterId} required>
              <select
                id="requesterId"
                value={form.requesterId}
                onChange={(event) => updateField('requesterId', event.target.value)}
                className={inputClass}
              >
                <option value="">Selecione um gestor</option>
                {managers.map((manager) => (
                  <option key={manager.id} value={manager.id}>
                    {manager.name}
                  </option>
                ))}
              </select>
            </FormField>
          </>
        )}

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <FormField id="neededByDate" label="Necessário até">
            <input
              id="neededByDate"
              type="date"
              value={form.neededByDate}
              onChange={(event) => updateField('neededByDate', event.target.value)}
              className={inputClass}
            />
          </FormField>
          <FormField id="priority" label="Prioridade" error={fieldErrors.priority} required>
            <select
              id="priority"
              value={form.priority}
              onChange={(event) => updateField('priority', event.target.value as MaterialRequestPriority)}
              className={inputClass}
            >
              <option value="">Selecione uma prioridade</option>
              {MATERIAL_REQUEST_PRIORITY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </FormField>
        </div>

        <FormField id="justification" label="Justificativa">
          <textarea
            id="justification"
            value={form.justification}
            onChange={(event) => updateField('justification', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="deliveryLocation" label="Local de entrega">
          <input
            id="deliveryLocation"
            type="text"
            value={form.deliveryLocation}
            onChange={(event) => updateField('deliveryLocation', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <FormField id="notes" label="Observações">
          <textarea
            id="notes"
            value={form.notes}
            onChange={(event) => updateField('notes', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <div>
          <div className="mb-1.5 flex items-center justify-between">
            <label className="text-sm font-medium text-foreground">Itens</label>
            <button type="button" onClick={addItem} className="text-sm font-medium text-accent-dark hover:underline">
              + Adicionar item
            </button>
          </div>

          <div className="space-y-4">
            {form.items.map((item, index) => {
              const errors = itemErrors[index]

              return (
                <div key={index} className="rounded-md border border-muted p-3">
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-xs font-medium text-gray-500">Item {index + 1}</span>
                    {form.items.length > 1 && (
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
                    <FormField id={`item-${index}-name`} label="Nome do material" error={errors?.name} required>
                      <input
                        id={`item-${index}-name`}
                        type="text"
                        value={item.name}
                        onChange={(event) => updateItem(index, 'name', event.target.value)}
                        className={inputClass}
                      />
                    </FormField>

                    <FormField id={`item-${index}-description`} label="Descrição">
                      <input
                        id={`item-${index}-description`}
                        type="text"
                        value={item.description}
                        onChange={(event) => updateItem(index, 'description', event.target.value)}
                        className={inputClass}
                      />
                    </FormField>

                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                      <FormField id={`item-${index}-quantity`} label="Quantidade" error={errors?.quantity} required>
                        <input
                          id={`item-${index}-quantity`}
                          type="number"
                          min="0"
                          step="0.01"
                          value={item.quantity}
                          onChange={(event) => updateItem(index, 'quantity', event.target.value)}
                          className={inputClass}
                        />
                      </FormField>
                      <FormField id={`item-${index}-unit`} label="Unidade" error={errors?.unit} required>
                        <select
                          id={`item-${index}-unit`}
                          value={item.unit}
                          onChange={(event) => updateItem(index, 'unit', event.target.value as MaterialRequestUnit)}
                          className={inputClass}
                        >
                          <option value="">Selecione</option>
                          {MATERIAL_REQUEST_UNIT_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                      </FormField>
                    </div>

                    <FormField id={`item-${index}-brand`} label="Marca/Referência">
                      <input
                        id={`item-${index}-brand`}
                        type="text"
                        value={item.brand}
                        onChange={(event) => updateItem(index, 'brand', event.target.value)}
                        className={inputClass}
                      />
                    </FormField>

                    <FormField id={`item-${index}-notes`} label="Observações">
                      <input
                        id={`item-${index}-notes`}
                        type="text"
                        value={item.notes}
                        onChange={(event) => updateItem(index, 'notes', event.target.value)}
                        className={inputClass}
                      />
                    </FormField>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="flex items-center gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-accent px-4 py-2.5 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </button>
          <Link to={backLink} className="text-sm font-medium text-gray-500 hover:text-foreground">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}
