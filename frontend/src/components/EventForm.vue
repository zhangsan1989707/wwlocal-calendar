<template>
  <el-dialog v-model="visible" :show-close="false" width="76vw" class="event-dialog">
    <div class="event-editor">
      <section class="event-form-pane">
        <button class="mobile-close-button" type="button" @click="visible = false">×</button>
        <el-input v-model="form.title" class="title-input" maxlength="100" placeholder="日程、活动主题" />

        <el-form :model="form" label-width="96px" class="compact-form">
          <el-form-item label="参会人">
            <el-select v-model="participantIds" multiple filterable placeholder="添加内部成员">
              <el-option v-for="item in users" :key="item.id" :label="item.name" :value="String(item.id)" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="participantIds.length > 0" label="可编辑">
            <el-select v-model="editorUserIds" multiple filterable placeholder="选择可编辑日程的成员">
              <el-option v-for="pid in participantIds" :key="pid" :label="getUserName(pid)" :value="pid" />
            </el-select>
          </el-form-item>
          <el-form-item label="外部联系人">
            <div style="display: flex; gap: 8px; align-items: flex-start;">
              <el-select v-model="externalContactIds" multiple filterable placeholder="添加外部联系人" style="flex:1">
                <el-option v-for="item in externalContacts" :key="item.id" :label="`${item.name} (${item.company || item.contact_type})`" :value="String(item.id)" />
              </el-select>
              <el-button type="success" size="small" @click="showAddExternalContact = true">+ 新增</el-button>
            </div>
          </el-form-item>
          <el-form-item label="开始" required>
            <div class="inline-fields" :class="{ 'all-day': form.all_day }">
              <el-date-picker v-model="startDate" type="date" format="M月D日 ddd" placeholder="选择日期" />
              <el-time-select v-if="!form.all_day" v-model="startTime" start="08:00" step="00:15" end="20:00" placeholder="选择时间" />
              <el-checkbox v-model="form.all_day">全天</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item label="结束">
            <div class="inline-fields" :class="{ 'all-day': form.all_day }">
              <el-date-picker v-model="endDate" type="date" format="M月D日 ddd" placeholder="选择日期" />
              <el-time-select v-if="!form.all_day" v-model="endTime" start="08:00" step="00:15" end="20:00" placeholder="选择时间" />
            </div>
          </el-form-item>
          <el-form-item v-if="!form.all_day" label="时长">
            <el-select v-model="durationLabel">
              <el-option label="30分钟" value="30分钟" />
              <el-option label="1小时" value="1小时" />
              <el-option label="2小时" value="2小时" />
              <el-option label="自定义" value="自定义" />
            </el-select>
          </el-form-item>
          <el-form-item label="时区">
            <el-select v-model="form.timezone" filterable clearable placeholder="选择时区（默认浏览器本地）">
              <el-option v-for="tz in timezoneOptions" :key="tz.value" :label="tz.label" :value="tz.value" />
            </el-select>
            <div v-if="form.timezone" class="form-hint">日期/时间按所选时区解释，存储时换算为 UTC</div>
          </el-form-item>
          <el-form-item label="参与部门">
            <el-select v-model="departmentIds" multiple filterable placeholder="添加部门（可选）">
              <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" maxlength="300" placeholder="请输入地点" />
          </el-form-item>
          <el-form-item label="附件">
            <div class="attachments-section">
              <input
                ref="fileInputRef"
                type="file"
                multiple
                style="display: none"
                @change="handleFileSelect"
              />
              <el-button class="attach-button" @click="fileInputRef?.click()">
                <el-icon><Upload /></el-icon>
                添加附件
              </el-button>
              <div v-if="attachments.length" class="attachments-list">
                <div v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
                  <span class="attachment-name">{{ attachment.file_name }}</span>
                  <span class="attachment-size">{{ formatFileSize(attachment.file_size) }}</span>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    size="small"
                    @click="deleteAttachment(attachment)"
                  />
                </div>
              </div>
              <div v-if="pendingAttachments.length" class="attachments-list">
                <div v-for="attachment in pendingAttachments" :key="attachment.key" class="attachment-item pending">
                  <span class="attachment-name">{{ attachment.file.name }}</span>
                  <span class="attachment-size">待保存 · {{ formatFileSize(attachment.file.size) }}</span>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    size="small"
                    @click="removePendingAttachment(attachment.key)"
                  />
                </div>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" maxlength="2000" :rows="3" placeholder="请输入描述" />
          </el-form-item>
          
          <el-form-item label="待办">
            <div class="todos-section">
              <div v-for="(todo, index) in todos" :key="index" class="todo-item">
                <el-input 
                  v-model="todo.title" 
                  class="todo-title" 
                  placeholder="待办事项标题"
                  size="small"
                />
                <el-select v-model="todo.priority" class="todo-priority" size="small">
                  <el-option label="低" value="LOW" />
                  <el-option label="中" value="MEDIUM" />
                  <el-option label="高" value="HIGH" />
                </el-select>
                <el-button type="danger" :icon="Delete" size="small" circle @click="removeTodo(index)" />
              </div>
              <el-button type="primary" size="small" plain @click="addTodo">
                + 添加待办
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="日历" required>
            <el-select v-model="form.calendar_id" filterable>
              <el-option v-for="item in calendars" :key="item.id" :label="item.name" :value="item.id">
                <span class="tag-dot" :style="{ background: item.color }" />{{ item.name }}
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tag_id" clearable placeholder="选择标签">
              <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id">
                <span class="tag-dot" :style="{ background: item.color }" />{{ item.name }}
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="提醒">
            <div class="reminder-row">
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
              <el-input-number
                v-if="reminderLabel === '自定义'"
                v-model="customReminderMinutes"
                :min="0"
                :max="10080"
                :step="5"
                size="small"
                class="reminder-custom-input"
              />
              <span v-if="reminderLabel === '自定义'" class="reminder-suffix">分钟前</span>
            </div>
          </el-form-item>
          <el-form-item label="重复">
            <el-select v-model="form.recurrence_rule" clearable placeholder="不重复" @change="onRecurrenceChange">
              <el-option label="每日" value="FREQ=DAILY;INTERVAL=1" />
              <el-option label="工作日" value="FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" />
              <el-option label="每周" value="FREQ=WEEKLY;INTERVAL=1" />
              <el-option label="双周" value="FREQ=WEEKLY;INTERVAL=2" />
              <el-option label="每月" value="FREQ=MONTHLY;INTERVAL=1" />
              <el-option label="每年" value="FREQ=YEARLY;INTERVAL=1" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="isEditingRecurrence && editScope === 'single'" label="提醒">
            <el-radio-group v-model="overrideReminder">
              <el-radio :value="false">继承原系列提醒</el-radio>
              <el-radio :value="true">自定义本次提醒</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="isEditingRecurrence && editScope === 'single' && overrideReminder" label="提醒档位">
            <div class="reminder-row">
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
              <el-input-number
                v-if="reminderLabel === '自定义'"
                v-model="customReminderMinutes"
                :min="0"
                :max="10080"
                :step="5"
                size="small"
                class="reminder-custom-input"
              />
              <span v-if="reminderLabel === '自定义'" class="reminder-suffix">分钟前</span>
            </div>
          </el-form-item>
          <el-form-item v-if="isEditingRecurrence" label="修改范围">
            <el-radio-group v-model="editScope">
              <el-radio value="single">仅修改本次</el-radio>
              <el-radio value="series">修改全系列</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.recurrence_rule" label="结束日期">
            <el-date-picker v-model="form.recurrence_end" type="date" placeholder="永不结束" clearable format="YYYY-MM-DD" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item v-if="form.recurrence_rule && !form.recurrence_end" label="重复次数">
            <el-input-number v-model="form.occurrence_count" :min="1" :max="365" placeholder="次数" />
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
        <el-checkbox v-if="isEditing" v-model="notifyParticipants" style="margin-right: auto; float: left; line-height: 32px;">
          保存并通知全部参会人
        </el-checkbox>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存日程</el-button>
      </div>
    </template>
  </el-dialog>

    <!-- 新增外部联系人弹窗 -->
    <el-dialog v-model="showAddExternalContact" title="新增外部联系人" width="400px" append-to-body>
      <el-form :model="newExternalContact" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="newExternalContact.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="newExternalContact.contact_type">
            <el-option label="微信联系人" value="wechat" />
            <el-option label="客户" value="client" />
            <el-option label="合作方" value="partner" />
          </el-select>
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="newExternalContact.company" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="newExternalContact.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="newExternalContact.phone" placeholder="请输入电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddExternalContact = false">取消</el-button>
        <el-button type="primary" @click="createExternalContact" :loading="savingExternalContact">保存</el-button>
      </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Upload, Delete } from '@element-plus/icons-vue'
