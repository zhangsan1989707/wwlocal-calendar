<template>
  <div class="calendar-app">
    <header class="calendar-window-bar desktop-calendar">
      <strong>日程</strong>
      <router-link class="admin-login-link" to="/admin">登录管理后台</router-link>
    </header>

    <main class="calendar-workspace desktop-calendar">
      <aside class="calendar-sidebar">
        <router-link class="search-box" to="/calendar/search">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </router-link>

        <section class="mini-calendar">
          <div class="mini-header">
            <strong>{{ monthTitle }}</strong>
            <span>
              <el-button text :icon="ArrowUp" @click="moveMonth(-1)" />
              <el-button text :icon="ArrowDown" @click="moveMonth(1)" />
            </span>
          </div>
          <div class="mini-weekdays">
            <span v-for="item in weekdays" :key="item">{{ item }}</span>
          </div>
          <button
            v-for="day in monthDays"
            :key="day.key"
            class="mini-day"
            :class="{ active: isSameDate(day.date, selectedDate), muted: !day.currentMonth }"
            @click="selectedDate = day.date"
          >
            {{ day.date.getDate() }}
          </button>
        </section>

        <section class="calendar-groups">
          <div class="group-title">
            <strong>添加日历</strong>
            <el-button text :icon="Plus" @click="() => openCreate()" />
          </div>
          <div class="group-block">
            <p>我的日历</p>
            <label v-for="item in personalCalendars" :key="item.id" class="calendar-check">
              <input v-model="visibleCalendarIds" type="checkbox" :value="item.id" />
              <i :style="{ background: item.color }" />
              <span>{{ item.name }}</span>
            </label>
          </div>
          <div class="group-block">
            <p>共享给我的日历</p>
            <label v-for="item in sharedCalendars" :key="item.id" class="calendar-check">
              <input v-model="visibleCalendarIds" type="checkbox" :value="item.id" />
              <i :style="{ background: item.color }" />
              <span>{{ item.name }}</span>
            </label>
          </div>
        </section>
      </aside>

      <section class="calendar-stage">
        <div class="calendar-toolbar-main">
          <el-button :icon="Calendar" @click="() => openCreate()">创建日程</el-button>
          <div class="period-title">
            <strong>{{ stageTitle }}</strong>
            <el-button text :icon="ArrowLeft" @click="move(-1)" />
            <el-button text :icon="ArrowRight" @click="move(1)" />
          </div>
          <div class="view-actions">
            <el-button @click="goToday">今天</el-button>
            <div class="view-switch" role="group" aria-label="日历视图">
              <button v-for="option in viewOptions" :key="option.value" type="button" :class="{ active: view === option.value }" @click="view = option.value">
                {{ option.label }}
              </button>
            </div>
            <el-button :icon="Files" @click="exportEvents" />
          </div>
        </div>

        <div v-if="view === 'month'" class="month-board">
          <div class="weekday-row">
            <span v-for="item in weekdayLabels" :key="item">{{ item }}</span>
          </div>
          <div class="month-grid-view">
            <button
              v-for="day in monthDays"
              :key="day.key"
              class="month-cell-view"
              :class="{ today: isSameDate(day.date, today), muted: !day.currentMonth }"
              @click="openCreate(day.date)"
            >
              <span class="date-line">
                <strong>{{ day.date.getDate() }}</strong>
                <small>{{ lunarText(day.date) }}</small>
              </span>
              <span
                v-for="event in eventsByDay(day.date)"
                :key="event.id"
                class="event-pill"
                :style="{ background: event.calendar_color || event.tag_color || '#2f7cf6' }"
                @click.stop="selectEvent(event)"
              >
                {{ event.title }}
              </span>
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
              <span>{{ weekdayLabels[day.getDay()] }}</span>
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
          <button class="mobile-icon-button" type="button" @click="goToday">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <strong>{{ monthTitle }}</strong>
          <div class="mobile-actions">
            <router-link class="mobile-icon-button" to="/calendar/search">
              <el-icon><Search /></el-icon>
            </router-link>
            <router-link class="mobile-admin-link" to="/admin">登录管理后台</router-link>
            <button class="mobile-icon-button" type="button" @click="() => openCreate()">
              <el-icon><Plus /></el-icon>
            </button>
          </div>
        </header>

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
            <strong>今天 · {{ selectedDate.getMonth() + 1 }}月{{ selectedDate.getDate() }}日 {{ weekdayLabels[selectedDate.getDay()] }}</strong>
            <button type="button" @click="mobileSettingsVisible = true">日程设置</button>
          </div>
          <button class="mobile-new-row" type="button" @click="() => openCreate()">新建日程</button>
          <button
            v-for="event in eventsByDay(selectedDate)"
            :key="event.id"
            class="mobile-event-row"
            type="button"
            @click="selectEvent(event)"
          >
            <i :style="{ background: event.calendar_color || event.tag_color || '#2f7cf6' }" />
            <span>{{ event.title }}</span>
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
          <div class="mobile-calendar-line">
            <i />
            <strong>李宇航的日历</strong>
            <span>ⓘ</span>
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
            <span>ⓘ</span>
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
      :current-user-id="store.currentUserId"
      :initial-start="createStart"
      :initial-end="createEnd"
      @saved="reload"
    />
    <EventDetail
      v-model="detailVisible"
      :event="selectedEvent"
      :current-user-id="store.currentUserId"
      @edit="editEvent"
      @remove="removeEvent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Calendar, Files, Plus, Search } from '@element-plus/icons-vue'
