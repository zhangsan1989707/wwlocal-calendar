<template>
  <div class="calendar-app">
    <header class="calendar-window-bar desktop-calendar">
      <div class="header-brand">
        <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
          <rect width="22" height="22" rx="6" fill="var(--calendar-primary)"/>
          <rect x="4" y="5" width="14" height="2" rx="1" fill="white"/>
          <rect x="4" y="9" width="10" height="2" rx="1" fill="white" opacity="0.7"/>
          <rect x="4" y="13" width="12" height="2" rx="1" fill="white" opacity="0.5"/>
          <rect x="4" y="17" width="8" height="2" rx="1" fill="white" opacity="0.3"/>
        </svg>
        <strong>日程</strong>
      </div>
    </header>

    <main class="calendar-workspace desktop-calendar">
      <aside class="calendar-sidebar">
        <router-link class="search-box" to="/calendar/search">
          <el-icon><Search /></el-icon>
          <span>搜索日程</span>
        </router-link>

        <section class="mini-calendar">
          <div class="mini-header">
            <strong>{{ monthTitle }}</strong>
            <span class="mini-nav">
              <el-button text :icon="ArrowUp" @click="moveMonth(-1)" />
              <el-button text :icon="ArrowDown" @click="moveMonth(1)" />
            </span>
          </div>
          <div class="mini-weekdays">
            <span v-for="item in weekdays" :key="item">{{ item }}</span>
          </div>
          <div class="mini-grid">
            <button
              v-for="day in monthDays"
              :key="day.key"
              class="mini-day"
              :class="{ active: isSameDate(day.date, selectedDate), muted: !day.currentMonth, today: isSameDate(day.date, today) }"
              @click="selectedDate = day.date"
            >
              {{ day.date.getDate() }}
            </button>
          </div>
        </section>

        <section class="calendar-groups">
          <div class="group-title">
            <strong>我的日历</strong>
            <el-button type="primary" plain :icon="Plus" @click="() => openCreate()" />
          </div>
          <div class="group-block">
            <label v-for="item in personalCalendars" :key="item.id" class="calendar-check">
              <input v-model="visibleCalendarIds" type="checkbox" :value="item.id" />
              <i :style="{ background: item.color }" />
              <span>{{ item.name }}</span>
            </label>
          </div>
          <div v-if="sharedCalendars.length" class="group-title group-title-split">
            <strong>共享日历</strong>
          </div>
          <div v-if="sharedCalendars.length" class="group-block">
            <label v-for="item in sharedCalendars" :key="item.id" class="calendar-check">
              <input v-model="visibleCalendarIds" type="checkbox" :value="item.id" />
              <i :style="{ background: item.color }" />
              <span>{{ item.name }}</span>
            </label>
          </div>
        </section>
      </aside>

      <section class="calendar-stage">
        <div class="calendar-toolbar">
          <el-button type="primary" :icon="Plus" @click="() => openCreate()">新建日程</el-button>
          <div class="period-title">
            <el-button text :icon="ArrowLeft" @click="move(-1)" />
            <strong>{{ stageTitle }}</strong>
            <el-button text :icon="ArrowRight" @click="move(1)" />
          </div>
          <div class="view-actions">
            <el-button @click="goToday" class="today-btn">今天</el-button>
            <div class="view-switch" role="group" aria-label="日历视图">
              <button v-for="option in viewOptions" :key="option.value" type="button" :class="{ active: view === option.value }" @click="view = option.value">
                {{ option.label }}
              </button>
            </div>
            <el-button :icon="Download" @click="exportEvents" />
          </div>
        </div>

        <div v-if="view === 'month'" class="month-board">
          <div class="weekday-row">
            <span v-for="(item, i) in weekdayLabels" :key="item"><em>{{ item }}</em></span>
          </div>
          <div class="month-grid-view">
            <button
              v-for="day in monthDays"
              :key="day.key"
              class="month-cell-view"
              :class="{ muted: !day.currentMonth }"
              @click="openCreate(day.date)"
            >
              <div class="date-wrap" :class="{ today: isSameDate(day.date, today) }">
                <strong>{{ day.date.getDate() }}</strong>
                <small>{{ lunarText(day.date) }}</small>
              </div>
              <div class="cell-events">
                <span
                  v-for="event in eventsByDay(day.date)"
                  :key="event.id"
                  class="event-pill"
                  :style="{ background: event.calendar_color || event.tag_color || 'var(--calendar-primary)' }"
                  @click.stop="selectEvent(event)"
                >
                  {{ event.title }}
                </span>
              </div>
            </button>
          </div>
        </div>

        <div v-else class="agenda-board" :class="{ single: view === 'day' }">
          <div class="day-header-row">
            <span />
            <button
              v-for="day in agendaDays"
              :key="day.toISOString()"
              :class="{ active: isSameDate(day, selectedDate) }"
              @click="openCreate(day)"
            >
              <span>{{ weekdayShortLabels[day.getDay()] }}</span>
              <strong>{{ day.getDate() }}</strong>
              <small>{{ lunarText(day) }}</small>
            </button>
          </div>
          <div class="time-grid-view">
            <div class="time-axis">
              <span v-for="hour in hours" :key="hour">{{ hour }}:00</span>
            </div>
            <div v-for="day in agendaDays" :key="day.toISOString()" class="day-lane-view">
              <button
                v-for="hour in hours"
                :key="hour"
                class="time-slot"
                type="button"
                :aria-label="`${day.getMonth() + 1}月${day.getDate()}日 ${hour}:00 新建日程`"
                @click="openCreate(day, hour)"
              />
              <button
                v-for="event in eventsByDay(day)"
                :key="event.id"
                class="timed-event"
                :style="eventStyle(event)"
                @click.stop="selectEvent(event)"
              >
                <strong>{{ event.title }}</strong>
                <small>{{ timeText(event) }}</small>
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <main class="mobile-calendar">
      <section v-if="!mobileSettingsVisible" class="mobile-main">
        <header class="mobile-topbar">
          <div class="mobile-brand">
            <svg width="20" height="20" viewBox="0 0 22 22" fill="none">
              <rect width="22" height="22" rx="6" fill="var(--calendar-primary)"/>
              <rect x="4" y="5" width="14" height="2" rx="1" fill="white"/>
              <rect x="4" y="9" width="10" height="2" rx="1" fill="white" opacity="0.7"/>
              <rect x="4" y="13" width="12" height="2" rx="1" fill="white" opacity="0.5"/>
            </svg>
          </div>
          <strong>{{ monthTitle }}</strong>
          <div class="mobile-actions">
            <router-link class="mobile-icon-btn" to="/calendar/search">
              <el-icon><Search /></el-icon>
            </router-link>
            <button class="mobile-icon-btn" type="button" @click="() => openCreate()">
              <el-icon><Plus /></el-icon>
            </button>
            <button class="mobile-icon-btn" type="button" @click="goToday">
              <span class="today-dot">今</span>
            </button>
          </div>
        </header>

        <div class="mobile-week-nav">
          <el-button text :icon="ArrowLeft" @click="moveMonth(-1)" />
          <span>{{ monthTitle }}</span>
          <el-button text :icon="ArrowRight" @click="moveMonth(1)" />
        </div>

        <section class="mobile-month">
          <div class="mobile-weekdays">
            <span v-for="item in weekdays" :key="item">{{ item }}</span>
          </div>
          <button
            v-for="day in mobileMonthDays"
            :key="day.key"
            class="mobile-day"
            :class="{ active: isSameDate(day.date, selectedDate), muted: !day.currentMonth }"
            @click="openCreate(day.date)"
          >
            <strong>{{ day.date.getDate() === 1 ? `${day.date.getMonth() + 1}月` : day.date.getDate() }}</strong>
            <span>{{ lunarText(day.date) }}</span>
            <i v-if="eventsByDay(day.date).length" />
          </button>
        </section>

        <section class="mobile-agenda">
          <div class="mobile-agenda-title">
            <div class="mobile-date-info">
              <strong>{{ selectedDate.getMonth() + 1 }}月{{ selectedDate.getDate() }}日</strong>
              <span>{{ weekdayLabels[selectedDate.getDay()] }}</span>
            </div>
            <button type="button" @click="mobileSettingsVisible = true">
              <el-icon><Setting /></el-icon>
              设置
            </button>
          </div>
          <button class="mobile-new-row" type="button" @click="() => openCreate()">
            <el-icon><Plus /></el-icon>
            新建日程
          </button>
          <div v-if="eventsByDay(selectedDate).length === 0" class="mobile-empty-day">
            <span>今日无日程</span>
          </div>
          <button
            v-for="event in eventsByDay(selectedDate)"
            :key="event.id"
            class="mobile-event-row"
            type="button"
            @click="selectEvent(event)"
          >
            <i :style="{ background: event.calendar_color || event.tag_color || 'var(--calendar-primary)' }" />
            <span class="mobile-event-title">{{ event.title }}</span>
            <small>{{ timeText(event) }}</small>
          </button>
        </section>
      </section>

      <section v-else class="mobile-settings">
        <header class="mobile-settings-header">
          <button class="mobile-icon-button" type="button" @click="mobileSettingsVisible = false">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <strong>日程设置</strong>
        </header>

        <div class="mobile-view-card">
          <button :class="{ active: mobileViewMode === 'day' }" type="button" @click="mobileViewMode = 'day'">
            <span class="mode-art day" />
            <strong>日</strong>
          </button>
          <button :class="{ active: mobileViewMode === 'three' }" type="button" @click="mobileViewMode = 'three'">
            <span class="mode-art three" />
            <strong>三日</strong>
          </button>
          <button :class="{ active: mobileViewMode === 'week' }" type="button" @click="mobileViewMode = 'week'">
            <span class="mode-art week" />
            <strong>周</strong>
          </button>
        </div>

        <button class="mobile-add-calendar" type="button" @click="mobileCalendarSheetVisible = true">
          <span>＋</span>
          添加日历
        </button>

        <section class="mobile-calendar-card">
          <p>我的日历</p>
          <div v-for="item in personalCalendars" :key="item.id" class="mobile-calendar-line">
            <i :style="{ background: item.color }" />
            <div>
              <strong>{{ item.name }}</strong>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </section>

        <section class="mobile-calendar-card">
          <p>共享给我的日历</p>
          <div v-for="item in sharedCalendars" :key="item.id" class="mobile-calendar-line">
            <i :style="{ background: item.color }" />
            <div>
              <strong>{{ item.name }}</strong>
              <small>由成员共享</small>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </section>

        <div v-if="mobileCalendarSheetVisible" class="mobile-sheet-mask" @click.self="mobileCalendarSheetVisible = false">
          <section class="mobile-add-sheet">
            <h2>添加日历</h2>
            <button type="button" @click="mobileCalendarSheetVisible = false">
              <span class="sheet-icon personal" />
              <div>
                <strong>个人/团队日历</strong>
                <small>管理个人工作安排，也可共享给团队成员协作</small>
              </div>
            </button>
            <button type="button" @click="mobileCalendarSheetVisible = false">
              <span class="sheet-icon public" />
              <div>
                <strong>订阅公共日历</strong>
                <small>统一安排单位日程，如：员工培训、放假安排</small>
              </div>
            </button>
          </section>
        </div>
      </section>
    </main>

    <EventForm
      v-model="formVisible"
      :event="editingEvent"
      :calendars="formCalendars"
      :users="store.users"
      :tags="store.tags"
      :currentUserId="store.currentUserId"
      :initial-start="createStart"
      :initial-end="createEnd"
      @saved="reload"
    />
    <EventDetail
      v-model="detailVisible"
      :event="selectedEvent"
      :currentUserId="store.currentUserId"
      @edit="editEvent"
      @remove="removeEvent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Calendar, Download, Plus, Search, Setting } from '@element-plus/icons-vue'
