export type CalendarViewMode = 'day' | 'week' | 'month' | 'list';
export type CalendarStatus = 'enabled' | 'disabled';

export interface CalendarItem {
  id: string;
  name: string;
  description: string;
  autoSubscribeScope: string[];
  sharedMembers: string[];
  color: string;
  status: CalendarStatus;
  owner: string;
}

export interface CalendarEvent {
  id: string;
  calendarId: string;
  title: string;
  location: string;
  start: string;
  end: string;
  attendees: string[];
  description: string;
  color: string;
}

export interface TableRecord {
  id: string;
  name: string;
  owner: string;
  status: string;
  updatedAt: string;
}
