import { useEffect, useState } from 'react'
import { DashboardSectionCard, type DashboardCardItem } from '../../components/DashboardSectionCard'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { getDashboard } from '../../services/dashboardService'
import type { DashboardResponse } from '../../types'

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

export function ManagerDashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadDashboard()
  }, [])

  async function loadDashboard() {
    setIsLoading(true)
    setError(null)

    try {
      setDashboard(await getDashboard())
    } catch {
      setError('Não foi possível carregar o painel. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (error || !dashboard) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p>{error ?? 'Não foi possível carregar o painel.'}</p>
        <button type="button" onClick={loadDashboard} className="mt-2 font-medium underline">
          Tentar novamente
        </button>
      </div>
    )
  }

  const dailyReportItems: DashboardCardItem[] = dashboard.pendingDailyReports.items.map((report) => ({
    id: report.id,
    href: `/manager/daily-reports/${report.id}`,
    label: `OS Nº ${report.workOrder.orderNumber} — ${formatDate(report.date)}`,
    sublabel: report.filledByWorker.name,
  }))

  const workOrderItems: DashboardCardItem[] = dashboard.todayWorkOrders.items.map((workOrder) => ({
    id: workOrder.id,
    href: `/manager/work-orders/${workOrder.id}`,
    label: `OS Nº ${workOrder.orderNumber}`,
    sublabel: workOrder.stage,
  }))

  const openMaterialRequestItems: DashboardCardItem[] = dashboard.openMaterialRequests.items.map(
    (request) => ({
      id: request.id,
      href: `/manager/material-requests/${request.id}`,
      label: `Pedido Nº ${request.requestNumber}`,
      sublabel: request.project.name,
      highlight: request.priority === 'URGENT',
    }),
  )

  const awaitingDeliveryItems: DashboardCardItem[] = dashboard.materialRequestsAwaitingDelivery.items.map(
    (request) => ({
      id: request.id,
      href: `/manager/material-requests/${request.id}`,
      label: `Pedido Nº ${request.requestNumber}`,
      sublabel: request.project.name,
      highlight: request.priority === 'URGENT',
    }),
  )

  const projectItems: DashboardCardItem[] = dashboard.activeProjects.items.map((project) => ({
    id: project.id,
    href: `/manager/projects/${project.id}`,
    label: project.name,
    sublabel: project.client,
  }))

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-foreground">Painel</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <DashboardSectionCard
          title="Checklists Pendentes"
          count={dashboard.pendingDailyReports.count}
          items={dailyReportItems}
          emptyMessage="Nenhum checklist pendente"
          viewAllHref="/manager/daily-reports"
        />
        <DashboardSectionCard
          title="Ordens de Serviço de Hoje"
          count={dashboard.todayWorkOrders.count}
          items={workOrderItems}
          emptyMessage="Nenhuma OS para hoje"
          viewAllHref="/manager/work-orders"
        />
        <DashboardSectionCard
          title="Pedidos em Aberto"
          count={dashboard.openMaterialRequests.count}
          items={openMaterialRequestItems}
          emptyMessage="Nenhum pedido em aberto"
          viewAllHref="/manager/material-requests"
        />
        <DashboardSectionCard
          title="Aguardando Entrega"
          count={dashboard.materialRequestsAwaitingDelivery.count}
          items={awaitingDeliveryItems}
          emptyMessage="Nenhum pedido aguardando entrega"
          viewAllHref="/manager/material-requests"
        />
      </div>

      <DashboardSectionCard
        title="Obras Ativas"
        count={dashboard.activeProjects.count}
        items={projectItems}
        emptyMessage="Nenhuma obra ativa no momento"
        viewAllHref="/manager/projects"
        itemsLayout="grid"
      />
    </div>
  )
}