import EventDetail from '../../components/EventDetail.vue'
import EventForm from '../../components/EventForm.vue'
import { api, downloadFile } from '../../api/http'
import type { CalendarItem, EventItem } from '../../api/types'
import { useAppStore } from '../../stores/app'

type CalendarDay = {
  key: string
  date: Date
  currentMonth: boolean
}

const store = useAppStore()
const today = new Date()
const selectedDate = ref(new Date(2026, 5, 5))
const view = ref('month')
const viewOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' }
]
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const weekdayLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const weekdayShortLabels = ['日', '一', '二', '三', '四', '五', '六']
const hours = Array.from({ length: 11 }, (_, index) => index + 8)
const visibleCalendarIds = ref<string[]>([])
const formVisible = ref(false)
const detailVisible = ref(false)
const editingEvent = ref<EventItem | null>(null)
const selectedEvent = ref<EventItem | null>(null)
const createStart = ref<Date | null>(null)
const createEnd = ref<Date | null>(null)
const mobileSettingsVisible = ref(false)
const mobileCalendarSheetVisible = ref(false)
const mobileViewMode = ref<'day' | 'three' | 'week'>('day')

const localCalendars: CalendarItem[] = [
  { id: 'local-1', name: '李宇航的日历', type: 'PERSONAL', color: '#3b82f6', visible: true, status: 'ACTIVE' },
  { id: 'local-2', name: '产品发布日历', type: 'SHARED', color: '#f59e0b', visible: true, status: 'ACTIVE' },
  { id: 'local-3', name: '部门协作日历', type: 'SHARED', color: '#ef4444', visible: true, status: 'ACTIVE' },
  { id: 'local-4', name: '项目日程', type: 'SHARED', color: '#10b981', visible: true, status: 'ACTIVE' }
]

