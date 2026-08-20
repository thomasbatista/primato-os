import { useState } from 'react'

// Structural, not tied to DailyReportPhotoResponse — project and work order photos share
// this shape and reuse this grid.
interface Photo {
  id: number
  url: string
}

interface PhotoGridProps {
  photos: Photo[]
  onDelete?: (photo: Photo) => void
  deletingPhotoId?: number | null
}

export function PhotoGrid({ photos, onDelete, deletingPhotoId }: PhotoGridProps) {
  const [expandedPhoto, setExpandedPhoto] = useState<Photo | null>(null)

  if (photos.length === 0) {
    return <p className="text-sm text-gray-500">Nenhuma foto anexada.</p>
  }

  return (
    <>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6">
        {photos.map((photo) => (
          <div key={photo.id} className="group relative aspect-square overflow-hidden rounded-md border border-muted">
            <button
              type="button"
              onClick={() => setExpandedPhoto(photo)}
              className="h-full w-full focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <img src={photo.url} alt="Foto" className="h-full w-full object-cover" />
            </button>
            {onDelete && (
              <button
                type="button"
                disabled={deletingPhotoId === photo.id}
                onClick={() => onDelete(photo)}
                title="Remover foto"
                className="absolute top-1 right-1 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-xs font-bold text-white transition hover:bg-black/80 disabled:cursor-not-allowed disabled:opacity-50"
              >
                ×
              </button>
            )}
          </div>
        ))}
      </div>

      {expandedPhoto && (
        <div
          role="dialog"
          aria-modal="true"
          onClick={() => setExpandedPhoto(null)}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
        >
          <img
            src={expandedPhoto.url}
            alt="Foto em tamanho ampliado"
            className="max-h-full max-w-full rounded-md object-contain"
          />
          <button
            type="button"
            onClick={() => setExpandedPhoto(null)}
            aria-label="Fechar"
            className="absolute top-4 right-4 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-lg font-bold text-foreground"
          >
            ×
          </button>
        </div>
      )}
    </>
  )
}