import { api, uploadFile } from '../api/http'
import type { CalendarItem, EventItem, CalendarTag, User } from '../api/types'

interface Attachment {
  id: number
  file_name: string
  file_path: string
  file_size: number
  content_type: string
  created_at: string
}

interface PendingAttachment {
  key: string
  file: File
}

const props = defineProps<{
  modelValue: boolean
  event?: EventItem | null
  calendars: CalendarItem[]
  users: User[]
  tags: CalendarTag[]
  departments: Array<{ id: string; name: string }>
  currentUserId: string
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

const isEditing = computed(() => !!props.event?.id)
const isEditingRecurrence = computed(() => isEditing.value && (!!props.event?.recurrence_rule || !!props.event?.is_recurrence_instance))

const form = reactive<Record<string, any>>({})
const participantIds = ref<string[]>([])
const departmentIds = ref<string[]>([])
const todos = ref<Array<{ title: string; assigneeUserId?: string; priority: string; completed: boolean }>>([])
const attachments = ref<Attachment[]>([])
const pendingAttachments = ref<PendingAttachment[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const startDate = ref(new Date())
const endDate = ref(new Date())
const startTime = ref('11:30')
const endTime = ref('12:30')
const durationLabel = ref('1小时')
const reminderLabel = ref('15分钟前')
const customReminderMinutes = ref(10)
const editScope = ref<'single' | 'series'>('series')
const notifyParticipants = ref(true)
const overrideReminder = ref(false)
const externalContacts = ref<Array<{ id: number; name: string; company: string; contact_type: string; email: string; phone: string }>>([])
const externalContactIds = ref<string[]>([])
const editorUserIds = ref<string[]>([])
const showAddExternalContact = ref(false)
const savingExternalContact = ref(false)
const newExternalContact = reactive({
  name: '',
  contact_type: 'wechat',
  company: '',
  email: '',
  phone: ''
})
const availabilityHours = Array.from({ length: 13 }, (_, index) => index + 5)
const timezoneOptions = [
  { label: '北京时间 (UTC+8)', value: 'Asia/Shanghai' },
  { label: '东京 (UTC+9)', value: 'Asia/Tokyo' },
  { label: '首尔 (UTC+9)', value: 'Asia/Seoul' },
  { label: '新加坡 (UTC+8)', value: 'Asia/Singapore' },
  { label: '纽约 (UTC-5)', value: 'America/New_York' },
  { label: '洛杉矶 (UTC-8)', value: 'America/Los_Angeles' },
  { label: '伦敦 (UTC+0)', value: 'Europe/London' },
  { label: '巴黎 (UTC+1)', value: 'Europe/Paris' },
  { label: '悉尼 (UTC+10)', value: 'Australia/Sydney' },
  { label: '迪拜 (UTC+4)', value: 'Asia/Dubai' }
]
const availabilityTitle = computed(() => {
  const date = startDate.value
  return `${date.getMonth() + 1}月${date.getDate()}日 周${'日一二三四五六'[date.getDay()]}`
})

// Busy slots from freebusy API
interface BusySlot {
  id: string
  title: string
  start_at: string
  end_at: string
  all_day: boolean
  status: string
  response_status: string
}
const busySlots = ref<BusySlot[]>([])

const availabilityMembers = computed(() => {
  const members: Array<{ id: string; name: string }> = [
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
function getUserName(userId: string): string {
  return props.users.find(u => String(u.id) === String(userId))?.name || `用户${userId}`
}

async function fetchExternalContacts() {
  try {
    const res = await api.get<any[]>('/external-contacts')
    externalContacts.value = res
  } catch { /* ignore */ }
}

async function createExternalContact() {
  if (!newExternalContact.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  savingExternalContact.value = true
  try {
    const res = await api.post<any>('/external-contacts', {
      ...newExternalContact,
      created_by: props.currentUserId
    })
    externalContacts.value.push(res)
    externalContactIds.value.push(String(res.id))
    showAddExternalContact.value = false
    Object.assign(newExternalContact, { name: '', contact_type: 'wechat', company: '', email: '', phone: '' })
    ElMessage.success('外部联系人已添加')
  } catch (e: any) {
    console.error('外部联系人添加失败:', e)
    ElMessage.error(e?.message || '添加失败，请重试')
  } finally {
    savingExternalContact.value = false
  }
}

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
  async (val) => {
    if (!val) return
    fetchExternalContacts()
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
      timezone: props.event?.timezone ?? 'Asia/Shanghai',
      recurrence_rule: props.event?.recurrence_rule ?? '',
      status: 'ACTIVE'
    })
    const startStr = props.event?.start_at
    const endStr = props.event?.end_at
    const start = props.event ? new Date(startStr!) : new Date(props.initialStart ?? new Date(2026, 5, 5, 11, 30))
    const end = props.event ? new Date(endStr!) : new Date(props.initialEnd ?? new Date(start.getTime() + 60 * 60 * 1000))
    startDate.value = start
    endDate.value = end
    startTime.value = formatTime(start)
    endTime.value = formatTime(end)
    participantIds.value = []
    departmentIds.value = []
    externalContactIds.value = []
    editorUserIds.value = []
    todos.value = []
    attachments.value = []
    pendingAttachments.value = []
    
    // 如果是编辑模式，加载附件和参与人
    if (props.event?.id) {
      try {
        attachments.value = await api.get(`/events/${props.event.id}/attachments`)
      } catch {
        // 忽略错误
      }
      
      try {
        const participants = await api.get<any[]>(`/events/${props.event.id}/participants`)
        participantIds.value = participants
          .filter((p: any) => p.user_id)
          .map((p: any) => String(p.user_id))
        departmentIds.value = participants
          .filter((p: any) => p.department_id)
          .map((p: any) => String(p.department_id))
        externalContactIds.value = participants
          .filter((p: any) => p.external_contact_id)
          .map((p: any) => String(p.external_contact_id))
        editorUserIds.value = participants
          .filter((p: any) => p.role === 'EDITOR' && p.user_id)
          .map((p: any) => String(p.user_id))
      } catch {
        // 忽略错误
      }
      
      // 加载提醒
      try {
        const reminders = await api.get<any[]>(`/events/${props.event.id}/reminders`)
        if (reminders.length > 0) {
          const reminder = reminders[0]
          const minutes = reminder.minutes_before
          // 仅当用户尚未在弹窗打开后修改提醒档位时，才把数据库里的提醒回填
          // 避免异步加载覆盖用户已选的“单次特例提醒”等值
          if (reminderLabel.value === '15分钟前' && customReminderMinutes.value === 10) {
            // 根据分钟数设置对应的标签
            if (minutes === 0) {
              reminderLabel.value = '即时'
            } else if (minutes === 5) {
              reminderLabel.value = '5分钟前'
            } else if (minutes === 15) {
              reminderLabel.value = '15分钟前'
            } else if (minutes === 30) {
              reminderLabel.value = '30分钟前'
            } else if (minutes === 60) {
              reminderLabel.value = '1小时前'
            } else if (minutes === 1440) {
              reminderLabel.value = '1天前'
            } else {
              reminderLabel.value = '自定义'
              customReminderMinutes.value = Number(minutes) || 10
            }
          }
        }
      } catch {
        // 忽略错误
      }
      
      // 加载待办事项
      try {
        const eventTodos = await api.get<any[]>(`/events/${props.event.id}/todos`)
        todos.value = eventTodos.map((todo: any) => ({
          title: todo.title || '',
          assigneeUserId: todo.assignee_user_id || undefined,
          priority: todo.priority || 'MEDIUM',
          completed: todo.completed || false
        }))
      } catch {
        // 忽略错误
      }
    }
  }
)

async function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || !files.length) return

  for (const file of Array.from(files)) {
    pendingAttachments.value.push({
      key: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      file
    })
  }
  ElMessage.success(files.length > 1 ? `已添加 ${files.length} 个附件，保存日程后上传` : '已添加附件，保存日程后上传')
  if (target) {
    target.value = ''
  }
}

function removePendingAttachment(key: string) {
  pendingAttachments.value = pendingAttachments.value.filter((item) => item.key !== key)
}

async function uploadPendingAttachments(eventId: number | string) {
  if (!pendingAttachments.value.length) return
  const uploaded: Attachment[] = []
  for (const attachment of pendingAttachments.value) {
    const formData = new FormData()
    formData.append('file', attachment.file)
    formData.append('eventId', String(eventId))
    formData.append('userId', props.currentUserId)
    const result = await uploadFile('/files/upload', formData)
    uploaded.push(result)
  }
  attachments.value.push(...uploaded)
  pendingAttachments.value = []
}

async function deleteAttachment(attachment: Attachment) {
  try {
    await api.post(`/attachments/${attachment.id}/delete`, { operatorUserId: props.currentUserId })
    attachments.value = attachments.value.filter(a => a.id !== attachment.id)
    ElMessage.success('附件已删除')
  } catch {
    ElMessage.error('删除附件失败')
  }
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function addTodo() {
  todos.value.push({
    title: '',
    priority: 'MEDIUM',
    completed: false
  })
}

function removeTodo(index: number) {
  todos.value.splice(index, 1)
}

function onRecurrenceChange() {
  if (!form.recurrence_rule) {
    form.recurrence_end = undefined
    form.occurrence_count = undefined
  }
}

// Sync end time when duration changes
watch(durationLabel, (label) => {
  if (form.all_day) return
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
  // 把用户在表单里选的墙钟时间按"事件时区"换算为 UTC
  // 若事件时区为空/非法则回退到浏览器本地时区
  let eventTimezone: string | undefined = form.timezone
  if (eventTimezone) {
    try {
      new Intl.DateTimeFormat('en-US', { timeZone: eventTimezone }).format(new Date())
    } catch {
      eventTimezone = undefined
    }
  }
  const start = eventTimezone
    ? zonedDateTimeToUtc(startDate.value, form.all_day ? '00:00' : startTime.value, eventTimezone)
    : mergeDateTime(startDate.value, form.all_day ? '00:00' : startTime.value)
  const end = eventTimezone
    ? zonedDateTimeToUtc(endDate.value, form.all_day ? '23:59' : endTime.value, eventTimezone)
    : mergeDateTime(endDate.value, form.all_day ? '23:59' : endTime.value)

  // Translate reminderLabel to reminders array for backend
  const reminders: Array<{ minutes_before: number; method: string }> = []
  const reminderLabelVal = reminderLabel.value
  if (reminderLabelVal === '不提醒') {
    // 不提醒：不写入任何提醒行
  } else if (reminderLabelVal === '即时') {
    reminders.push({ minutes_before: 0, method: 'SYSTEM' })
  } else if (reminderLabelVal === '1小时前') {
    reminders.push({ minutes_before: 60, method: 'SYSTEM' })
  } else if (reminderLabelVal === '1天前') {
    reminders.push({ minutes_before: 1440, method: 'SYSTEM' })
  } else if (reminderLabelVal === '自定义') {
    const minutes = Number(customReminderMinutes.value)
    if (Number.isFinite(minutes) && minutes >= 0) {
      reminders.push({ minutes_before: minutes, method: 'SYSTEM' })
    }
  } else {
    const reminderMatch = reminderLabelVal.match(/^(\d+)/)
    if (reminderMatch) {
      reminders.push({ minutes_before: parseInt(reminderMatch[1]), method: 'SYSTEM' })
    }
  }

  function reminderMinutes(): number {
    if (reminderLabelVal === '不提醒') return -1
    if (reminderLabelVal === '即时') return 0
    if (reminderLabelVal === '1小时前') return 60
    if (reminderLabelVal === '1天前') return 1440
    if (reminderLabelVal === '自定义') return Number(customReminderMinutes.value) || 0
    const match = reminderLabelVal.match(/^(\d+)/)
    return match ? parseInt(match[1]) : 15
  }

  const payload = {
    ...form,
    start_at: start.toISOString(),
    end_at: end.toISOString(),
    participantIds: participantIds.value,
    externalContactIds: externalContactIds.value,
    editorUserIds: editorUserIds.value,
    departmentIds: departmentIds.value,
    reminders: reminders,
    todos: todos.value.filter((item) => item.title),
    operatorUserId: props.currentUserId,
    rrule: form.recurrence_rule || undefined,
    recurrence_end: form.recurrence_end || undefined,
    occurrence_count: form.occurrence_count || undefined,
    editSingle: isEditingRecurrence.value && editScope.value === 'single',
    originalStartAt: isEditingRecurrence.value && editScope.value === 'single'
      ? (props.event?.original_start_at ?? props.event?.start_at ?? undefined)
      : undefined,
    notifyParticipants: isEditing.value && notifyParticipants.value,
    overrideReminder: isEditingRecurrence.value && editScope.value === 'single' && overrideReminder.value,
    reminderOverrideMinutes: isEditingRecurrence.value && editScope.value === 'single' && overrideReminder.value
      ? reminderMinutes()
      : undefined
  }
  try {
    let savedEvent: any
    if (form.id) {
      savedEvent = await api.put(`/events/${form.id}`, payload)
    } else {
      savedEvent = await api.post('/events', payload)
    }
    const savedEventId = savedEvent?.id ?? form.id
    await uploadPendingAttachments(savedEventId)
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

// 把"用户在表单里选的本地墙钟时间 + 时区"换算为 UTC Date 对象
// 例如：date=2026-06-06, time=10:00, tz=America/Los_Angeles → UTC 17:00 (PST) 或 18:00 (PDT)
function zonedDateTimeToUtc(date: Date, time: string, timeZone: string): Date {
  const [hour, minute] = time.split(':').map(Number)
  const y = date.getFullYear()
  const m = date.getMonth()
  const d = date.getDate()
  // 把输入当作 UTC 来构造一个"假 UTC"时间
  const fakeUtc = new Date(Date.UTC(y, m, d, hour || 0, minute || 0, 0))
  // 计算目标时区在这个 instant 下的偏移（毫秒）
  const fmt = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false
  })
  const parts = fmt.formatToParts(fakeUtc)
  const get = (t: string) => +parts.find(p => p.type === t)!.value
  // 注意：hour 可能为 "24"（午夜特殊情况），需要取模
  const tzAsUtc = Date.UTC(
    get('year'), get('month') - 1, get('day'),
    get('hour') % 24, get('minute'), get('second')
  )
  const offsetMs = tzAsUtc - fakeUtc.getTime()
  // 真实的 UTC 时刻 = 假 UTC - 偏移
  return new Date(fakeUtc.getTime() - offsetMs)
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

.reminder-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.reminder-row .reminder-custom-input {
  width: 130px;
}
.reminder-row .reminder-suffix {
  color: #909399;
  font-size: 12px;
}
.form-hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 2px;
}

.event-form-pane {
  position: relative;
  padding: 32px 32px 28px;
  overflow: auto;
  border-right: 1px solid #e5e8ef;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, sans-serif;
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

.compact-form {
  --el-form-label-width: 96px;
}

.compact-form :deep(.el-form-item__label) {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  word-break: keep-all;
  width: 96px !important;
  min-width: 96px;
}

.compact-form :deep(.el-input__wrapper) {
  border-radius: 6px;
  border-color: #e5e8ef;
  box-shadow: none;
  transition: all 0.2s ease;
}

.compact-form :deep(.el-input__wrapper:hover) {
  border-color: #2f7cf6;
}

.compact-form :deep(.el-input__wrapper.is-focus) {
  border-color: #2f7cf6;
  box-shadow: 0 0 0 2px rgba(47, 124, 246, 0.1);
}

.compact-form :deep(.el-select__wrapper) {
  border-radius: 6px;
  border-color: #e5e8ef;
  box-shadow: none;
}

.compact-form :deep(.el-select__wrapper:hover) {
  border-color: #2f7cf6;
}

.compact-form :deep(.el-select__wrapper.is-focus) {
  border-color: #2f7cf6;
  box-shadow: 0 0 0 2px rgba(47, 124, 246, 0.1);
}

.compact-form :deep(.el-select-dropdown) {
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border-color: #e5e8ef;
}

.compact-form :deep(.el-select-dropdown__item) {
  padding: 10px 16px;
  font-size: 14px;
  color: #172033;
}

.compact-form :deep(.el-select-dropdown__item:hover) {
  background: #f3f6fb;
}

.compact-form :deep(.el-select-dropdown__item.selected) {
  color: #2f7cf6;
  background: #e8f0fe;
}

.inline-fields {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) 126px auto;
  gap: 10px;
  width: 100%;
}

/* 全天模式：隐藏时间选择器，只保留日期选择器 + 复选框（开始行）或仅日期选择器（结束行） */
.inline-fields.all-day {
  grid-template-columns: minmax(160px, 1fr) auto;
}

.attach-button {
  width: 116px;
}

.attachments-section {
  width: 100%;
}

.attachments-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.attachment-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--calendar-border);
  border-radius: 4px;
  background: var(--calendar-bg);
}

.attachment-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-size {
  font-size: 12px;
  color: var(--calendar-muted);
}

.todos-section {
  width: 100%;
}

.todo-item {
  display: grid;
  grid-template-columns: 1fr 100px auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.todo-title {
  flex: 1;
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
  grid-row: 1 / -1;
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
    border-radius: 0;
  }

  :global(.event-dialog .el-dialog__body) {
    height: calc(100vh - 96px);
    overflow: auto;
    padding: 0;
  }

  :global(.event-dialog .el-dialog__header) {
    display: none;
  }

  :global(.event-dialog .el-dialog__footer) {
    padding: 0;
    border: 0;
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
    color: #111;
    font-size: 36px;
    line-height: 1;
    cursor: pointer;
    z-index: 10;
  }

  .availability-pane {
    display: none;
  }

  .title-input {
    margin: 0;
    padding: 0 24px 24px;
    border-bottom: 6px solid #f3f4f6;
  }

  .title-input :deep(.el-input__wrapper) {
    height: 48px;
    padding-left: 0;
    border-radius: 0;
    box-shadow: none;
    border-left: 3px solid #2f7cf6;
    border-right: 0;
    border-top: 0;
    border-bottom: 0;
  }

  .title-input :deep(.el-input__inner) {
    padding-left: 12px;
    color: #172033;
    font-size: 18px;
    font-weight: 600;
  }

  .compact-form :deep(.el-form-item) {
    min-height: 72px;
    margin: 0;
    padding: 0 24px;
    border-bottom: 1px solid #e5e8ef;
    display: flex;
    align-items: center;
  }

  .compact-form :deep(.el-form-item__label) {
    min-width: 100px;
    width: 100px !important;
    height: 72px;
    display: flex;
    align-items: center;
    color: #172033;
    font-size: 16px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .compact-form :deep(.el-form-item__content) {
    min-height: 72px;
    align-items: center;
    justify-content: flex-end;
    flex: 1;
  }

  .compact-form :deep(.el-input__wrapper),
  .compact-form :deep(.el-select__wrapper) {
    min-height: 44px;
    border-radius: 8px;
    background: #f8f9fa;
    box-shadow: none;
    border-color: #e5e8ef;
  }

  .compact-form :deep(.el-input__inner) {
    font-size: 16px;
    color: #172033;
  }

  .compact-form :deep(.el-select__placeholder) {
    font-size: 16px;
    color: #9aa4b2;
  }

  .compact-form :deep(.el-textarea__inner) {
    min-height: 100px;
    padding: 12px;
    border-radius: 8px;
    background: #f8f9fa;
    box-shadow: none;
    font-size: 16px;
    border-color: #e5e8ef;
  }

  .inline-fields {
    grid-template-columns: minmax(120px, 1fr) 100px auto;
    justify-content: end;
    gap: 8px;
  }

  /* 移动端全天模式 */
  .inline-fields.all-day {
    grid-template-columns: minmax(120px, 1fr) auto;
  }

  .inline-fields :deep(.el-date-editor.el-input),
  .inline-fields :deep(.el-select),
  .inline-fields :deep(.el-input) {
    width: 100%;
  }

  .inline-fields :deep(.el-date-editor .el-input__inner) {
    font-size: 16px;
  }

  .inline-fields :deep(.el-time-select .el-input__inner) {
    font-size: 16px;
  }

  .inline-fields :deep(.el-checkbox) {
    margin-top: 8px;
  }

  .inline-fields :deep(.el-checkbox__label) {
    font-size: 14px;
  }

  .attach-button {
    width: auto;
    padding: 12px 16px;
    border: 0;
    color: #2f7cf6;
    background: transparent;
    font-size: 16px;
    font-weight: 600;
    border-radius: 0;
    text-decoration: underline;
  }

  .event-footer {
    height: 100px;
    padding: 16px 24px;
    border-top: 6px solid #f3f4f6;
    background: #fff;
    position: sticky;
    bottom: 0;
    z-index: 10;
  }

  .event-footer .el-button:first-child {
    display: none;
  }

  .event-footer .el-button--primary {
    width: 100%;
    height: 52px;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .todo-row {
    grid-template-columns: 1fr;
  }

  .todo-item {
    grid-template-columns: 1fr 100px auto;
    gap: 8px;
  }

  .todo-item :deep(.el-input__wrapper) {
    min-height: 44px;
  }

  .todo-item :deep(.el-select__wrapper) {
    min-height: 44px;
  }
}
</style>
