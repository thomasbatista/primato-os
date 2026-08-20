import api from './api'
import type {
  Page,
  ProjectCreateRequest,
  ProjectPhotoResponse,
  ProjectResponse,
  ProjectStatus,
  ProjectUpdateRequest,
} from '../types'

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

// GET /projects/{id} is manager-only — workers reach a project through this sibling endpoint,
// which checks they are assigned to a work order in it.
export async function getMyProject(id: number): Promise<ProjectResponse> {
  const response = await api.get<ProjectResponse>(`/projects/mine/${id}`)
  return response.data
}

export async function getProjectPhotos(id: number): Promise<ProjectPhotoResponse[]> {
  const response = await api.get<ProjectPhotoResponse[]>(`/projects/${id}/photos`)
  return response.data
}

export async function uploadProjectPhoto(id: number, file: File): Promise<ProjectPhotoResponse> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<ProjectPhotoResponse>(`/projects/${id}/photos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export async function deleteProjectPhoto(id: number, photoId: number): Promise<void> {
  await api.delete(`/projects/${id}/photos/${photoId}`)
}
