import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { FormField } from '../../components/FormField'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { getProjects } from '../../services/projectService'
import { getManagers } from '../../services/userService'
import { getActiveWorkers } from '../../services/workerService'
import { createWorkOrder, getWorkOrder, updateWorkOrder } from '../../services/workOrderService'
import type { ErrorResponse, ProjectSummaryResponse, UserResponse, WorkerSummaryResponse } from '../../types'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none'

interface FormState {
  projectId: string
  responsibleUserId: string
  date: string
  stage: string
  location: string
  description: string
  dailyGoal: string
  plannedStartTime: string
  plannedEndTime: string
  materialsNeeded: string
  tools: string
  safetyGuidelines: string
  qualityCriteria: string
  notes: string
  assignedWorkerIds: number[]
}

interface FieldErrors {
  projectId?: string
  responsibleUserId?: string
  date?: string
  stage?: string
  description?: string
}

const EMPTY_FORM: FormState = {
  projectId: '',
  responsibleUserId: '',
  date: '',
  stage: '',
  location: '',
  description: '',
  dailyGoal: '',
  plannedStartTime: '',
  plannedEndTime: '',
  materialsNeeded: '',
  tools: '',
  safetyGuidelines: '',
  qualityCriteria: '',
  notes: '',
  assignedWorkerIds: [],
}

interface WorkerOption {
  id: number
  name: string
  function: string | null
  inactive: boolean
}

