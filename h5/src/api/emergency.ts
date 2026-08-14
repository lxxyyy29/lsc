import { http } from './http'

// ─── 应急调度指令（H5 移动端接收/反馈） ─────────────────────────────────

export interface EmergencyDispatch {
  id: number
  dispatch_no: string
  title: string
  type: string
  type_name?: string
  level: string
  level_name?: string
  grid_name?: string
  content: string
  status: string
  creator_name?: string
  dispatch_time?: string
  completed_at?: string
  meeting_url?: string
  my_status?: string
  my_feedback?: string
  myReceipt?: {
    status: string
    feedback?: string
  }
  receipts?: Array<{
    id: number
    user_id: number
    user_name?: string
    status: string
    feedback?: string
    received_at?: string
  }>
}

/** 我收到的指令列表 */
export async function getMyEmergencyDispatches(): Promise<EmergencyDispatch[]> {
  return http.get('/emergency/dispatches')
}

/** 指令详情（查看即标记已接收） */
export async function getEmergencyDispatchDetail(id: number): Promise<EmergencyDispatch> {
  return http.get(`/emergency/dispatches/${id}`)
}

/** 反馈状态：RESPONDING 响应中 / COMPLETED 已完成 */
export async function feedbackEmergencyReceipt(
  id: number,
  status: string,
  feedback?: string,
): Promise<{ status: string; feedback?: string }> {
  return http.post(`/emergency/dispatches/${id}/receipt`, { status, feedback })
}
