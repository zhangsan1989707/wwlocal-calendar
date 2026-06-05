<template>
  <el-dialog v-model="visible" :show-close="false" width="76vw" class="event-dialog">
    <div class="event-editor">
      <section class="event-form-pane">
        <button class="mobile-close-button" type="button" @click="visible = false">×</button>
        <el-input v-model="form.title" class="title-input" maxlength="100" placeholder="日程、活动主题" />

        <el-form :model="form" label-width="74px" class="compact-form">
          <el-form-item label="参与人">
            <el-select v-model="participantIds" multiple filterable placeholder="添加联系人">
              <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始" required>
            <div class="inline-fields">
              <el-date-picker v-model="startDate" type="date" format="M月D日 ddd" />
              <el-time-select v-model="startTime" start="08:00" step="00:15" end="20:00" />
              <el-checkbox v-model="form.all_day">全天</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item label="结束">
            <div class="inline-fields">
              <el-date-picker v-model="endDate" type="date" format="M月D日 ddd" />
              <el-time-select v-model="endTime" start="08:00" step="00:15" end="20:00" />
            </div>
          </el-form-item>
          <el-form-item label="时长">
            <el-select v-model="durationLabel">
              <el-option label="30分钟" value="30分钟" />
              <el-option label="1小时" value="1小时" />
              <el-option label="2小时" value="2小时" />
              <el-option label="自定义" value="自定义" />
            </el-select>
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" maxlength="300" placeholder="请输入地点" />
          </el-form-item>
          <el-form-item label="附件">
            <el-button class="attach-button">添加附件</el-button>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" maxlength="2000" :rows="5" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="日历" required>
            <el-select v-model="form.calendar_id" filterable>
              <el-option v-for="item in calendars" :key="item.id" :label="item.name" :value="item.id">
                <span class="tag-dot" :style="{ background: item.color }" />{{ item.name }}
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="提醒">
            <el-select v-model="reminderLabel">
              <el-option label="不提醒" value="不提醒" />
              <el-option label="即时" value="即时" />
              <el-option label="5分钟前" value="5分钟前" />
              <el-option label="15分钟前" value="15分钟前" />
              <el-option label="30分钟前" value="30分钟前" />
              <el-option label="1小时前" value="1小时前" />
              <el-option label="1天前" value="1天前" />
              <el-option label="自定义" value="自定义" />
            </el-select>
          </el-form-item>
          <el-form-item label="重复">
            <el-select v-model="form.recurrence_rule" clearable placeholder="不重复">
              <el-option label="每日" value="FREQ=DAILY;INTERVAL=1" />
              <el-option label="工作日" value="FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" />
              <el-option label="每周" value="FREQ=WEEKLY;INTERVAL=1" />
              <el-option label="双周" value="FREQ=WEEKLY;INTERVAL=2" />
              <el-option label="每月" value="FREQ=MONTHLY;INTERVAL=1" />
              <el-option label="每年" value="FREQ=YEARLY;INTERVAL=1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.allow_join">允许成员主动加入</el-checkbox>
          </el-form-item>
        </el-form>
      </section>

      <section class="availability-pane">
        <div class="availability-toolbar">
          <el-button @click="setToday">今天</el-button>
          <el-button :icon="ArrowLeft" />
          <el-button :icon="ArrowRight" />
          <strong>{{ availabilityTitle }}</strong>
        </div>
        <div class="availability-board">
          <div class="availability-member" v-for="member in availabilityMembers" :key="member.id">
            <span>{{ member.name }}</span>
          </div>
          <div class="availability-grid">
            <span v-for="hour in availabilityHours" :key="hour">{{ hour }}:00</span>
            <div
              v-for="slot in busySlots"
              :key="slot.id"
              class="busy-block"
              :style="slotStyle(slot)"
            >
              {{ slot.title }}
            </div>
            <button v-if="!busySlots.length" class="free-block">所有人都有空</button>
          </div>
        </div>
      </section>
    </div>
    <template #footer>
      <div class="event-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存日程</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { api } from '../api/http'
import type { CalendarItem, EventItem, TagColor, User } from '../api/types'

