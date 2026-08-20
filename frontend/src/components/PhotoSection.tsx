import { useEffect, useState, type ChangeEvent } from 'react'
import axios from 'axios'
import { PhotoGrid } from './PhotoGrid'
import type { ErrorResponse } from '../types'

interface Photo {
  id: number
  url: string
}

interface PhotoSectionProps {
  label: string
  load: () => Promise<Photo[]>
  // Omitting these renders the section read-only, which is how workers see it.
  upload?: (file: File) => Promise<Photo>
  remove?: (photo: Photo) => Promise<void>
}

function errorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ErrorResponse>(error) && error.response) {
    return error.response.data.message
  }
  return fallback
}

export function PhotoSection({ label, load, upload, remove }: PhotoSectionProps) {
  const [photos, setPhotos] = useState<Photo[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploadingCount, setUploadingCount] = useState(0)
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadPhotos() {
      setIsLoading(true)

      try {
        const data = await load()
        if (!cancelled) {
          setPhotos(data)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('Não foi possível carregar as fotos.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadPhotos()

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleFileSelect(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    if (!upload || files.length === 0) {
      return
    }

    setError(null)
    setUploadingCount((count) => count + files.length)

    for (const file of files) {
      try {
        const photo = await upload(file)
        setPhotos((prev) => [...prev, photo])
      } catch (uploadError) {
        setError(errorMessage(uploadError, 'Falha no envio da foto.'))
      } finally {
        setUploadingCount((count) => count - 1)
      }
    }
  }

  async function handleDelete(photo: Photo) {
    if (!remove || !window.confirm('Tem certeza que deseja remover esta foto?')) {
      return
    }

    setError(null)
    setDeletingPhotoId(photo.id)

    try {
      await remove(photo)
      setPhotos((prev) => prev.filter((p) => p.id !== photo.id))
    } catch (deleteError) {
      setError(errorMessage(deleteError, 'Não foi possível remover a foto.'))
    } finally {
      setDeletingPhotoId(null)
    }
  }

  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-foreground">{label}</label>

      {error && (
        <div role="alert" className="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}

      {upload && (
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          onChange={handleFileSelect}
          className="mb-3 block w-full text-sm text-gray-500 file:mr-3 file:rounded-md file:border-0 file:bg-background file:px-3 file:py-2 file:text-sm file:font-medium file:text-foreground"
        />
      )}

      {uploadingCount > 0 && (
        <p className="mb-2 text-xs text-gray-500">Enviando {uploadingCount} foto(s)...</p>
      )}

      {isLoading ? (
        <p className="text-sm text-gray-500">Carregando fotos...</p>
      ) : (
        <PhotoGrid photos={photos} onDelete={remove ? handleDelete : undefined} deletingPhotoId={deletingPhotoId} />
      )}
    </div>
  )
}
