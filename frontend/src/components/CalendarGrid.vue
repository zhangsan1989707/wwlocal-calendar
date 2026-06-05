<template>
  <section class="calendar-board">
    <template v-if="mode === 'month'">
      <div class="board-weekdays">
        <span v-for="index in 7" :key="index">{{ getWeekdayName(index - 1) }}</span>
      </div>
      <div class="month-grid">
        <button
          v-for="cell in monthCells"
          :key="cell.key"
          class="month-cell"
          :class="{ muted: !cell.inMonth, today: cell.isToday }"
          @click="$emit('create', cell.date)"
        >
          <span class="day-number">{{ cell.date.getDate() }}</span>
          <span v-for="event in eventsByDate[cell.key] || []" :key="event.id" class="event-chip" :style="{ background: event.color }">
            {{ event.title }}
          </span>
        </button>
      </div>
    </template>

    <template v-else-if="mode === 'week' || mode === 'day'">
      <div class="time-grid" :class="{ single: mode === 'day' }">
        <div class="time-column">
          <span v-for="hour in hours" :key="hour">{{ hour }}:00</span>
        </div>
        <div v-for="day in activeDays" :key="formatDate(day)" class="day-column">
          <header>{{ getWeekdayName(day.getDay()) }} {{ day.getMonth() + 1 }}/{{ day.getDate() }}</header>
          <button class="day-lane" @click="$emit('create', day)">
            <span
              v-for="event in eventsByDate[formatDate(day)] || []"
              :key="event.id"
              class="time-event"
              :style="{ borderColor: event.color }"
            >
              {{ event.title }}
              <small>{{ event.start.slice(11, 16) }} - {{ event.end.slice(11, 16) }}</small>
            </span>
          </button>
        </div>
      </div>
    </template>

    <div v-else class="event-list">
      <button v-for="event in orderedEvents" :key="event.id" class="event-row">
        <i :style="{ background: event.color }"></i>
        <strong>{{ event.title }}</strong>
        <span>{{ event.start.replace('T', ' ') }} - {{ event.end.slice(11, 16) }}</span>
        <em>{{ event.location }}</em>
      </button>
      <el-empty v-if="orderedEvents.length === 0" description="暂无日程" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { CalendarEvent, CalendarViewMode } from '@/types/calendar';
import { addDays, formatDate, getMonthCells, getWeekdayName, startOfWeek } from '@/utils/date';

const props = defineProps<{
  date: Date;
  mode: CalendarViewMode;
  events: CalendarEvent[];
}>();

defineEmits<{ create: [date: Date] }>();

const hours = Array.from({ length: 11 }, (_, index) => index + 8);
const monthCells = computed(() => getMonthCells(props.date));
const activeDays = computed(() => {
  if (props.mode === 'day') return [props.date];
  const start = startOfWeek(props.date);
  return Array.from({ length: 7 }, (_, index) => addDays(start, index));
});
const orderedEvents = computed(() => [...props.events].sort((a, b) => a.start.localeCompare(b.start)));
const eventsByDate = computed(() =>
  props.events.reduce<Record<string, CalendarEvent[]>>((result, event) => {
    const key = event.start.slice(0, 10);
    result[key] = result[key] || [];
    result[key].push(event);
    return result;
  }, {})
);
</script>