const props = defineProps<{
  modelValue: boolean
  event?: EventItem | null
  calendars: CalendarItem[]
  users: User[]
  tags: TagColor[]
  currentUserId: number
  initialStart?: Date | null
  initialEnd?: Date | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = reactive<Record<string, any>>({})
const participantIds = ref<number[]>([])
const todos = ref<Array<{ title: string; assigneeUserId?: number; priority: string; completed: boolean }>>([])
const startDate = ref(new Date())
const endDate = ref(new Date())
const startTime = ref('11:30')
const endTime = ref('12:30')
const durationLabel = ref('1小时')
const reminderLabel = ref('15分钟前')
const availabilityHours = Array.from({ length: 13 }, (_, index) => index + 5)
const availabilityTitle = computed(() => {
  const date = startDate.value
  return `${date.getMonth() + 1}月${date.getDate()}日 周${'日一二三四五六'[date.getDay()]}`
})

// Busy slots from freebusy API
interface BusySlot {
  id: number
  title: string
  start_at: string
  end_at: string
  all_day: boolean
  status: string
  response_status: string
}
const busySlots = ref<BusySlot[]>([])

const availabilityMembers = computed(() => {
  const members: Array<{ id: number; name: string }> = [
    { id: props.currentUserId, name: '我' }
  ]
  for (const pid of participantIds.value) {
    if (pid !== props.currentUserId) {
      const user = props.users.find(u => u.id === pid)
      members.push({ id: pid, name: user?.name || `用户${pid}` })
    }
  }
  return members
})

function slotStyle(slot: BusySlot) {
  const startHour = new Date(slot.start_at).getHours()
  const startMin = new Date(slot.start_at).getMinutes()
  const endHour = new Date(slot.end_at).getHours()
  const endMin = new Date(slot.end_at).getMinutes()

  const gridStartHour = 5 // first hour in grid
  const rowHeight = 43 // row height from CSS

  const top = (startHour - gridStartHour + startMin / 60) * rowHeight
  const height = Math.max(((endHour - startHour) + (endMin - startMin) / 60) * rowHeight, 20)

  return {
    top: `${top}px`,
    height: `${height}px`
  }
}

// Fetch busy slots when participants change
async function fetchBusySlots() {
  const ids = [...participantIds.value]
  if (!ids.length) {
    busySlots.value = []
    return
  }
  try {
    const startOfDay = new Date(startDate.value)
    startOfDay.setHours(0, 0, 0, 0)
    const endOfDay = new Date(startDate.value)
    endOfDay.setHours(23, 59, 59, 999)

    const result = await api.post<BusySlot[]>('/freebusy/query', {
      user_ids: ids,
      start_at: startOfDay.toISOString(),
      end_at: endOfDay.toISOString()
    })
    busySlots.value = result
  } catch {
    busySlots.value = []
  }
}

watch(participantIds, fetchBusySlots, { deep: true })
watch(startDate, () => {
  if (participantIds.value.length) fetchBusySlots()
})

watch(
  () => props.modelValue,
  () => {
    const firstCalendar = props.calendars[0]
    Object.assign(form, {
      id: props.event?.id,
      title: props.event?.title ?? '',
      calendar_id: props.event?.calendar_id ?? firstCalendar?.id,
      organizer_user_id: props.event?.organizer_user_id ?? props.currentUserId,
      location: props.event?.location ?? '',
      description: props.event?.description ?? '',
      tag_id: props.event?.tag_id ?? undefined,
      all_day: props.event?.all_day ?? false,
      recurrence_rule: props.event?.recurrence_rule ?? '',
      allow_join: props.event?.allow_join ?? false,
      status: 'ACTIVE'
    })
    const start = props.event ? new Date(props.event.start_at) : new Date(props.initialStart ?? new Date(2026, 5, 5, 11, 30))
    const end = props.event ? new Date(props.event.end_at) : new Date(props.initialEnd ?? new Date(start.getTime() + 60 * 60 * 1000))
    startDate.value = start
    endDate.value = end
    startTime.value = formatTime(start)
    endTime.value = formatTime(end)
    participantIds.value = []
    todos.value = []
  }
)

// Sync end time when duration changes
watch(durationLabel, (label) => {
  const durationMap: Record<string, number> = {
    '30分钟': 30,
    '1小时': 60,
    '2小时': 120
  }
  const minutes = durationMap[label]
  if (!minutes) return
  const start = mergeDateTime(startDate.value, startTime.value)
  const end = new Date(start.getTime() + minutes * 60 * 1000)
  endDate.value = end
  endTime.value = formatTime(end)
})

async function submit() {
  if (!form.title || !form.calendar_id || !startDate.value || !endDate.value) {
    ElMessage.warning('请填写必填信息')
    return
  }
  const start = mergeDateTime(startDate.value, startTime.value)
  const end = mergeDateTime(endDate.value, endTime.value)

  // Translate reminderLabel to reminders array for backend
  const reminders: Array<{ minutes_before: number; method: string }> = []
  const reminderLabelVal = reminderLabel.value
  if (reminderLabelVal === '即时') {
    reminders.push({ minutes_before: 0, method: 'SYSTEM' })
  } else if (reminderLabelVal === '1小时前') {
    reminders.push({ minutes_before: 60, method: 'SYSTEM' })
  } else if (reminderLabelVal === '1天前') {
    reminders.push({ minutes_before: 1440, method: 'SYSTEM' })
  } else {
    const reminderMatch = reminderLabelVal.match(/^(\d+)/)
    if (reminderMatch) {
      reminders.push({ minutes_before: parseInt(reminderMatch[1]), method: 'SYSTEM' })
    }
  }

  const payload = {
    ...form,
    start_at: start.toISOString(),
    end_at: end.toISOString(),
    participantIds: participantIds.value,
    reminders: reminders,
    todos: todos.value.filter((item) => item.title),
    operatorUserId: props.currentUserId
  }
  try {
    if (form.id && form.id > 0) {
      await api.put(`/events/${form.id}`, payload)
    } else {
      await api.post('/events', payload)
    }
    ElMessage.success('已保存')
    visible.value = false
    emit('saved')
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败，请稍后重试')
  }
}

function setToday() {
  const now = new Date()
  startDate.value = now
  endDate.value = now
}

function mergeDateTime(date: Date, time: string) {
  const [hour, minute] = time.split(':').map(Number)
  const result = new Date(date)
  result.setHours(hour || 0, minute || 0, 0, 0)
  return result
}

function formatTime(date: Date) {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.event-editor {
  display: grid;
  grid-template-columns: 43% 57%;
  height: min(640px, calc(100vh - 172px));
  min-height: 520px;
}

.event-form-pane {
  position: relative;
  padding: 32px 32px 28px;
  overflow: auto;
  border-right: 1px solid var(--calendar-border);
}

.mobile-close-button {
  display: none;
}

.availability-pane {
  min-width: 0;
  background: #fff;
}

.title-input {
  margin-bottom: 24px;
}

.title-input :deep(.el-input__wrapper) {
  height: 44px;
}

.compact-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.compact-form :deep(.el-form-item__label) {
  color: var(--calendar-text);
  font-size: 16px;
  font-weight: 700;
}

.inline-fields {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) 126px auto;
  gap: 10px;
  width: 100%;
}

.attach-button {
  width: 116px;
}

.tag-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 8px;
  border-radius: 50%;
}

