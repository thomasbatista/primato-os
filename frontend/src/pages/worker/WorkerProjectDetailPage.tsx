import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { DetailField } from '../../components/DetailField'
import { formatDate } from '../../components/formatters'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { PhotoSection } from '../../components/PhotoSection'
import { getMyProject, getProjectPhotos } from '../../services/projectService'
import type { ProjectResponse } from '../../types'

export function WorkerProjectDetailPage() {
  const { id } = useParams<{ id: string }>()

  const [project, setProject] = useState<ProjectResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    loadProject()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function loadProject() {
    setIsLoading(true)
    setLoadError(null)

    try {
      setProject(await getMyProject(Number(id)))
    } catch {
      setLoadError('Não foi possível carregar a obra. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (loadError || !project) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{loadError ?? 'Obra não encontrada.'}</p>
        <button type="button" onClick={loadProject} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-foreground">{project.name}</h1>
        <Link to="/worker" className="text-sm font-medium text-accent-dark hover:underline">
          Voltar
        </Link>
      </div>

      <div className="rounded-lg border border-muted bg-white p-6 shadow-sm">
        <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
          <DetailField label="Cliente">{project.client}</DetailField>
          <DetailField label="Endereço">{project.address}</DetailField>
          <DetailField label="Etapa atual">{project.currentStage}</DetailField>
          <DetailField label="Início">{project.startDate ? formatDate(project.startDate) : null}</DetailField>
        </dl>

        <div className="mt-6 border-t border-muted pt-4">
          <PhotoSection label="Fotos da obra" load={() => getProjectPhotos(project.id)} />
        </div>
      </div>
    </div>
  )
}
