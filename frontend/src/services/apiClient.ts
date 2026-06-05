const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export interface ApiRequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

function buildUrl(path: string, params?: ApiRequestOptions['params']) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined) url.searchParams.set(key, String(value));
  });

  return url.toString();
}

async function request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { params, headers, body, ...rest } = options;
  const response = await fetch(buildUrl(path, params), {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...headers
    },
    body: body && typeof body !== 'string' ? JSON.stringify(body) : body
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `请求失败：${response.status}`);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string, options?: ApiRequestOptions) => request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: body as BodyInit }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: body as BodyInit }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' })
};
