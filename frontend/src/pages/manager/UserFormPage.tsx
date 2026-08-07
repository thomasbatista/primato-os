import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { FormField } from '../../components/FormField'
import { createUser } from '../../services/userService'
import type { ErrorResponse, UserRole } from '../../types'
import { USER_ROLE_OPTIONS } from './userRoleOptions'

const inputClass =
  'w-full rounded-md border border-muted px-3 py-2.5 text-base text-foreground focus:border-primary focus:ring-1 focus:ring-primary focus:outline-none'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_PASSWORD_LENGTH = 8

interface FormState {
  name: string
  email: string
  password: string
  role: UserRole | ''
}

interface FieldErrors {
  name?: string
  email?: string
  password?: string
  role?: string
}

const EMPTY_FORM: FormState = { name: '', email: '', password: '', role: '' }

export function UserFormPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  function validate(): boolean {
    const errors: FieldErrors = {}

    if (!form.name.trim()) {
      errors.name = 'Nome é obrigatório'
    }
    if (!form.email.trim()) {
      errors.email = 'Email é obrigatório'
    } else if (!EMAIL_PATTERN.test(form.email.trim())) {
      errors.email = 'Informe um email válido'
    }
    if (!form.password) {
      errors.password = 'Senha é obrigatória'
    } else if (form.password.length < MIN_PASSWORD_LENGTH) {
      errors.password = `Senha deve ter no mínimo ${MIN_PASSWORD_LENGTH} caracteres`
    }
    if (!form.role) {
      errors.role = 'Papel é obrigatório'
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

    try {
      await createUser({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role as UserRole,
      })
      navigate('/manager/users')
    } catch (error) {
      if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
        setSubmitError(error.response.data.message)
      } else {
        setSubmitError('Não foi possível salvar o usuário. Tente novamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const passwordLength = form.password.length
  const passwordMeetsMinimum = passwordLength >= MIN_PASSWORD_LENGTH
  const passwordHintClass = passwordLength === 0
    ? 'text-gray-500'
    : passwordMeetsMinimum
      ? 'text-green-700'
      : 'text-red-600'

  return (
    <div className="max-w-md space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">Novo Usuário</h1>
        <Link to="/manager/users" className="text-sm font-medium text-accent-dark hover:underline">
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

        <FormField id="email" label="Email" error={fieldErrors.email} required>
          <input
            id="email"
            type="email"
            value={form.email}
            onChange={(event) => updateField('email', event.target.value)}
            className={inputClass}
          />
        </FormField>

        <FormField id="password" label="Senha" error={fieldErrors.password} required>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={(event) => updateField('password', event.target.value)}
            className={inputClass}
          />
          <p className={`mt-1.5 text-xs ${passwordHintClass}`}>
            {passwordMeetsMinimum ? '✓ ' : ''}
            {passwordLength}/{MIN_PASSWORD_LENGTH} caracteres mínimos
          </p>
        </FormField>

        <FormField id="role" label="Papel" error={fieldErrors.role} required>
          <select
            id="role"
            value={form.role}
            onChange={(event) => updateField('role', event.target.value as UserRole)}
            className={inputClass}
          >
            <option value="">Selecione um papel</option>
            {USER_ROLE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>

        {form.role === 'WORKER' && (
          <p className="text-xs text-gray-500">
            Isso cria apenas o login do colaborador, sem vínculo com um cadastro de colaborador (função, telefone).
            Esse vínculo precisa ser feito separadamente.
          </p>
        )}

        <div className="flex items-center gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-accent px-4 py-2.5 text-sm font-medium text-foreground transition hover:brightness-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </button>
          <Link to="/manager/users" className="text-sm font-medium text-gray-500 hover:text-foreground">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}
