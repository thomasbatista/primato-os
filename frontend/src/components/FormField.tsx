import type { ReactNode } from 'react'

interface FormFieldProps {
  id: string
  label: string
  error?: string
  required?: boolean
  children: ReactNode
}

export function FormField({ id, label, error, required, children }: FormFieldProps) {
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