.availability-toolbar {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 22px;
  border-bottom: 1px solid var(--calendar-border);
}

.availability-toolbar strong {
  margin-left: 10px;
  color: var(--calendar-primary);
  font-size: 18px;
}

.availability-board {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  height: calc(100% - 72px);
  overflow: hidden;
}

.availability-member {
  padding-top: 28px;
  border-right: 1px solid var(--calendar-border-soft);
  text-align: center;
}

.availability-member span {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  background: #77aaf9;
  font-weight: 700;
}

.availability-grid {
  position: relative;
  display: grid;
  grid-template-rows: repeat(13, 43px);
}

.availability-grid span {
  padding: 7px 12px;
  color: var(--calendar-muted);
  border-bottom: 1px solid var(--calendar-border-soft);
}

.free-block {
  position: absolute;
  left: 0;
  right: 0;
  top: 282px;
  height: 50px;
  padding: 0 18px;
  border: 1px solid #22c55e;
  border-left-width: 3px;
  border-radius: 3px;
  color: #16a34a;
  background: #e8f9ed;
  text-align: left;
  font-weight: 700;
}

.busy-block {
  position: absolute;
  left: 0;
  right: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px 8px;
  border: 1px solid #ef4444;
  border-left-width: 3px;
  border-radius: 3px;
  color: #b91c1c;
  background: #fef2f2;
  font-size: 12px;
  font-weight: 600;
}

