import { defineStore } from 'pinia';
import type { CalendarEvent, CalendarItem, CalendarViewMode } from '@/types/calendar';

const calendars: CalendarItem[] = [
  {
    id: 'cal-sales',
    name: '销售中心日历',
    description: '销售中心客户拜访与内部协作安排',
    autoSubscribeScope: ['销售中心'],
    sharedMembers: ['李宇航', '周明'],
    color: '#2f7cf6',
    status: 'enabled',
    owner: '李宇航'
  },
  {
    id: 'cal-ops',
    name: '运营排期',
    description: '运营活动、内容发布与复盘节奏',
    autoSubscribeScope: ['运营部'],
    sharedMembers: ['陈晓', '王宁'],
    color: '#f0b429',
    status: 'enabled',
    owner: '陈晓'
  }
];

const events: CalendarEvent[] = [
  {
    id: 'evt-1',
    calendarId: 'cal-sales',
    title: '客户方案评审',
    location: '会议室 A',
    start: '2026-06-08T10:00',
    end: '2026-06-08T11:30',
    attendees: ['李宇航', '周明'],
    description: '确认客户方案与后续推进事项',
    color: '#2f7cf6'
  },
  {
    id: 'evt-2',
    calendarId: 'cal-ops',
    title: '六月活动排期会',
    location: '线上会议',
    start: '2026-06-12T14:00',
    end: '2026-06-12T15:00',
    attendees: ['陈晓', '王宁'],
    description: '同步本月重点活动计划',
    color: '#f0b429'
  }
];

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
      const index = this.events.findIndex((item) => item.id === event.id);
      if (index >= 0) this.events[index] = event;
      else this.events.push(event);
    },
    saveCalendar(calendar: CalendarItem) {
      const index = this.calendars.findIndex((item) => item.id === calendar.id);
      if (index >= 0) this.calendars[index] = calendar;
      else {
        this.calendars.push(calendar);
        this.selectedCalendarIds.push(calendar.id);
      }
    }
  }
});
