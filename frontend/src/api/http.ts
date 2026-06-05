const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export interface ApiResult<T> {
  success: boolean
  data: T
  message: string
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: options.body instanceof FormData ? undefined : { 'Content-Type': 'application/json' },
    ...options
  })
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  const text = await response.text()
  if (!text) {
    return undefined as T
  }
  const result = JSON.parse(text) as ApiResult<T>
  if (!result.success) {
    throw new Error(result.message || '服务处理失败')
  }
  return result.data
}

export async function downloadFile(path: string, body?: unknown, filename?: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body ?? {})
  })
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const contentDisposition = response.headers.get('Content-Disposition')
  const suggestedFilename = contentDisposition 
    ? contentDisposition.split('filename=')[1]?.replace(/"/g, '') 
    : (filename || 'export.xlsx')
  a.download = suggestedFilename
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  a.remove()
}

export async function uploadFile(path: string, formData: FormData): Promise<any> {
  return request(path, { method: 'POST', body: formData })
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body ?? {}) }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(body ?? {}) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  downloadFile,
  uploadFile
}