.event-footer {
  height: 72px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 0 32px;
  border-top: 1px solid var(--calendar-border);
}

.event-footer .el-button--primary {
  width: 180px;
}

.todo-list {
  display: grid;
  width: 100%;
  gap: 8px;
}

.todo-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 120px 88px 72px;
  gap: 8px;
}

@media (max-width: 720px) {
  :global(.event-dialog.el-dialog) {
    width: 100vw;
    height: 100vh;
    margin: 0;
    background: #fff;
  }

  :global(.event-dialog .el-dialog__body) {
    height: calc(100vh - 96px);
    overflow: auto;
  }

  .event-editor {
    grid-template-columns: 1fr;
    height: auto;
    min-height: calc(100vh - 96px);
  }

  .event-form-pane {
    padding: 78px 0 24px;
    border-right: 0;
    overflow: visible;
  }

  .mobile-close-button {
    position: absolute;
    left: 18px;
    top: 24px;
    display: inline-flex;
    width: 36px;
    height: 36px;
    align-items: center;
    justify-content: center;
    border: 0;
    background: transparent;
    color: #000;
    font-size: 42px;
    line-height: 1;
  }

  .availability-pane {
    display: none;
  }

  .title-input {
    margin: 0;
    padding: 0 24px 34px;
    border-bottom: 8px solid #f3f4f6;
  }

  .title-input :deep(.el-input__wrapper) {
    height: 52px;
    padding-left: 0;
    border-radius: 0;
    box-shadow: none;
    border-left: 3px solid #18a999;
  }

  .title-input :deep(.el-input__inner) {
    padding-left: 8px;
    color: #111;
    font-size: 24px;
  }

  .compact-form :deep(.el-form-item) {
    min-height: 64px;
    margin: 0;
    padding: 0 24px;
    border-bottom: 1px solid #eef0f3;
  }

  .compact-form :deep(.el-form-item__label) {
    min-width: 88px;
    height: 64px;
    display: flex;
    align-items: center;
    color: #111;
    font-size: 22px;
    font-weight: 500;
  }

  .compact-form :deep(.el-form-item__content) {
    min-height: 64px;
    align-items: center;
    justify-content: flex-end;
  }

  .compact-form :deep(.el-input__wrapper),
  .compact-form :deep(.el-select__wrapper) {
    min-height: 40px;
    border-radius: 4px;
    background: #f5f6f8;
    box-shadow: none;
  }

  .compact-form :deep(.el-textarea__inner) {
    min-height: 92px;
    padding: 16px 0;
    border-radius: 0;
    background: #fff;
    box-shadow: none;
    font-size: 20px;
  }

  .inline-fields {
    grid-template-columns: minmax(92px, 1fr) 84px auto;
    justify-content: end;
  }

  .inline-fields :deep(.el-date-editor.el-input),
  .inline-fields :deep(.el-select),
  .inline-fields :deep(.el-input) {
    width: 100%;
  }

  .attach-button {
    width: auto;
    padding: 0;
    border: 0;
    color: #111;
    background: transparent;
    font-size: 22px;
    font-weight: 500;
  }

  .event-footer {
    height: 96px;
    padding: 14px 18px 22px;
    border-top: 8px solid #f3f4f6;
    background: #fff;
  }

  .event-footer .el-button:first-child {
    display: none;
  }

  .event-footer .el-button--primary {
    width: 100%;
    height: 56px;
    border-radius: 6px;
    font-size: 24px;
  }

  .todo-row {
    grid-template-columns: 1fr;
  }
}
</style>
