import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { createProject, getProject, updateProject } from '../../services/projectService'
import { getManagers } from '../../services/userService'
import type { ErrorResponse, ProjectStatus, UserResponse } from '../../types'
import { PROJECT_STATUS_OPTIONS } from './projectStatusOptions'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none'

interface FormState {
  name: string
  client: string
  address: string
  responsibleUserId: string
  startDate: string
  expectedDeadline: string
  currentStage: string
  status: ProjectStatus | ''
  notes: string
}

interface FieldErrors {
  name?: string
  client?: string
  responsibleUserId?: string
  status?: string
}

const EMPTY_FORM: FormState = {
  name: '',
  client: '',
  address: '',
  responsibleUserId: '',
  startDate: '',
  expectedDeadline: '',
  currentStage: '',
  status: '',
  notes: '',
}

interface FieldProps {
  id: string
  label: string
  error?: string
  required?: boolean
  children: ReactNode
}

function Field({ id, label, error, required, children }: FieldProps) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-foreground">
        {label}
        {required && <span className="text-red-600"> *</span>}
      </label>
      {children}
      {error && <p className="mt-1.5 text-sm text-red-600">{error}</p>}
    </div>
  )
}

export function ProjectFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEditMode = id !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [managers, setManagers] = useState<UserResponse[]>([])
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadData() {
    setIsLoading(true)
    setLoadError(null)

    try {
      const managersList = await getManagers()
      setManagers(managersList)

      if (id) {
        const project = await getProject(Number(id))
        setForm({
          name: project.name,
          client: project.client,
          address: project.address ?? '',
          responsibleUserId: String(project.responsibleUser.id),
          startDate: project.startDate ?? '',
          expectedDeadline: project.expectedDeadline ?? '',
          currentStage: project.currentStage ?? '',
          status: project.status,
          notes: project.notes ?? '',
        })
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

  function validate(): boolean {
    const errors: FieldErrors = {}

    if (!form.name.trim()) {
      errors.name = 'Nome da obra é obrigatório'
    }
    if (!form.client.trim()) {
      errors.client = 'Cliente é obrigatório'
    }
    if (!form.responsibleUserId) {
      errors.responsibleUserId = 'Responsável é obrigatório'
    }
    if (isEditMode && !form.status) {
      errors.status = 'Status é obrigatório'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    setSuccessMessage(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)

    try {
      if (isEditMode) {
        const updated = await updateProject(Number(id), {
          name: form.name.trim(),
          client: form.client.trim(),
          address: form.address.trim() || null,
          responsibleUserId: Number(form.responsibleUserId),
          startDate: form.startDate || null,
          expectedDeadline: form.expectedDeadline || null,
          currentStage: form.currentStage.trim() || null,
          notes: form.notes.trim() || null,
          status: form.status as ProjectStatus,
        })
        setForm({
          name: updated.name,
          client: updated.client,
          address: updated.address ?? '',
          responsibleUserId: String(updated.responsibleUser.id),
          startDate: updated.startDate ?? '',
          expectedDeadline: updated.expectedDeadline ?? '',
          currentStage: updated.currentStage ?? '',
          status: updated.status,
          notes: updated.notes ?? '',
        })
        setSuccessMessage('Obra atualizada com sucesso.')
      } else {
        await createProject({
          name: form.name.trim(),
          client: form.client.trim(),
          address: form.address.trim() || null,
          responsibleUserId: Number(form.responsibleUserId),
          startDate: form.startDate || null,
          expectedDeadline: form.expectedDeadline || null,
          currentStage: form.currentStage.trim() || null,
          notes: form.notes.trim() || null,
        })
        navigate('/manager/projects')
      }
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível salvar a obra. Tente novamente.')
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

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">{isEditMode ? 'Editar Obra' : 'Nova Obra'}</h1>
        <Link to="/manager/projects" className="text-sm font-medium text-accent-dark hover:underline">
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
        {successMessage && (
          <div
            role="status"
            className="rounded-md border border-green-200 bg-green-50 px-3 py-2.5 text-sm text-green-700"
          >
            {successMessage}
          </div>
        )}

        <Field id="name" label="Nome da obra" error={fieldErrors.name} required>
          <input
            id="name"
            type="text"
            value={form.name}
            onChange={(event) => updateField('name', event.target.value)}
            className={inputClass}
          />
        </Field>

        <Field id="client" label="Cliente" error={fieldErrors.client} required>
          <input
            id="client"
            type="text"
            value={form.client}
            onChange={(event) => updateField('client', event.target.value)}
            className={inputClass}
          />
        </Field>

        <Field id="address" label="Endereço">
          <input
            id="address"
            type="text"
            value={form.address}
            onChange={(event) => updateField('address', event.target.value)}
            className={inputClass}
          />
        </Field>

        <Field id="responsibleUserId" label="Responsável" error={fieldErrors.responsibleUserId} required>
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
        </Field>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <Field id="startDate" label="Data de início">
            <input
              id="startDate"
              type="date"
              value={form.startDate}
              onChange={(event) => updateField('startDate', event.target.value)}
              className={inputClass}
            />
          </Field>
          <Field id="expectedDeadline" label="Prazo previsto">
            <input
              id="expectedDeadline"
              type="date"
              value={form.expectedDeadline}
              onChange={(event) => updateField('expectedDeadline', event.target.value)}
              className={inputClass}
            />
          </Field>
        </div>

        <Field id="currentStage" label="Etapa atual">
          <input
            id="currentStage"
            type="text"
            value={form.currentStage}
            onChange={(event) => updateField('currentStage', event.target.value)}
            className={inputClass}
          />
        </Field>

        {isEditMode && (
          <Field id="status" label="Status" error={fieldErrors.status} required>
            <select
              id="status"
              value={form.status}
              onChange={(event) => updateField('status', event.target.value as ProjectStatus)}
              className={inputClass}
            >
              <option value="">Selecione um status</option>
              {PROJECT_STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </Field>
        )}

        <Field id="notes" label="Observações">
          <textarea
            id="notes"
            value={form.notes}
            onChange={(event) => updateField('notes', event.target.value)}
            rows={3}
            className={inputClass}
          />
        </Field>

        <div className="flex items-center gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-accent px-4 py-2.5 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </button>
          <Link to="/manager/projects" className="text-sm font-medium text-gray-500 hover:text-foreground">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}
