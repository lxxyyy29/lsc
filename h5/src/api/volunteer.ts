import { http } from './http'

export interface VolunteerActivity {
  id: number
  title: string
  description: string | null
  /** 接口返回下划线字段 activity_date */
  activity_date: string
  max_participants: number | null
  status: 'PLANNED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'
  signedUpCount: number
  signedUp: boolean
}

export interface VolunteerPoints {
  totalPoints: number
  availablePoints: number
}

export interface VolunteerPointsLog {
  points: number
  reason: string | null
  sourceType: string | null
  createdAt: string
}

export interface VolunteerPointsResponse {
  account: VolunteerPoints
  logs: VolunteerPointsLog[]
}

export function getVolunteerActivities() {
  return http.get<VolunteerActivity[], VolunteerActivity[]>('/activities')
}

export function signupVolunteerActivity(activityId: number) {
  return http.post<boolean, boolean>(`/activities/${activityId}/signup`, {})
}

export function cancelVolunteerActivitySignup(activityId: number) {
  return http.delete<boolean, boolean>(`/activities/${activityId}/signup`)
}

export function getMyVolunteerPoints() {
  return http.get<VolunteerPointsResponse, VolunteerPointsResponse>('/points')
}
