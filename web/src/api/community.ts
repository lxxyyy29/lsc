import { http } from './http'

export interface GridTreeVo {
  id: number
  gridCode: string
  gridName: string
  gridLevel: number
  parentId: number | null
  roiJson: string
  area: number
  population: number
  buildingCount: number
  sortOrder: number
  status: string
  remark: string
  children?: GridTreeVo[]
}

export interface GridEntity {
  id?: number
  gridCode: string
  gridName: string
  gridLevel?: number
  parentId?: number | null
  roiJson?: string
  area?: number
  population?: number
  buildingCount?: number
  sortOrder?: number
  status?: string
  remark?: string
}

export function getGridTree() {
  return http.get<GridTreeVo[], GridTreeVo[]>('/community/grids/tree')
}

export function getGridDetail(id: number) {
  return http.get<GridEntity, GridEntity>(`/community/grids/${id}`)
}

export function getGridChildren(id: number) {
  return http.get<GridEntity[], GridEntity[]>(`/community/grids/${id}/children`)
}

export function createGrid(data: GridEntity) {
  return http.post<boolean, boolean>('/community/grids', data)
}

export function updateGrid(id: number, data: GridEntity) {
  return http.put<boolean, boolean>(`/community/grids/${id}`, data)
}

export function deleteGrid(id: number) {
  return http.delete<boolean, boolean>(`/community/grids/${id}`)
}

// 实有人口
export interface PopulationEntity {
  id?: number
  gridId?: number
  name: string
  idCard?: string
  phone?: string
  gender?: string
  birthday?: string
  householdType?: string
  address?: string
  buildingNo?: string
  roomNo?: string
  tags?: string
  photoUrl?: string
  status?: string
  remark?: string
}

export function listPopulation(gridId?: number) {
  return http.get<PopulationEntity[], PopulationEntity[]>('/community/population',
    gridId ? { params: { gridId } } : {})
}

export function getPopulationDetail(id: number) {
  return http.get<PopulationEntity, PopulationEntity>(`/community/population/${id}`)
}

export function createPopulation(data: PopulationEntity) {
  return http.post<boolean, boolean>('/community/population', data)
}

export function updatePopulation(id: number, data: PopulationEntity) {
  return http.put<boolean, boolean>(`/community/population/${id}`, data)
}

export function deletePopulation(id: number) {
  return http.delete<boolean, boolean>(`/community/population/${id}`)
}
