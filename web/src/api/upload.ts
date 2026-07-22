import { http } from './http'

export interface UploadedFile {
  objectName: string
  name: string
  size: string
}

let cachedAccessPrefix: string | null = null

export async function fetchAccessPrefix(): Promise<string> {
  if (cachedAccessPrefix !== null) return cachedAccessPrefix
  const response = await fetch('/api/upload/access-prefix?clientType=WEB', {
    headers: {
      Authorization: `Bearer ${JSON.parse(localStorage.getItem('dgcp-oa-web-session') || '{}').token || ''}`
    }
  })
  const payload = await response.json()
  if (!response.ok || !payload?.success) {
    throw new Error(payload?.message || '获取访问前缀失败')
  }
  cachedAccessPrefix = payload.data.accessPrefix || ''
  return cachedAccessPrefix!
}

export function toImageUrl(objectName: string | null | undefined): string {
  if (!objectName) return ''
  // Already a full URL (legacy data) — extract path to go through proxy
  if (objectName.startsWith('http://') || objectName.startsWith('https://')) {
    try {
      return new URL(objectName).pathname
    } catch {
      return objectName
    }
  }
  // Prepend cached access prefix, then extract path for proxy
  const prefix = cachedAccessPrefix || ''
  const url = prefix + objectName
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      return new URL(url).pathname
    } catch {
      return url
    }
  }
  return url
}

export async function uploadFile(file: File, bizType = 'common'): Promise<UploadedFile> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  formData.append('clientType', 'WEB')
  const response = await fetch('/api/upload', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${JSON.parse(localStorage.getItem('dgcp-oa-web-session') || '{}').token || ''}`
    },
    body: formData
  })
  const payload = await response.json()
  if (!response.ok || !payload?.success) {
    throw new Error(payload?.message || '上传失败')
  }
  return payload.data as UploadedFile
}

export async function deleteUploadedFile(objectNameOrUrl: string): Promise<boolean> {
  return await http.delete<boolean, boolean>('/upload', {
    params: {
      url: objectNameOrUrl,
      clientType: 'WEB'
    }
  } as any)
}

export function buildDownloadUrl(url: string, fileName?: string) {
  const params = new URLSearchParams({ url, clientType: 'WEB' })
  if (fileName) {
    params.set('fileName', fileName)
  }
  return `/api/upload/download?${params.toString()}`
}