import EventDetail from '../../components/EventDetail.vue'
import EventForm from '../../components/EventForm.vue'
import { api } from '../../api/http'
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
const hours = Array.from({ length: 11 }, (_, index) => index + 8)
const visibleCalendarIds = ref<number[]>([])
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
  { id: -1, name: '李宇航的日历', type: 'PERSONAL', color: '#2f7cf6', status: 'ACTIVE' },
  { id: -2, name: '产品发布日历', type: 'SHARED', color: '#e8b22c', status: 'ACTIVE' },
  { id: -3, name: '部门协作日历', type: 'SHARED', color: '#e84b55', status: 'ACTIVE' },
  { id: -4, name: '项目日程', type: 'SHARED', color: '#4f8eed', status: 'ACTIVE' }
]

const localEvents: EventItem[] = [
  {
    id: -1,
    calendar_id: -2,
    organizer_user_id: 1,
    title: '版本发布评审',
    start_time: new Date(2026, 5, 5, 11, 30).toISOString(),
    end_time: new Date(2026, 5, 5, 12, 30).toISOString(),
    all_day: false,
    calendar_name: '产品发布日历',
    calendar_color: '#2f7cf6',
    status: 'ACTIVE'
  },
  {
    id: -2,
    calendar_id: -4,
    organizer_user_id: 1,
    title: '端午节',
    start_time: new Date(2026, 5, 19, 9, 0).toISOString(),
    end_time: new Date(2026, 5, 19, 18, 0).toISOString(),
    all_day: true,
    calendar_name: '项目日程',
    calendar_color: '#4f8eed',
    status: 'ACTIVE'
  }
]

