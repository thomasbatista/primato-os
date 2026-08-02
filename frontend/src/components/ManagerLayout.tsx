import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

const NAV_LINKS = [
  { label: 'Obras', to: '/manager/projects' },
  { label: 'Ordens de Serviço', to: '/manager/work-orders' },
  { label: 'Checklists Diários', to: '/manager/daily-reports' },
  { label: 'Pedidos de Materiais', to: '/manager/material-requests' },
]

export function ManagerLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-primary text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <Link to="/manager" className="text-lg font-semibold tracking-wide">
            Primato OS
          </Link>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden sm:inline">{user?.name}</span>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-md border border-white/30 px-3 py-1.5 transition hover:bg-white/10"
            >
              Sair
            </button>
          </div>
        </div>
      </header>

      <nav className="border-b border-muted bg-white">
        <div className="mx-auto flex max-w-6xl gap-1 overflow-x-auto px-4 sm:px-6">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `whitespace-nowrap border-b-2 px-3 py-3 text-sm font-medium transition ${
                  isActive
                    ? 'border-accent-dark text-accent-dark'
                    : 'border-transparent text-gray-500 hover:text-foreground'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>
      </nav>

      <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6">
        <Outlet />
      </main>
    </div>
  )
}