export function WorkOrderFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEditMode = id !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([])
  const [managers, setManagers] = useState<UserResponse[]>([])
  const [activeWorkers, setActiveWorkers] = useState<WorkerOption[]>([])
  const [assignedInactiveWorkers, setAssignedInactiveWorkers] = useState<WorkerOption[]>([])
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadData() {
    setIsLoading(true)
    setLoadError(null)

    try {
      const [projectsList, managersList, workersList] = await Promise.all([
        getProjects({ size: 100 }).then((page) => page.content),
        getManagers(),
        getActiveWorkers(),
      ])
      setProjects(projectsList)
      setManagers(managersList)
      const activeOptions: WorkerOption[] = workersList.map((w) => ({
        id: w.id,
        name: w.name,
        function: w.function,
        inactive: false,
      }))
      setActiveWorkers(activeOptions)

      if (id) {
        const workOrder = await getWorkOrder(Number(id))
        setForm({
          projectId: String(workOrder.project.id),
          responsibleUserId: String(workOrder.responsibleUser.id),
          date: workOrder.date,
          stage: workOrder.stage,
          location: workOrder.location ?? '',
          description: workOrder.description,
          dailyGoal: workOrder.dailyGoal ?? '',
          plannedStartTime: workOrder.plannedStartTime?.slice(0, 5) ?? '',
          plannedEndTime: workOrder.plannedEndTime?.slice(0, 5) ?? '',
          materialsNeeded: workOrder.materialsNeeded ?? '',
          tools: workOrder.tools ?? '',
          safetyGuidelines: workOrder.safetyGuidelines ?? '',
          qualityCriteria: workOrder.qualityCriteria ?? '',
          notes: workOrder.notes ?? '',
          assignedWorkerIds: workOrder.assignedWorkers.map((w) => w.id),
        })

        // A worker assigned before being deactivated won't show up in the active-workers
        // fetch — keep them visible (and checked) so saving doesn't silently drop them;
        // the backend will still reject the save unless the manager unchecks them.
        const activeIds = new Set(activeOptions.map((w) => w.id))
        const inactiveAssigned: WorkerSummaryResponse[] = workOrder.assignedWorkers.filter(
          (w) => !activeIds.has(w.id),
        )
        setAssignedInactiveWorkers(
          inactiveAssigned.map((w) => ({ id: w.id, name: w.name, function: w.function, inactive: true })),
        )
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

  function toggleWorker(workerId: number) {
    setForm((prev) => ({
      ...prev,
      assignedWorkerIds: prev.assignedWorkerIds.includes(workerId)
        ? prev.assignedWorkerIds.filter((existingId) => existingId !== workerId)
        : [...prev.assignedWorkerIds, workerId],
    }))
  }

  function validate(): boolean {
    const errors: FieldErrors = {}

    if (!form.projectId) {
      errors.projectId = 'Obra é obrigatória'
    }
    if (!form.date) {
      errors.date = 'Data é obrigatória'
    }
    if (!form.responsibleUserId) {
      errors.responsibleUserId = 'Responsável é obrigatório'
    }
    if (!form.stage.trim()) {
      errors.stage = 'Etapa é obrigatória'
    }
    if (!form.description.trim()) {
      errors.description = 'Descrição é obrigatória'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)

    const payload = {
      projectId: Number(form.projectId),
      responsibleUserId: Number(form.responsibleUserId),
      date: form.date,
      stage: form.stage.trim(),
      location: form.location.trim() || null,
      description: form.description.trim(),
      dailyGoal: form.dailyGoal.trim() || null,
      plannedStartTime: form.plannedStartTime || null,
      plannedEndTime: form.plannedEndTime || null,
      materialsNeeded: form.materialsNeeded.trim() || null,
      tools: form.tools.trim() || null,
      safetyGuidelines: form.safetyGuidelines.trim() || null,
      qualityCriteria: form.qualityCriteria.trim() || null,
      notes: form.notes.trim() || null,
      assignedWorkerIds: form.assignedWorkerIds,
    }

    try {
      if (isEditMode) {
        await updateWorkOrder(Number(id), payload)
        navigate(`/manager/work-orders/${id}`)
      } else {
        const created = await createWorkOrder(payload)
        navigate(`/manager/work-orders/${created.id}`)
      }
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível salvar a ordem de serviço. Tente novamente.')
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

  const workerOptions = [...activeWorkers, ...assignedInactiveWorkers]
  const backLink = isEditMode ? `/manager/work-orders/${id}` : '/manager/work-orders'

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">
          {isEditMode ? 'Editar Ordem de Serviço' : 'Nova Ordem de Serviço'}
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

        <FormField id="projectId" label="Obra" error={fieldErrors.projectId} required>
          <select
            id="projectId"
            value={form.projectId}
            onChange={(event) => updateField('projectId', event.target.value)}
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

        <FormField id="responsibleUserId" label="Responsável" error={fieldErrors.responsibleUserId} required>
          <select
            id="responsibleUserId"
            value={form.responsibleUserId}
            onChange={(event) => updateField('responsibleUserId', event.target.value)}
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

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <FormField id="date" label="Data" error={fieldErrors.date} required>
            <input
              id="date"
              type="date"
              value={form.date}
              onChange={(event) => updateField('date', event.target.value)}
              className={inputClass}
            />
          </FormField>
          <FormField id="stage" label="Etapa" error={fieldErrors.stage} required>
            <input
              id="stage"
              type="text"
              value={form.stage}
              onChange={(event) => updateField('stage', event.target.value)}
              className={inputClass}
            />
          </FormField>
        </div>

        <FormField id="location" label="Local">
          <input
            id="location"
            type="text"
            value={form.location}
            onChange={(event) => updateField('location', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <FormField id="description" label="Descrição" error={fieldErrors.description} required>
          <textarea
            id="description"
            value={form.description}
            onChange={(event) => updateField('description', event.target.value)}
            rows={3}
            className={inputClass}
          />
        </FormField>

        <FormField id="dailyGoal" label="Meta diária">
          <input
            id="dailyGoal"
            type="text"
            value={form.dailyGoal}
            onChange={(event) => updateField('dailyGoal', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <FormField id="plannedStartTime" label="Horário planejado (início)">
            <input
              id="plannedStartTime"
              type="time"
              value={form.plannedStartTime}
              onChange={(event) => updateField('plannedStartTime', event.target.value)}
              className={inputClass}
            />
          </FormField>
          <FormField id="plannedEndTime" label="Horário planejado (fim)">
            <input
              id="plannedEndTime"
              type="time"
              value={form.plannedEndTime}
              onChange={(event) => updateField('plannedEndTime', event.target.value)}
              className={inputClass}
            />
          </FormField>
        </div>

        <FormField id="materialsNeeded" label="Materiais necessários">
          <textarea
            id="materialsNeeded"
            value={form.materialsNeeded}
            onChange={(event) => updateField('materialsNeeded', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="tools" label="Ferramentas">
          <textarea
            id="tools"
            value={form.tools}
            onChange={(event) => updateField('tools', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="safetyGuidelines" label="Diretrizes de segurança">
          <textarea
            id="safetyGuidelines"
            value={form.safetyGuidelines}
            onChange={(event) => updateField('safetyGuidelines', event.target.value)}
            rows={2}
            className={inputClass}
          />
        </FormField>

        <FormField id="qualityCriteria" label="Critérios de qualidade">
          <textarea
            id="qualityCriteria"
            value={form.qualityCriteria}
            onChange={(event) => updateField('qualityCriteria', event.target.value)}
            rows={2}
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
          <label className="mb-1.5 block text-sm font-medium text-foreground">Equipe atribuída</label>
          <div className="max-h-56 space-y-2 overflow-y-auto rounded-md border border-muted p-3">
            {workerOptions.length === 0 && <p className="text-sm text-gray-500">Nenhum colaborador ativo encontrado.</p>}
            {workerOptions.map((worker) => (
              <label key={worker.id} className="flex items-center gap-2 text-sm text-foreground">
                <input
                  type="checkbox"
                  checked={form.assignedWorkerIds.includes(worker.id)}
                  onChange={() => toggleWorker(worker.id)}
                  className="h-4 w-4 rounded border-muted accent-primary focus:ring-primary"
                />
                <span>
                  {worker.name}
                  {worker.function && ` — ${worker.function}`}
                  {worker.inactive && <span className="text-red-600"> (inativo — remova antes de salvar)</span>}
                </span>
              </label>
            ))}
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