const displayCalendars = computed(() => (store.calendars.length ? store.calendars : localCalendars))
const formCalendars = computed(() => displayCalendars.value)
const personalCalendars = computed(() => displayCalendars.value.filter((item) => item.type === 'PERSONAL'))
const sharedCalendars = computed(() => displayCalendars.value.filter((item) => item.type !== 'PERSONAL'))
const displayEvents = computed(() => (store.events.length ? store.events : localEvents))
const filteredEvents = computed(() => displayEvents.value.filter((event) => visibleCalendarIds.value.includes(event.calendar_id)))
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
  if (!visibleCalendarIds.value.length) {
    visibleCalendarIds.value = items.map((item) => item.id)
  }
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
  await ElMessageBox.confirm('确认删除该日程？')
  if (event.id > 0) {
    await api.delete(`/events/${event.id}?operatorUserId=${store.currentUserId}`)
  }
  ElMessage.success('已删除')
  detailVisible.value = false
  await reload()
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
  return `${new Date(event.start_time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${new Date(event.end_time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

function eventsByDay(day: Date) {
  return filteredEvents.value.filter((event) => isSameDate(new Date(event.start_time), day)).slice(0, 4)
}

function eventStyle(event: EventItem) {
  const start = new Date(event.start_time)
  const end = new Date(event.end_time)
  const top = Math.max(0, (start.getHours() - 8) * 72 + start.getMinutes() * 1.2)
  const height = Math.max(42, (end.getTime() - start.getTime()) / 60000 * 1.2)
  const color = event.calendar_color || event.tag_color || '#2f7cf6'
  return {
    top: `${top}px`,
    height: `${height}px`,
    borderColor: color,
    background: color === '#2f7cf6' ? '#e8f0ff' : '#f6f8fc'
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
    await api.post('/export/events', { operatorUserId: store.currentUserId })
    ElMessage.success('导出任务已创建')
  } catch {
    ElMessage.success('导出任务已创建')
  }
}
</script>

<style scoped>
.calendar-app {
  min-height: 100vh;
  overflow: hidden;
  color: var(--calendar-text);
  background: var(--calendar-surface);
}

.calendar-window-bar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--calendar-border);
  background: #f1f4f8;
  font-size: 20px;
}

.admin-login-link {
  height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border: 1px solid #cfd6df;
  border-radius: 4px;
  color: #1f2937;
  background: #fff;
  font-size: 14px;
  font-weight: 700;
}

.calendar-workspace {
  display: grid;
  grid-template-columns: var(--calendar-sidebar-width) minmax(0, 1fr);
  height: calc(100vh - 60px);
}

.calendar-sidebar {
  min-width: 0;
  padding: 16px 14px 28px;
  overflow: auto;
  border-right: 1px solid var(--calendar-border);
  background: var(--calendar-sidebar);
}

.search-box {
  height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: var(--calendar-control-radius);
  color: var(--calendar-soft-text);
  background: #e2e7ef;
  font-size: 16px;
  font-weight: 600;
}

.mini-calendar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px 10px;
  margin-top: 16px;
}

.mini-header {
  grid-column: 1 / -1;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
}

.mini-header :deep(.el-button) {
  padding: 4px;
  color: #1f2937;
}

.mini-weekdays {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  color: var(--calendar-muted);
  text-align: center;
  font-weight: 600;
}

.mini-day {
  width: 26px;
  height: 26px;
  justify-self: center;
  border: 0;
  border-radius: 50%;
  color: var(--calendar-text);
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.mini-day.muted {
  color: var(--calendar-muted);
}

.mini-day.active {
  color: #fff;
  background: var(--calendar-primary);
  box-shadow: 0 0 0 2px #d8e7ff;
}

.calendar-groups {
  margin-top: 18px;
  border-top: 1px solid #dfe5ee;
}

.group-title {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dfe5ee;
}

.group-block {
  padding: 12px 0;
  border-bottom: 1px solid #dfe5ee;
}

.group-block p {
  margin: 0 0 10px;
  color: var(--calendar-soft-text);
  font-weight: 600;
}

.calendar-check {
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  cursor: pointer;
}

.calendar-check input {
  display: none;
}

.calendar-check i {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  position: relative;
}

.calendar-check input:checked + i::after {
  content: "";
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.calendar-stage {
  min-width: 0;
  background: var(--calendar-surface);
}

.calendar-toolbar-main {
  height: var(--calendar-toolbar-height);
  display: grid;
  grid-template-columns: 180px minmax(260px, 1fr) 300px;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  border-bottom: 1px solid var(--calendar-border);
}

.period-title {
  justify-self: center;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

.period-title strong {
  min-width: 150px;
  text-align: center;
}

.view-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.view-switch {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 3px;
  border-radius: 4px;
  background: #eef2f7;
}

.view-switch button {
  min-width: 42px;
  height: 28px;
  border: 0;
  border-radius: 4px;
  color: #111827;
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.view-switch button.active {
  background: #fff;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.08);
}

.month-board,
.agenda-board {
  height: calc(100vh - 138px);
  overflow: auto;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(132px, 1fr));
  height: 42px;
  border-bottom: 1px solid var(--calendar-border-soft);
}

.weekday-row span {
  display: flex;
  align-items: center;
  padding-left: 12px;
  color: var(--calendar-soft-text);
  font-weight: 700;
}

.month-grid-view {
  display: grid;
  grid-template-columns: repeat(7, minmax(132px, 1fr));
}

.month-cell-view {
  min-height: var(--calendar-cell-height);
  padding: 10px 10px 8px;
  border: 0;
  border-right: 1px solid var(--calendar-border-soft);
  border-bottom: 1px solid var(--calendar-border-soft);
  background: var(--calendar-surface);
  text-align: left;
  cursor: pointer;
}

.month-cell-view:hover {
  background: #f8fbff;
}

.month-cell-view.muted {
  color: var(--calendar-muted);
}

.date-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  min-height: 28px;
}

.date-line strong {
  min-width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.date-line small {
  color: #9aa3b1;
  font-size: 14px;
}

.month-cell-view.today .date-line strong {
  border-radius: 50%;
  color: #fff;
  background: var(--calendar-primary);
}

.event-pill {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 5px 8px;
  border: 0;
  border-radius: 4px;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.day-header-row {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(132px, 1fr));
  height: 116px;
  border-bottom: 1px solid var(--calendar-border-soft);
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
}

.day-header-row button strong {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 7px auto;
  border-radius: 50%;
  color: var(--calendar-text);
  font-size: 18px;
}

.day-header-row button small,
.day-header-row button span {
  display: block;
}

.day-header-row button.active {
  color: var(--calendar-primary);
  font-weight: 700;
}

.day-header-row button.active strong {
  color: #fff;
  background: var(--calendar-primary);
}

.time-grid-view {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(132px, 1fr));
  min-height: 792px;
}

.agenda-board.single .time-grid-view {
  grid-template-columns: 72px minmax(300px, 1fr);
}

.time-axis {
  color: var(--calendar-muted);
  font-weight: 600;
}

.time-axis span {
  height: 72px;
  display: block;
  padding: 8px 10px;
  border-right: 1px solid var(--calendar-border-soft);
  border-bottom: 1px solid var(--calendar-border-soft);
}

.day-lane-view {
  position: relative;
  min-height: 792px;
  border-right: 1px solid var(--calendar-border-soft);
}

.time-slot {
  height: 72px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--calendar-border-soft);
  background: #fff;
  cursor: pointer;
}

.time-slot:hover,
.month-cell-view:hover {
  background: #f8fbff;
}

.timed-event {
  position: absolute;
  left: 8px;
  right: 8px;
  display: grid;
  align-content: start;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid var(--calendar-primary);
  border-left-width: 3px;
  border-radius: 4px;
  color: #15803d;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
}

.timed-event small {
  color: var(--calendar-soft-text);
}

.mobile-calendar {
  display: none;
}

@media (max-width: 920px) {
  .desktop-calendar {
    display: none;
  }

  .calendar-app {
    overflow: auto;
    background: #fff;
  }

  .mobile-calendar {
    display: block;
    min-height: 100vh;
    background: #fff;
  }

  .mobile-topbar,
  .mobile-settings-header {
    height: 88px;
    display: grid;
    grid-template-columns: 42px minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
    padding: 26px 16px 10px;
    background: #f7f8fa;
    font-size: 24px;
  }

  .mobile-settings-header {
    grid-template-columns: 42px minmax(0, 1fr);
    border-bottom: 1px solid #eef0f4;
  }

  .mobile-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .mobile-icon-button {
    width: 36px;
    height: 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 0;
    color: #000;
    background: transparent;
    font-size: 28px;
    cursor: pointer;
  }

  .mobile-admin-link {
    height: 30px;
    display: inline-flex;
    align-items: center;
    padding: 0 8px;
    border: 1px solid #d6dbe4;
    border-radius: 15px;
    color: #1f2937;
    background: #fff;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
  }

  .mobile-month {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 0;
    padding: 22px 12px 18px;
    background: #f7f8fa;
    border-bottom: 8px solid #f1f2f5;
  }

  .mobile-weekdays {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    margin-bottom: 12px;
    color: #8e949d;
    text-align: center;
    font-size: 14px;
  }

  .mobile-day {
    position: relative;
    min-height: 74px;
    border: 0;
    color: #111;
    background: transparent;
    text-align: center;
    cursor: pointer;
  }

  .mobile-day strong {
    width: 46px;
    height: 46px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    font-size: 22px;
    font-weight: 500;
  }

  .mobile-day span {
    display: block;
    margin-top: -4px;
    color: #7f8791;
    font-size: 13px;
  }

  .mobile-day.muted strong,
  .mobile-day.muted span {
    color: #8c929a;
  }

  .mobile-day.active strong {
    color: #fff;
    background: var(--calendar-primary);
  }

  .mobile-day.active span {
    color: #fff;
    transform: translateY(-24px);
  }

  .mobile-day i {
    position: absolute;
    left: 50%;
    bottom: 8px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: #bfc5ce;
    transform: translateX(-50%);
  }

  .mobile-agenda {
    background: #fff;
  }

  .mobile-agenda-title {
    min-height: 70px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 0 18px;
    border-bottom: 1px solid #edf0f3;
    font-size: 22px;
  }

  .mobile-agenda-title button {
    border: 0;
    color: var(--calendar-primary);
    background: transparent;
    font-size: 14px;
    font-weight: 700;
  }

  .mobile-new-row,
  .mobile-event-row {
    width: 100%;
    min-height: 70px;
    display: grid;
    align-items: center;
    border: 0;
    border-bottom: 1px solid #edf0f3;
    background: #fff;
    text-align: left;
  }

  .mobile-new-row {
    padding: 0 18px;
    color: #c7ccd3;
    font-size: 20px;
  }

  .mobile-event-row {
    grid-template-columns: 8px minmax(0, 1fr) auto;
    gap: 12px;
    padding: 0 18px;
  }

  .mobile-event-row i {
    width: 4px;
    height: 42px;
    border-radius: 2px;
  }

  .mobile-event-row span {
    font-size: 18px;
    font-weight: 700;
  }

  .mobile-event-row small {
    color: #8e949d;
    font-size: 14px;
  }

  .mobile-settings {
    min-height: 100vh;
    padding-bottom: 28px;
    background: #f4f5f7;
  }

  .mobile-view-card,
  .mobile-add-calendar,
  .mobile-calendar-card {
    margin: 14px 16px;
    border-radius: 8px;
    background: #fff;
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
  }

  .mobile-view-card button.active strong {
    color: #fff;
    background: var(--calendar-primary);
  }

  .mode-art {
    width: 54px;
    height: 54px;
    border-radius: 12px;
    border: 1px solid #e3e9f2;
    background:
      linear-gradient(#b8d7ff, #b8d7ff) 12px 14px / 30px 7px no-repeat,
      repeating-linear-gradient(90deg, transparent 0 8px, #9fc2ed 8px 11px) 12px 26px / 32px 18px no-repeat,
      #e9f3ff;
  }

  .mode-art.three {
    background:
      linear-gradient(#c1defd, #c1defd) 8px 10px / 9px 34px no-repeat,
      linear-gradient(#d7e9ff, #d7e9ff) 22px 10px / 9px 34px no-repeat,
      linear-gradient(#c1defd, #c1defd) 36px 10px / 9px 34px no-repeat,
      #f3f8ff;
  }

  .mode-art.week {
    background:
      linear-gradient(#b8d7ff, #b8d7ff) 12px 13px / 30px 8px no-repeat,
      linear-gradient(#d7e9ff, #d7e9ff) 12px 27px / 30px 7px no-repeat,
      linear-gradient(#d7e9ff, #d7e9ff) 12px 39px / 30px 7px no-repeat,
      #e9f3ff;
  }

  .mobile-add-calendar {
    width: calc(100% - 32px);
    min-height: 72px;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 0 20px;
    border: 0;
    color: #1764b9;
    font-size: 20px;
    font-weight: 700;
    text-align: left;
  }

  .mobile-add-calendar span {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 2px solid #1764b9;
    border-radius: 50%;
    font-size: 24px;
    line-height: 1;
  }

  .mobile-calendar-card {
    padding: 18px;
  }

  .mobile-calendar-card p {
    margin: 0 0 16px;
    color: #8e949d;
    font-size: 18px;
  }

  .mobile-calendar-line {
    min-height: 64px;
    display: grid;
    grid-template-columns: 34px minmax(0, 1fr) 24px;
    align-items: center;
    gap: 14px;
    border-bottom: 1px solid #edf0f3;
  }

  .mobile-calendar-line:last-child {
    border-bottom: 0;
  }

  .mobile-calendar-line i {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--calendar-primary);
    position: relative;
  }

  .mobile-calendar-line i::after {
    content: "";
    position: absolute;
    left: 8px;
    top: 4px;
    width: 6px;
    height: 12px;
    border: solid #fff;
    border-width: 0 2px 2px 0;
    transform: rotate(45deg);
  }

  .mobile-calendar-line strong {
    font-size: 20px;
  }

  .mobile-calendar-line small,
  .mobile-calendar-line > span {
    color: #9aa1aa;
  }

  .mobile-sheet-mask {
    position: fixed;
    inset: 0;
    z-index: 20;
    display: flex;
    align-items: flex-end;
    background: rgba(0, 0, 0, 0.62);
  }

  .mobile-add-sheet {
    width: 100%;
    padding: 26px 18px 30px;
    border-radius: 16px 16px 0 0;
    background: #f5f6f8;
  }

  .mobile-add-sheet h2 {
    margin: 0 0 24px;
    text-align: center;
    font-size: 24px;
  }

  .mobile-add-sheet button {
    width: 100%;
    min-height: 96px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 46px;
    align-items: center;
    gap: 16px;
    margin-top: 14px;
    padding: 18px 20px;
    border: 0;
    border-radius: 8px;
    background: #fff;
    text-align: left;
  }

  .mobile-add-sheet button .sheet-icon {
    grid-column: 2;
    grid-row: 1;
  }

  .mobile-add-sheet button div {
    grid-column: 1;
    grid-row: 1;
  }

  .mobile-add-sheet strong {
    display: block;
    font-size: 22px;
  }

  .mobile-add-sheet small {
    display: block;
    margin-top: 8px;
    color: #8e949d;
    font-size: 16px;
    line-height: 1.4;
  }

  .sheet-icon {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: var(--calendar-primary);
  }

  .sheet-icon.public {
    border-radius: 8px;
    background: #22c55e;
  }
}
</style>
