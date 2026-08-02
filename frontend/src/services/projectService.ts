import api from './api'
import type { Page, ProjectCreateRequest, ProjectResponse, ProjectStatus, ProjectUpdateRequest } from '../types'

interface GetProjectsParams {
  status?: ProjectStatus
  page?: number
  size?: number
}

export async function getProjects(params: GetProjectsParams = {}): Promise<Page<ProjectResponse>> {
  const response = await api.get<Page<ProjectResponse>>('/projects', { params })
  return response.data
}

export async function getProject(id: number): Promise<ProjectResponse> {
  const response = await api.get<ProjectResponse>(`/projects/${id}`)
  return response.data
}

export async function createProject(request: ProjectCreateRequest): Promise<ProjectResponse> {
  const response = await api.post<ProjectResponse>('/projects', request)
  return response.data
}

export async function updateProject(id: number, request: ProjectUpdateRequest): Promise<ProjectResponse> {
  const response = await api.put<ProjectResponse>(`/projects/${id}`, request)
  return response.data
}