const localEvents: EventItem[] = [
  {
    id: 'local-1',
    calendar_id: 'local-2',
    organizer_user_id: 'user-001',
    title: '版本发布评审',
    start_at: new Date(2026, 5, 5, 11, 30).toISOString(),
    end_at: new Date(2026, 5, 5, 12, 30).toISOString(),
    all_day: false,
    calendar_name: '产品发布日历',
    calendar_color: '#3b82f6',
    status: 'ACTIVE'
  },
  {
    id: 'local-2',
    calendar_id: 'local-4',
    organizer_user_id: 'user-001',
    title: '端午节',
    start_at: new Date(2026, 5, 19, 9, 0).toISOString(),
    end_at: new Date(2026, 5, 19, 18, 0).toISOString(),
    all_day: true,
    calendar_name: '项目日程',
    calendar_color: '#10b981',
    status: 'ACTIVE'
  }
]

const displayCalendars = computed(() => (store.calendars.length ? store.calendars : localCalendars))
const formCalendars = computed(() => displayCalendars.value)
const personalCalendars = computed(() => displayCalendars.value.filter((item) => item.type === 'PERSONAL'))
const sharedCalendars = computed(() => displayCalendars.value.filter((item) => item.type !== 'PERSONAL'))
const displayEvents = computed(() => (store.events.length ? store.events : localEvents))
const filteredEvents = computed(() => displayEvents.value.filter((event) => visibleCalendarIds.value.map(String).includes(String(event.calendar_id))))
const monthTitle = computed(() => `${selectedDate.value.getFullYear()}年${selectedDate.value.getMonth() + 1}月`)
const stageTitle = computed(() => {
  if (view.value === 'day') return `${selectedDate.value.getFullYear()}年${selectedDate.value.getMonth() + 1}月${selectedDate.value.getDate()}日`
  return `${selectedDate.value.getFullYear()}年${selectedDate.value.getMonth() + 1}月`
})
const monthDays = computed<CalendarDay[]>(() => {
  const date = selectedDate.value
  const first = new Date(date.getFullYear(), date.getMonth(), 1)
  const start = new Date(first)
  start.setDate(first.getDate() - first.getDay())
  return Array.from({ length: 42 }, (_, index) => {
    const item = new Date(start)
    item.setDate(start.getDate() + index)
    return {
      key: item.toISOString(),
      date: item,
      currentMonth: item.getMonth() === date.getMonth()
    }
  })
})
const mobileMonthDays = computed(() => monthDays.value.slice(0, 28))
const agendaDays = computed(() => {
  if (view.value === 'day') return [selectedDate.value]
  const start = new Date(selectedDate.value)
  start.setDate(start.getDate() - start.getDay())
  return Array.from({ length: 7 }, (_, index) => {
    const item = new Date(start)
    item.setDate(start.getDate() + index)
    return item
  })
})

