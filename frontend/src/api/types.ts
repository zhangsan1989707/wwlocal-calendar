export interface User {
  id: number
  name: string
  department_id?: number
  email?: string
  phone?: string
  avatar_color?: string
  status: string
}

export interface Department {
  id: number
  name: string
  parent_id?: number
  sort_order?: number
  status: string
}

export interface CalendarItem {
  id: number
  name: string
  description?: string
  type: 'PERSONAL' | 'SHARED' | 'PUBLIC' | 'ALL_MEMBER'
  color: string
  owner_user_id?: number
  visibility?: string
  status: string
  scopes?: unknown
  shared_members?: unknown
}

export interface EventItem {
  id: number
  calendar_id: number
  organizer_user_id: number
  title: string
  location?: string
  description?: string
  tag_id?: number
  tag_name?: string
  tag_color?: string
  start_at: string
  end_at: string
  all_day: boolean
  timezone?: string
  visibility?: string
  calendar_name?: string
  calendar_color?: string
  status: string
}

export interface TagColor {
  id: number
  name: string
  color: string
  sort_order: number
  status: string
}
