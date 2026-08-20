import type { UserSummaryResponse, WorkerSummaryResponse } from '../types'

interface FilledBy {
  filledByWorker: WorkerSummaryResponse | null
  filledByUser: UserSummaryResponse | null
}

/**
 * A Checklist Diário is filled either by the assigned colaborador or, when the manager visits
 * the site himself, by the manager. The backend guarantees exactly one of the two is set.
 */
export function fillerName(report: FilledBy): string {
  return report.filledByWorker?.name ?? report.filledByUser?.name ?? '—'
}

export function isFilledByManager(report: FilledBy): boolean {
  return report.filledByUser !== null
}
