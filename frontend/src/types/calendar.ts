// Calendar view modes
export type CalendarViewMode = 'day' | 'week' | 'month' | 'list';
export type CalendarStatus = 'enabled' | 'disabled';

// Calendar item - re-export from api/types
export type { CalendarItem } from '@/api/types';

// Calendar event for calendar display (internal view type)
export interface CalendarEvent {
  id: string
  calendarId: string
  title: string
  location: string
  start: string
  end: string
  attendees: string[]
  description: string
  color: string
}

// Table record for admin tables
export interface TableRecord {
  id: string
  name: string
  owner: string
  status: string
  updatedAt: string
}
