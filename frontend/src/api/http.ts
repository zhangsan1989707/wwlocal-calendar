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

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body ?? {}) }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(body ?? {}) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' })
}
