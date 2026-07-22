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

// 房屋
export interface BuildingEntity {
  id?: number
  gridId?: number
  buildingNo: string
  address?: string
  householdCount?: number
  landlordName?: string
  landlordPhone?: string
  fireRiskLevel?: string
  isGroupRental?: number
  status?: string
  remark?: string
}

export function listBuildings(gridId?: number) {
  return http.get<BuildingEntity[], BuildingEntity[]>('/community/buildings',
    gridId ? { params: { gridId } } : {})
}
export function getBuildingDetail(id: number) {
  return http.get<BuildingEntity, BuildingEntity>(`/community/buildings/${id}`)
}
export function createBuilding(data: BuildingEntity) {
  return http.post<boolean, boolean>('/community/buildings', data)
}
export function updateBuilding(id: number, data: BuildingEntity) {
  return http.put<boolean, boolean>(`/community/buildings/${id}`, data)
}
export function deleteBuilding(id: number) {
  return http.delete<boolean, boolean>(`/community/buildings/${id}`)
}

// 场所
export interface PlaceEntity {
  id?: number
  gridId?: number
  placeName: string
  placeType?: string
  address?: string
  contactName?: string
  contactPhone?: string
  fireFacilities?: string
  riskTags?: string
  longitude?: number
  latitude?: number
  status?: string
  remark?: string
}

export function listPlaces(gridId?: number) {
  return http.get<PlaceEntity[], PlaceEntity[]>('/community/places',
    gridId ? { params: { gridId } } : {})
}
export function getPlaceDetail(id: number) {
  return http.get<PlaceEntity, PlaceEntity>(`/community/places/${id}`)
}
export function createPlace(data: PlaceEntity) {
  return http.post<boolean, boolean>('/community/places', data)
}
export function updatePlace(id: number, data: PlaceEntity) {
  return http.put<boolean, boolean>(`/community/places/${id}`, data)
}
export function deletePlace(id: number) {
  return http.delete<boolean, boolean>(`/community/places/${id}`)
}

// 组织力量
export interface OrgMemberEntity {
  id?: number
  gridId?: number
  sysUserId?: number
  memberType?: string
  name: string
  phone?: string
  status?: string
  remark?: string
}

export function listOrgMembers(gridId?: number) {
  return http.get<OrgMemberEntity[], OrgMemberEntity[]>('/community/org-members',
    gridId ? { params: { gridId } } : {})
}
export function getOrgMemberDetail(id: number) {
  return http.get<OrgMemberEntity, OrgMemberEntity>(`/community/org-members/${id}`)
}
export function createOrgMember(data: OrgMemberEntity) {
  return http.post<boolean, boolean>('/community/org-members', data)
}
export function updateOrgMember(id: number, data: OrgMemberEntity) {
  return http.put<boolean, boolean>(`/community/org-members/${id}`, data)
}
export function deleteOrgMember(id: number) {
  return http.delete<boolean, boolean>(`/community/org-members/${id}`)
}

// BI 看板
export function getDashboardOverview() {
  return http.get<any, any>('/community/dashboard/overview')
}

export function getGridStats() {
  return http.get<any, any>('/community/dashboard/grid-stats')
}
