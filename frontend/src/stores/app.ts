import { defineStore } from 'pinia'
import { api } from '../api/http'
import type { CalendarItem, Department, EventItem, TagColor, User } from '../api/types'

export const useAppStore = defineStore('app', {
  state: () => ({
    users: [] as User[],
    departments: [] as Department[],
    calendars: [] as CalendarItem[],
    tags: [] as TagColor[],
    events: [] as EventItem[],
    currentUserId: 1
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
          api.get<TagColor[]>('/tags')
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
        this.events = await api.get<EventItem[]>(`/events${query}`)
      } catch (err) {
        console.warn('loadEvents failed:', err)
        this.events = []
      }
    }
  }
})