onMounted(async () => {
  await store.loadBase()
  visibleCalendarIds.value = displayCalendars.value.map((item) => item.id)
  await reload()
})

watch(selectedDate, reload)
watch(displayCalendars, (items) => {
  visibleCalendarIds.value = items.map((item) => item.id)
})

async function reload() {
  await store.loadEvents('')
}

function openCreate(date = selectedDate.value, hour?: number) {
  const start = new Date(date)
  if (hour !== undefined) {
    start.setHours(hour, 0, 0, 0)
  } else {
    start.setHours(11, 30, 0, 0)
  }
  const end = new Date(start)
  end.setHours(start.getHours() + 1, start.getMinutes(), 0, 0)
  selectedDate.value = new Date(date)
  createStart.value = start
  createEnd.value = end
  editingEvent.value = null
  formVisible.value = true
}

function selectEvent(event: EventItem) {
  createStart.value = null
  createEnd.value = null
  selectedEvent.value = event
  detailVisible.value = true
}

function editEvent(event: EventItem) {
  createStart.value = null
  createEnd.value = null
  selectedEvent.value = null
  detailVisible.value = false
  editingEvent.value = event
  formVisible.value = true
}

async function removeEvent(event: EventItem) {
  try {
    await ElMessageBox.confirm('确认删除该日程？')
  } catch {
    return
  }
  try {
    if (event.id && !event.id.toString().startsWith('local-')) {
      await api.delete(`/events/${event.id}?operatorUserId=${store.currentUserId}&scope=single`)
    }
    ElMessage.success('已删除')
    detailVisible.value = false
    await reload()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

function goToday() {
  selectedDate.value = new Date()
}

function move(step: number) {
  const next = new Date(selectedDate.value)
  next.setDate(next.getDate() + step * (view.value === 'month' ? 30 : view.value === 'day' ? 1 : 7))
  selectedDate.value = next
}

function moveMonth(step: number) {
  const next = new Date(selectedDate.value)
  next.setMonth(next.getMonth() + step)
  selectedDate.value = next
}

function timeText(event: EventItem) {
  if (event.all_day) return '全天'
  return `${new Date(event.start_at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${new Date(event.end_at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

function eventsByDay(day: Date) {
  return filteredEvents.value.filter((event) => {
    return isSameDate(new Date(event.start_at), day)
  }).slice(0, 4)
}

function eventStyle(event: EventItem) {
  const start = new Date(event.start_at)
  const end = new Date(event.end_at)
  const top = Math.max(0, (start.getHours() - 8) * 72 + start.getMinutes() * 1.2)
  const height = Math.max(42, (end.getTime() - start.getTime()) / 60000 * 1.2)
  const color = event.calendar_color || event.tag_color || 'var(--calendar-primary)'
  return {
    top: `${top}px`,
    height: `${height}px`,
    borderColor: color,
    background: `${color}18`
  }
}

function lunarText(day: Date) {
  const marks: Record<string, string> = {
    '2026-06-01': '儿童节',
    '2026-06-19': '端午节',
    '2026-07-01': '建党节'
  }
  const key = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`
  return marks[key] || ['初七', '初八', '初九', '初十', '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十'][day.getDate() % 14]
}

function isSameDate(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

async function exportEvents() {
  try {
    await downloadFile('/export/events', { operatorUserId: store.currentUserId })
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error('导出失败：' + (err as Error).message)
  }
}
</script>

<style scoped>
.calendar-app {
  min-height: 100vh;
  overflow: hidden;
  color: var(--calendar-text);
  background: var(--calendar-bg);
}

/* ===== Header ===== */
.calendar-window-bar {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 28px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
  box-shadow: var(--calendar-shadow-sm);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

/* ===== Workspace Layout ===== */
.calendar-workspace {
  display: grid;
  grid-template-columns: var(--calendar-sidebar-width) minmax(0, 1fr);
  height: calc(100vh - 60px);
}

/* ===== Sidebar ===== */
.calendar-sidebar {
  min-width: 0;
  padding: 16px 14px 28px;
  overflow: auto;
  border-right: 1px solid var(--calendar-border);
  background: var(--calendar-sidebar);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.search-box {
  height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-radius: var(--calendar-control-radius);
  color: var(--calendar-soft-text);
  background: var(--calendar-bg);
  font-size: 14px;
  font-weight: 600;
  border: 1px solid var(--calendar-border);
  transition: all 0.18s ease;
}

.search-box:hover {
  border-color: var(--calendar-primary);
  color: var(--calendar-primary);
  background: var(--calendar-primary-bg);
  box-shadow: var(--calendar-shadow-sm);
}

.mini-calendar {
  margin-top: 8px;
}

.mini-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 700;
}

.mini-nav {
  display: flex;
  gap: 2px;
}

.mini-nav :deep(.el-button) {
  padding: 4px;
  color: var(--calendar-soft-text);
}

.mini-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 6px;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.mini-day {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  color: var(--calendar-text);
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mini-day.muted {
  color: var(--calendar-muted);
}

.mini-day.today:not(.active) {
  color: var(--calendar-primary);
  font-weight: 700;
}

.mini-day.active {
  color: #fff;
  background: var(--calendar-primary);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.35);
}

.mini-day:hover:not(.active) {
  background: var(--calendar-bg);
  color: var(--calendar-text);
}

/* ===== Calendar Groups ===== */
.calendar-groups {
  margin-top: 12px;
}

.group-title {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.group-title-split {
  margin-top: 10px;
  border-top: 1px solid var(--calendar-border-soft);
  padding-top: 10px;
}

.group-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-bottom: 8px;
}

.group-block p {
  margin: 0 0 10px;
  color: var(--calendar-soft-text);
  font-weight: 600;
}

.calendar-check {
  min-height: 32px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 6px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.calendar-check:hover {
  background: var(--calendar-bg);
}

.calendar-check input {
  display: none;
}

.calendar-check i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.calendar-check input:checked + i::after {
  content: "";
  display: block;
  width: 6px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) translate(-1px, -1px);
}

/* ===== Stage / Main Content ===== */
.calendar-stage {
  min-width: 0;
  background: var(--calendar-bg);
  display: flex;
  flex-direction: column;
}

/* ===== Toolbar ===== */
.calendar-toolbar {
  height: var(--calendar-toolbar-height);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
}

.period-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
}

.view-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.today-btn {
  font-weight: 600;
  color: var(--calendar-primary);
}

.view-switch {
  display: inline-flex;
  height: 34px;
  padding: 3px;
  border-radius: 8px;
  background: var(--calendar-bg);
  border: 1px solid var(--calendar-border);
}

.view-switch button {
  min-width: 42px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  color: var(--calendar-soft-text);
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.view-switch button:hover {
  color: var(--calendar-text);
  background: var(--calendar-surface);
}

.view-switch button.active {
  background: var(--calendar-primary);
  color: #fff;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.3);
}

/* ===== Month Board ===== */
.month-board,
.agenda-board {
  flex: 1;
  overflow: auto;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(132px, 1fr));
  height: 44px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
  position: sticky;
  top: 0;
  z-index: 2;
}

.weekday-row span {
  display: flex;
  align-items: center;
  padding-left: 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.month-grid-view {
  display: grid;
  grid-template-columns: repeat(7, minmax(132px, 1fr));
  background: var(--calendar-border-soft);
  gap: 1px;
}

.month-cell-view {
  min-height: var(--calendar-cell-height);
  padding: 8px 10px 8px;
  border: 0;
  background: var(--calendar-surface);
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: background 0.12s ease;
}

.month-cell-view:hover {
  background: var(--calendar-primary-bg);
}

.month-cell-view.muted {
  background: var(--calendar-bg);
}

.date-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 28px;
}

.date-wrap strong {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.date-wrap small {
  color: var(--calendar-muted);
  font-size: 11px;
}

.date-wrap.today strong {
  background: var(--calendar-primary);
  color: #fff;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.35);
}

.cell-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.event-pill {
  display: block;
  padding: 3px 8px;
  border-radius: 6px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s ease, transform 0.15s ease;
  line-height: 1.5;
  text-shadow: 0 1px 1px rgba(0,0,0,0.15);
}

.event-pill:hover {
  opacity: 0.85;
  transform: translateX(2px);
}

/* ===== Agenda / Week / Day View ===== */
.day-header-row {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(132px, 1fr));
  height: 96px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
}

.day-header-row span {
  display: flex;
  align-items: flex-end;
  padding: 0 0 8px 12px;
  font-size: 11px;
  color: var(--calendar-muted);
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.agenda-board.single .day-header-row {
  grid-template-columns: 72px minmax(300px, 1fr);
}

.day-header-row button {
  border: 0;
  border-right: 1px solid var(--calendar-border-soft);
  background: var(--calendar-surface);
  color: var(--calendar-soft-text);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 8px;
  transition: background 0.12s ease;
}

.day-header-row button:hover {
  background: var(--calendar-primary-bg);
}

.day-header-row button strong {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--calendar-text);
  font-size: 16px;
  font-weight: 700;
}

.day-header-row button small {
  font-size: 11px;
  color: var(--calendar-muted);
}

.day-header-row button.active strong {
  background: var(--calendar-primary);
  color: #fff;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
}

.day-header-row button.active small {
  color: var(--calendar-primary);
  font-weight: 600;
}

.time-grid-view {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(132px, 1fr));
  min-height: 792px;
  background: var(--calendar-border-soft);
  gap: 1px;
}

.agenda-board.single .time-grid-view {
  grid-template-columns: 72px minmax(300px, 1fr);
}

.time-axis {
  color: var(--calendar-muted);
  font-weight: 600;
  background: var(--calendar-surface);
}

.time-axis span {
  height: 72px;
  display: flex;
  align-items: flex-start;
  padding: 8px 10px;
  font-size: 11px;
  border-bottom: 1px solid var(--calendar-border-soft);
  letter-spacing: 0.02em;
}

.day-lane-view {
  position: relative;
  min-height: 792px;
  background: var(--calendar-surface);
  border-right: 1px solid var(--calendar-border-soft);
}

.time-slot {
  height: 72px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--calendar-border-soft);
  background: transparent;
  cursor: pointer;
  transition: background 0.1s ease;
}

.time-slot:hover {
  background: var(--calendar-primary-bg);
}

.timed-event {
  position: absolute;
  left: 6px;
  right: 6px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 2px;
  padding: 6px 10px;
  border: 1px solid;
  border-left-width: 3px;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.timed-event:hover {
  opacity: 0.88;
  transform: translateX(2px);
}

.timed-event strong {
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timed-event small {
  font-size: 11px;
  font-weight: 600;
  opacity: 0.7;
}

/* ===== Mobile ===== */
.mobile-calendar {
  display: none;
}

@media (max-width: 920px) {
  .desktop-calendar {
    display: none;
  }

  .calendar-app {
    overflow: auto;
    background: var(--calendar-bg);
  }

  .mobile-calendar {
    display: block;
    min-height: 100vh;
    background: var(--calendar-bg);
  }

  .mobile-topbar,
  .mobile-settings-header {
    height: 64px;
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
    padding: 0 16px;
    background: var(--calendar-surface);
    border-bottom: 1px solid var(--calendar-border);
  }

  .mobile-brand {
    display: flex;
    align-items: center;
  }

  .mobile-topbar strong {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  .mobile-settings-header {
    grid-template-columns: 40px minmax(0, 1fr);
    box-shadow: var(--calendar-shadow-sm);
  }

  .mobile-actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .mobile-icon-btn {
    width: 36px;
    height: 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 0;
    border-radius: 8px;
    color: var(--calendar-text);
    background: transparent;
    font-size: 20px;
    cursor: pointer;
    transition: background 0.15s ease;
  }

  .mobile-icon-btn:hover {
    background: var(--calendar-bg);
  }

  .today-dot {
    font-size: 12px;
    font-weight: 700;
    color: var(--calendar-primary);
  }

  .mobile-week-nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    background: var(--calendar-surface);
    border-bottom: 1px solid var(--calendar-border-soft);
    font-size: 15px;
    font-weight: 700;
  }

  .mobile-week-nav :deep(.el-button) {
    color: var(--calendar-primary);
  }

  .mobile-month {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 0;
    padding: 16px 12px 14px;
    background: var(--calendar-surface);
    border-bottom: 8px solid var(--calendar-bg);
  }

  .mobile-weekdays {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    margin-bottom: 8px;
    color: var(--calendar-muted);
    text-align: center;
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .mobile-day {
    position: relative;
    min-height: 68px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    border: 0;
    color: var(--calendar-text);
    background: transparent;
    text-align: center;
    cursor: pointer;
    border-radius: 10px;
    transition: background 0.12s ease;
  }

  .mobile-day:hover {
    background: var(--calendar-primary-bg);
  }

  .mobile-day strong {
    width: 38px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    font-size: 16px;
    font-weight: 600;
    transition: all 0.15s ease;
  }

  .mobile-day span {
    display: block;
    color: var(--calendar-muted);
    font-size: 11px;
  }

  .mobile-day.muted strong,
  .mobile-day.muted span {
    color: var(--calendar-muted);
  }

  .mobile-day.active strong {
    color: #fff;
    background: var(--calendar-primary);
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.35);
  }

  .mobile-day.active span {
    color: var(--calendar-primary);
    font-weight: 600;
  }

  .mobile-day i {
    position: absolute;
    left: 50%;
    bottom: 6px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--calendar-primary);
    transform: translateX(-50%);
  }

  /* Mobile Agenda */
  .mobile-agenda {
    background: var(--calendar-surface);
  }

  .mobile-agenda-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 18px;
    border-bottom: 1px solid var(--calendar-border-soft);
  }

  .mobile-date-info {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  .mobile-date-info strong {
    font-size: 18px;
    font-weight: 700;
  }

  .mobile-date-info span {
    font-size: 13px;
    color: var(--calendar-muted);
    font-weight: 600;
  }

  .mobile-agenda-title button {
    display: flex;
    align-items: center;
    gap: 4px;
    border: 0;
    color: var(--calendar-primary);
    background: transparent;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
  }

  .mobile-new-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    width: 100%;
    min-height: 52px;
    border: 0;
    border-bottom: 1px solid var(--calendar-border-soft);
    color: var(--calendar-muted);
    background: var(--calendar-surface);
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.12s ease;
  }

  .mobile-new-row:hover {
    background: var(--calendar-primary-bg);
    color: var(--calendar-primary);
  }

  .mobile-empty-day {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 80px;
    color: var(--calendar-muted);
    font-size: 14px;
    font-weight: 600;
    border-bottom: 1px solid var(--calendar-border-soft);
  }

  .mobile-event-row {
    width: 100%;
    display: grid;
    grid-template-columns: 4px 1fr auto;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    border: 0;
    border-bottom: 1px solid var(--calendar-border-soft);
    background: var(--calendar-surface);
    text-align: left;
    cursor: pointer;
    transition: background 0.12s ease;
  }

  .mobile-event-row:hover {
    background: var(--calendar-bg);
  }

  .mobile-event-row i {
    width: 4px;
    height: 40px;
    border-radius: 2px;
  }

  .mobile-event-title {
    font-size: 15px;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-event-row small {
    color: var(--calendar-muted);
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }

  /* Mobile Settings */
  .mobile-settings {
    min-height: 100vh;
    padding-bottom: 28px;
    background: var(--calendar-bg);
  }

  .mobile-view-card,
  .mobile-add-calendar,
  .mobile-calendar-card {
    margin: 14px 16px;
    border-radius: var(--calendar-radius);
    background: var(--calendar-surface);
    box-shadow: var(--calendar-shadow-sm);
  }

  .mobile-view-card {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    padding: 16px 10px;
  }

  .mobile-view-card button {
    display: grid;
    justify-items: center;
    gap: 8px;
    border: 0;
    background: transparent;
    font-size: 18px;
    cursor: pointer;
  }

  .mobile-view-card button strong {
    min-width: 58px;
    padding: 5px 14px;
    border-radius: 18px;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.15s ease;
  }

  .mobile-view-card button.active strong {
    color: #fff;
    background: var(--calendar-primary);
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
  }

  .mode-art {
    width: 54px;
    height: 54px;
    border-radius: 12px;
    border: 1px solid var(--calendar-border);
    background:
      linear-gradient(var(--calendar-primary), var(--calendar-primary)) 12px 14px / 30px 7px no-repeat,
      repeating-linear-gradient(90deg, transparent 0 8px, rgba(59,130,246,0.3) 8px 11px) 12px 26px / 32px 18px no-repeat,
      var(--calendar-primary-bg);
  }

  .mode-art.three {
    background:
      linear-gradient(var(--calendar-primary), var(--calendar-primary)) 8px 10px / 9px 34px no-repeat,
      linear-gradient(var(--calendar-primary-light), var(--calendar-primary-light)) 22px 10px / 9px 34px no-repeat,
      linear-gradient(var(--calendar-primary), var(--calendar-primary)) 36px 10px / 9px 34px no-repeat,
      var(--calendar-primary-bg);
  }

  .mode-art.week {
    background:
      linear-gradient(var(--calendar-primary), var(--calendar-primary)) 12px 13px / 30px 8px no-repeat,
      linear-gradient(var(--calendar-primary-light), var(--calendar-primary-light)) 12px 27px / 30px 7px no-repeat,
      linear-gradient(var(--calendar-primary-light), var(--calendar-primary-light)) 12px 39px / 30px 7px no-repeat,
      var(--calendar-primary-bg);
  }

  .mobile-add-calendar {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 0 20px;
    min-height: 56px;
    border: 0;
    color: var(--calendar-primary);
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.12s ease;
  }

  .mobile-add-calendar:hover {
    background: var(--calendar-primary-bg);
  }

  .mobile-add-calendar span {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--calendar-primary);
    border-radius: 50%;
    font-size: 22px;
    line-height: 1;
  }

  .mobile-calendar-card {
    padding: 18px;
  }

  .mobile-calendar-card p {
    margin: 0 0 14px;
    color: var(--calendar-muted);
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .mobile-calendar-line {
    display: grid;
    grid-template-columns: 28px 1fr 20px;
    align-items: center;
    gap: 12px;
    min-height: 56px;
    border-bottom: 1px solid var(--calendar-border-soft);
  }

  .mobile-calendar-line:last-child {
    border-bottom: 0;
  }

  .mobile-calendar-line > :last-child {
    color: var(--calendar-muted);
  }

  .mobile-calendar-line i {
    width: 20px;
    height: 20px;
    border-radius: 5px;
  }

  .mobile-calendar-line strong {
    font-size: 15px;
    font-weight: 700;
  }

  .mobile-calendar-line small {
    display: block;
    margin-top: 2px;
    color: var(--calendar-muted);
    font-size: 12px;
    font-weight: 600;
  }

  .mobile-sheet-mask {
    position: fixed;
    inset: 0;
    z-index: 20;
    display: flex;
    align-items: flex-end;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(2px);
  }

  .mobile-add-sheet {
    width: 100%;
    padding: 26px 18px 30px;
    border-radius: 16px 16px 0 0;
    background: var(--calendar-surface);
  }

  .mobile-add-sheet h2 {
    margin: 0 0 24px;
    text-align: center;
    font-size: 18px;
    font-weight: 700;
  }

  .mobile-add-sheet button {
    width: 100%;
    min-height: 80px;
    display: grid;
    grid-template-columns: 48px 1fr;
    align-items: center;
    gap: 14px;
    margin-top: 12px;
    padding: 16px 18px;
    border: 0;
    border-radius: var(--calendar-control-radius);
    background: var(--calendar-bg);
    text-align: left;
    cursor: pointer;
    transition: background 0.12s ease;
  }

  .mobile-add-sheet button:hover {
    background: var(--calendar-primary-bg);
  }

  .mobile-add-sheet strong {
    display: block;
    font-size: 16px;
    font-weight: 700;
  }

  .mobile-add-sheet small {
    display: block;
    margin-top: 4px;
    color: var(--calendar-muted);
    font-size: 13px;
    font-weight: 500;
    line-height: 1.4;
  }

  .sheet-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--calendar-primary);
  }

  .sheet-icon.public {
    border-radius: 10px;
    background: var(--calendar-success);
  }
}
</style>
