import { defineStore } from 'pinia';
import type { CalendarEvent, CalendarItem, CalendarViewMode } from '@/types/calendar';

const calendars: CalendarItem[] = [];

const events: CalendarEvent[] = [];

export const useCalendarStore = defineStore('calendar', {
  state: () => ({
    currentDate: new Date('2026-06-05T09:00:00'),
    viewMode: 'month' as CalendarViewMode,
    calendars,
    events,
    selectedCalendarIds: calendars.map((item) => item.id)
  }),
  getters: {
    visibleEvents(state) {
      return state.events.filter((event) => state.selectedCalendarIds.includes(event.calendarId));
    }
  },
  actions: {
    setViewMode(mode: CalendarViewMode) {
      this.viewMode = mode;
    },
    toggleCalendar(id: string) {
      this.selectedCalendarIds = this.selectedCalendarIds.includes(id)
        ? this.selectedCalendarIds.filter((item) => item !== id)
        : [...this.selectedCalendarIds, id];
    },
    saveEvent(event: CalendarEvent) {
      console.warn('calendar store saveEvent is deprecated, use appStore.loadEvents() + api instead')
    },
    saveCalendar(calendar: CalendarItem) {
      console.warn('calendar store saveCalendar is deprecated, use appStore.loadBase() + api instead')
    }
  }
});
