// ========== User ==========
export interface User {
  id: string
  name: string
  department_id?: string
  email?: string
  mobile?: string
  status: string
  created_at?: string
  updated_at?: string
}

// ========== Department ==========
export interface Department {
  id: string
  name: string
  parent_id?: string
  sort_order?: number
  enabled: boolean
  created_at?: string
  updated_at?: string
}

// ========== Calendar ==========
export interface CalendarItem {
  id: string
  name: string
  description?: string
  type: 'PERSONAL' | 'SHARED' | 'PUBLIC' | 'ALL_MEMBER'
  color: string
  owner_user_id?: string
  visible: boolean
  status: string
  created_at?: string
  updated_at?: string
}

// ========== Event (Schedule) ==========
export interface EventItem {
  id: string
  calendar_id: string
  tag_id?: number
  organizer_user_id: string
  title: string
  location?: string
  description?: string
  all_day: boolean
  timezone?: string
  visibility?: string
  status: string
  start_at: string
  end_at: string
  tag_name?: string
  tag_color?: string
  calendar_name?: string
  calendar_color?: string
  recurrence_rule?: string
  total_participants?: number
  created_at?: string
  updated_at?: string
}

// ========== Calendar Tag ==========
export interface CalendarTag {
  id: number
  name: string
  color: string
  enabled: boolean
  created_at?: string
}

// ========== Attachment ==========
export interface Attachment {
  id: string
  schedule_id: string
  file_name: string
  file_path: string
  file_size: number
  uploaded_by: string
  content_type?: string
  created_at?: string
  updated_at?: string
}

// ========== Export Task ==========
export interface ExportTask {
  id: string
  name: string
  scope: string
  status: string
  file_path?: string
  created_by: string
  started_at?: string
  finished_at?: string
  created_at?: string
}

// ========== System Config ==========
export interface SystemConfig {
  id: string
  config_key: string
  config_value: string
  description?: string
  updated_by?: string
  created_at?: string
  updated_at?: string
}
