import { Link } from 'react-router-dom'

export interface DashboardCardItem {
  id: number
  href: string
  label: string
  sublabel?: string
  highlight?: boolean
}

interface DashboardSectionCardProps {
  title: string
  count: number
  items: DashboardCardItem[]
  emptyMessage: string
  viewAllHref: string
  itemsLayout?: 'list' | 'grid'
  className?: string
}

export function DashboardSectionCard({
  title,
  count,
  items,
  emptyMessage,
  viewAllHref,
  itemsLayout = 'list',
  className = '',
}: DashboardSectionCardProps) {
  return (
    <div className={`flex flex-col rounded-lg border border-muted bg-white p-5 shadow-sm ${className}`}>
      <h2 className="text-sm font-medium text-gray-500">{title}</h2>
      <p className="mt-1 text-3xl font-semibold text-primary">{count}</p>

      {items.length === 0 ? (
        <p className="mt-4 text-sm text-gray-500">{emptyMessage}</p>
      ) : (
        <ul
          className={`mt-4 gap-1 ${itemsLayout === 'grid' ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3' : 'flex flex-col'}`}
        >
          {items.map((item) => (
            <li key={item.id}>
              <Link
                to={item.href}
                className="flex items-center justify-between gap-2 rounded-md px-2 py-2 text-sm transition hover:bg-background"
              >
                <span className="min-w-0">
                  <span className="block truncate font-medium text-foreground">{item.label}</span>
                  {item.sublabel && (
                    <span className="block truncate text-xs text-gray-500">{item.sublabel}</span>
                  )}
                </span>
                {item.highlight && (
                  <span className="shrink-0 rounded-full bg-accent px-2 py-0.5 text-xs font-medium text-foreground">
                    Urgente
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}

      {count > items.length && (
        <Link to={viewAllHref} className="mt-3 text-sm font-medium text-primary hover:underline">
          Ver todos ({count})
        </Link>
      )}
    </div>
  )
}
