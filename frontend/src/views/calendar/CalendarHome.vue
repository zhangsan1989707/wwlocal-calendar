<template>
  <div class="page-shell">
    <header class="topbar">
      <div class="brand">企业协同日历 H5 系统</div>
      <div class="toolbar">
        <el-select v-model="store.currentUserId" size="small" class="user-select">
          <el-option v-for="user in store.users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
        <router-link to="/calendar/search"><el-button>搜索</el-button></router-link>
        <router-link to="/admin"><el-button>管理端</el-button></router-link>
      </div>
    </header>

    <main class="calendar-layout">
      <aside class="side panel">
        <el-calendar v-model="selectedDate" />
        <h3>日历列表</h3>
        <el-checkbox-group v-model="visibleCalendarIds">
          <el-checkbox v-for="item in store.calendars" :key="item.id" :label="item.id">
            <span class="calendar-chip" :style="{ background: item.color }" />{{ item.name }}
          </el-checkbox>
        </el-checkbox-group>
      </aside>

      <section class="main panel">
        <div class="calendar-actions">
          <el-button type="primary" @click="openCreate">创建日程</el-button>
          <el-button @click="goToday">今天</el-button>
          <el-button @click="move(-1)">上一周期</el-button>
          <el-button @click="move(1)">下一周期</el-button>
          <strong>{{ title }}</strong>
          <el-segmented v-model="view" :options="viewOptions" />
          <el-button @click="exportEvents">导出</el-button>
        </div>

        <div v-if="view === 'day' || view === 'week'" class="time-grid">
          <div class="hour-label" v-for="hour in hours" :key="hour">{{ hour }}:00</div>
          <div class="time-column">
            <button
              v-for="event in filteredEvents"
              :key="event.id"
              class="event-block"
              :style="{ borderColor: event.calendar_color || event.tag_color || '#2563eb' }"
              @click="selectEvent(event)"
            >
              <span>{{ event.title }}</span>
              <small>{{ timeText(event) }}</small>
            </button>
          </div>
        </div>

        <div v-else-if="view === 'month'" class="month-grid">
          <div v-for="day in monthDays" :key="day.toISOString()" class="month-cell">
            <strong>{{ day.getDate() }}</strong>
            <button v-for="event in eventsByDay(day)" :key="event.id" class="month-event" @click="selectEvent(event)">
              {{ event.title }}
            </button>
          </div>
        </div>

        <div v-else class="list-view">
          <button v-for="event in filteredEvents" :key="event.id" class="list-row" @click="selectEvent(event)">
            <span>{{ event.title }}</span>
            <small>{{ timeText(event) }} · {{ event.calendar_name }}</small>
          </button>
        </div>
      </section>
    </main>

    <EventForm
      v-model="formVisible"
      :event="editingEvent"
      :calendars="store.calendars"
      :users="store.users"
      :tags="store.tags"
      :current-user-id="store.currentUserId"
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
import EventDetail from '../../components/EventDetail.vue'
import EventForm from '../../components/EventForm.vue'
import { api } from '../../api/http'
import type { EventItem } from '../../api/types'
import { useAppStore } from '../../stores/app'

const store = useAppStore()
const selectedDate = ref(new Date())
const view = ref('week')
const viewOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '列表', value: 'list' }
]
const visibleCalendarIds = ref<number[]>([])
const formVisible = ref(false)
const detailVisible = ref(false)
const editingEvent = ref<EventItem | null>(null)
const selectedEvent = ref<EventItem | null>(null)
const hours = Array.from({ length: 13 }, (_, index) => index + 8)

const title = computed(() => selectedDate.value.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }))
const filteredEvents = computed(() => store.events.filter((event) => visibleCalendarIds.value.includes(event.calendar_id)))
const monthDays = computed(() => {
  const date = selectedDate.value
  const first = new Date(date.getFullYear(), date.getMonth(), 1)
  const start = new Date(first)
  start.setDate(first.getDate() - first.getDay())
  return Array.from({ length: 42 }, (_, index) => {
    const item = new Date(start)
    item.setDate(start.getDate() + index)
    return item
  })
})

onMounted(async () => {
  await store.loadBase()
  visibleCalendarIds.value = store.calendars.map((item) => item.id)
  await reload()
})

watch(selectedDate, reload)

async function reload() {
  await store.loadEvents('')
}

function openCreate() {
  editingEvent.value = null
  formVisible.value = true
}

function selectEvent(event: EventItem) {
  selectedEvent.value = event
  detailVisible.value = true
}

function editEvent(event: EventItem) {
  selectedEvent.value = null
  detailVisible.value = false
  editingEvent.value = event
  formVisible.value = true
}

async function removeEvent(event: EventItem) {
  await ElMessageBox.confirm('确认删除该日程？')
  await api.delete(`/events/${event.id}?operatorUserId=${store.currentUserId}`)
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

function timeText(event: EventItem) {
  if (event.all_day) return '全天'
  return `${new Date(event.start_time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${new Date(event.end_time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

function eventsByDay(day: Date) {
  return filteredEvents.value.filter((event) => new Date(event.start_time).toDateString() === day.toDateString()).slice(0, 4)
}

async function exportEvents() {
  await api.post('/export/events', { operatorUserId: store.currentUserId })
  ElMessage.success('导出任务已创建')
}
</script>

<style scoped>
.calendar-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
}

.side {
  padding: 12px;
}

.side :deep(.el-calendar-table .el-calendar-day) {
  height: 42px;
}

.calendar-chip {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 8px;
  border-radius: 50%;
}

.main {
  min-height: calc(100vh - 88px);
  padding: 16px;
}

.calendar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.user-select {
  width: 140px;
}

.time-grid {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  min-height: 680px;
  border-top: 1px solid #e5eaf3;
}

.hour-label {
  height: 52px;
  padding-top: 8px;
  color: #6b7280;
  border-bottom: 1px solid #eef2f7;
}

.time-column {
  grid-row: 1 / span 13;
  grid-column: 2;
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 8px;
  border-left: 1px solid #e5eaf3;
}

.event-block,
.list-row,
.month-event {
  width: 100%;
  border: 1px solid #d8e0ec;
  border-left-width: 4px;
  background: #fff;
  color: #172033;
  text-align: left;
  cursor: pointer;
}

.event-block {
  display: grid;
  gap: 4px;
  padding: 10px;
  border-radius: 6px;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
  border-top: 1px solid #e5eaf3;
  border-left: 1px solid #e5eaf3;
}

.month-cell {
  min-height: 132px;
  padding: 8px;
  border-right: 1px solid #e5eaf3;
  border-bottom: 1px solid #e5eaf3;
}

.month-event {
  margin-top: 6px;
  padding: 5px 7px;
  border-radius: 4px;
}

.list-view {
  display: grid;
  gap: 8px;
}

.list-row {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .calendar-layout {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .month-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
