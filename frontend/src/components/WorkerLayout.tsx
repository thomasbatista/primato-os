import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function WorkerLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-primary text-white">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3">
          <Link to="/worker" className="text-lg font-semibold tracking-wide">
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
        <nav className="mx-auto flex max-w-2xl gap-4 px-4 pb-3 text-sm">
          <Link to="/worker" className="text-white/90 hover:text-white">
            Minhas OS
          </Link>
          <Link to="/worker/daily-reports" className="text-white/90 hover:text-white">
            Meus Checklists
          </Link>
        </nav>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
