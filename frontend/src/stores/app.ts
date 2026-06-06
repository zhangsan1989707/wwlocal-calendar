import { defineStore } from 'pinia'
import { api } from '../api/http'
import type { User, Department, CalendarItem, CalendarTag, EventItem } from '../api/types'

export const useAppStore = defineStore('app', {
  state: () => ({
    users: [] as User[],
    departments: [] as Department[],
    calendars: [] as CalendarItem[],
    tags: [] as CalendarTag[],
    events: [] as EventItem[],
    currentUserId: '' as string
  }),
  getters: {
    currentUser: (state) => state.users.find((item) => item.id === state.currentUserId)
  },
  actions: {
    async loadBase() {
      try {
        const [users, departments, calendars, tags] = await Promise.all([
          api.get<User[]>('/users'),
          api.get<Department[]>('/departments'),
          api.get<CalendarItem[]>('/calendars'),
          api.get<CalendarTag[]>('/tags')
        ])
        this.users = users
        this.departments = departments
        this.calendars = calendars
        this.tags = tags
      } catch {
        this.users = []
        this.departments = []
        this.calendars = []
        this.tags = []
      }
      if (!this.users.some((item) => item.id === this.currentUserId) && this.users[0]) {
        this.currentUserId = this.users[0].id
      }
    },
    async loadEvents(query = '') {
      try {
        // 后端已默认按当前用户过滤；显式传 currentUserId 以兼容非默认分支
        const separator = query.includes('?') ? '&' : '?'
        const userIdQuery = `userId=${encodeURIComponent(this.currentUserId)}`
        const allUsersQuery = 'allUsers=true'
        const finalQuery = query
          ? `${query}&${userIdQuery}&${allUsersQuery}`
          : `?${userIdQuery}&${allUsersQuery}`
        this.events = await api.get<EventItem[]>(`/events${finalQuery}`)
      } catch (err) {
        console.warn('loadEvents failed:', err)
        this.events = []
      }
    }
  }
})
