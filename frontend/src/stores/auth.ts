import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

interface AuthState {
  token: string | null
  userId: string | null
  username: string | null
  displayName: string | null
  role: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const state = ref<AuthState>({
    token: localStorage.getItem('token'),
    userId: localStorage.getItem('userId'),
    username: localStorage.getItem('username'),
    displayName: localStorage.getItem('displayName'),
    role: localStorage.getItem('role')
  })

  const isLoggedIn = computed(() => !!state.value.token)
  const isAdmin = computed(() => state.value.role === 'admin')

  async function login(username: string, password: string) {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '登录失败')
    }
    const d = data.data
    state.value.token = d.token
    state.value.userId = d.userId
    state.value.username = d.username
    state.value.displayName = d.displayName
    state.value.role = d.role

    localStorage.setItem('token', d.token)
    localStorage.setItem('userId', d.userId)
    localStorage.setItem('username', d.username)
    localStorage.setItem('displayName', d.displayName)
    localStorage.setItem('role', d.role)
  }

  async function register(username: string, password: string, displayName: string) {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, displayName })
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '注册失败')
    }
    return data
  }

  function logout() {
    state.value = { token: null, userId: null, username: null, displayName: null, role: null }
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
    localStorage.removeItem('role')
  }

  function getAuthHeaders(): Record<string, string> {
    if (state.value.token) {
      return { Authorization: `Bearer ${state.value.token}` }
    }
    return {}
  }

  return { state, isLoggedIn, isAdmin, login, register, logout, getAuthHeaders }
})
