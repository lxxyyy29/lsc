import { type CurrentWebUser, createWebSessionFromLoginResponse, persistWebSession, clearWebSession, type WebLoginResponse, type WebSession } from '../auth/session'
import { http } from './http'

export interface WebLoginPayload {
  account: string
  password: string
}

export async function loginWeb(payload: WebLoginPayload): Promise<WebSession> {
  const response = await http.post<WebLoginResponse, WebLoginResponse>('/auth/login', payload)
  const session = createWebSessionFromLoginResponse(response)
  persistWebSession(session)
  return session
}

export async function logoutWeb() {
  try {
    await http.post<void, void>('/auth/logout')
  } finally {
    clearWebSession()
  }
}

export async function fetchCurrentWebUser() {
  return http.get<CurrentWebUser, CurrentWebUser>('/auth/me')
}
