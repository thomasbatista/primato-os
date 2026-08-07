import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { FormField } from '../../components/FormField'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { getUnlinkedWorkerUsers } from '../../services/userService'
import { createWorker, getWorker, updateWorker } from '../../services/workerService'
import type { ErrorResponse, UserSummaryResponse } from '../../types'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none'

interface FormState {
  name: string
  function: string
  phone: string
  userId: string
}

interface FieldErrors {
  name?: string
}

const EMPTY_FORM: FormState = { name: '', function: '', phone: '', userId: '' }

export function WorkerFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEditMode = id !== undefined
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [linkableUsers, setLinkableUsers] = useState<UserSummaryResponse[]>([])
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
      const unlinkedUsers = await getUnlinkedWorkerUsers()

      if (isEditMode) {
        const worker = await getWorker(Number(id))
        setForm({
          name: worker.name,
          function: worker.function ?? '',
          phone: worker.phone ?? '',
          userId: worker.user ? String(worker.user.id) : '',
        })

        // The worker's currently linked user is excluded from the "unlinked" list by
        // definition (it's linked — to this very worker) — add it back so the picker
        // still shows and keeps it selected, instead of silently dropping the link.
        setLinkableUsers(worker.user ? [worker.user, ...unlinkedUsers] : unlinkedUsers)
      } else {
        setLinkableUsers(unlinkedUsers)
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
      errors.name = 'Nome é obrigatório'
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
      name: form.name.trim(),
      function: form.function.trim() || null,
      phone: form.phone.trim() || null,
      userId: form.userId ? Number(form.userId) : null,
    }

    try {
      if (isEditMode) {
        await updateWorker(Number(id), payload)
      } else {
        await createWorker(payload)
      }
      navigate('/manager/workers')
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível salvar o colaborador. Tente novamente.')
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
    <div className="max-w-md space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">
          {isEditMode ? 'Editar Colaborador' : 'Novo Colaborador'}
        </h1>
        <Link to="/manager/workers" className="text-sm font-medium text-accent-dark hover:underline">
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

        <FormField id="name" label="Nome" error={fieldErrors.name} required>
          <input
            id="name"
            type="text"
            value={form.name}
            onChange={(event) => updateField('name', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <FormField id="function" label="Função">
          <input
            id="function"
            type="text"
            value={form.function}
            onChange={(event) => updateField('function', event.target.value)}
            placeholder="Ex: Pedreiro, Eletricista"
            className={inputClass}
          />
        </FormField>

        <FormField id="phone" label="Telefone">
          <input
            id="phone"
            type="text"
            value={form.phone}
            onChange={(event) => updateField('phone', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <FormField id="userId" label="Login vinculado">
          <select
            id="userId"
            value={form.userId}
            onChange={(event) => updateField('userId', event.target.value)}
            className={inputClass}
          >
            <option value="">Nenhum</option>
            {linkableUsers.map((user) => (
              <option key={user.id} value={user.id}>
                {user.name} ({user.email})
              </option>
            ))}
          </select>
          <p className="mt-1.5 text-xs text-gray-500">
            Só aparecem aqui usuários com papel de colaborador que ainda não estão vinculados a outro cadastro.
          </p>
        </FormField>

        <div className="flex items-center gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-accent px-4 py-2.5 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </button>
          <Link to="/manager/workers" className="text-sm font-medium text-gray-500 hover:text-foreground">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}
