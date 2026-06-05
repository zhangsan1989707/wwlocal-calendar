<template>
  <el-dialog v-model="visible" :title="form.id ? '编辑日程' : '创建日程'" width="680px" class="event-dialog">
    <el-form :model="form" label-width="96px">
      <el-form-item label="日程标题" required>
        <el-input v-model="form.title" maxlength="100" />
      </el-form-item>
      <el-form-item label="日历" required>
        <el-select v-model="form.calendar_id" filterable>
          <el-option v-for="item in calendars" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间" required>
        <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" />
      </el-form-item>
      <el-form-item label="全天">
        <el-switch v-model="form.all_day" />
      </el-form-item>
      <el-form-item label="参会人">
        <el-select v-model="participantIds" multiple filterable>
          <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.location" maxlength="300" />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tag" clearable>
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.name">
            <span class="tag-dot" :style="{ background: tag.color }" />{{ tag.name }}
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="重复">
        <el-select v-model="form.recurrence_rule" clearable>
          <el-option label="每日" value="FREQ=DAILY;INTERVAL=1" />
          <el-option label="工作日" value="FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" />
          <el-option label="每周" value="FREQ=WEEKLY;INTERVAL=1" />
          <el-option label="双周" value="FREQ=WEEKLY;INTERVAL=2" />
          <el-option label="每月" value="FREQ=MONTHLY;INTERVAL=1" />
          <el-option label="每年" value="FREQ=YEARLY;INTERVAL=1" />
        </el-select>
      </el-form-item>
      <el-form-item label="允许加入">
        <el-switch v-model="form.allow_join" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" maxlength="2000" :rows="4" />
      </el-form-item>
      <el-form-item label="待办">
        <div class="todo-list">
          <div v-for="(todo, index) in todos" :key="index" class="todo-row">
            <el-input v-model="todo.title" placeholder="任务内容" />
            <el-select v-model="todo.assigneeUserId" placeholder="负责人">
              <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="todo.priority">
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
            </el-select>
            <el-checkbox v-model="todo.completed">完成</el-checkbox>
          </div>
          <el-button @click="todos.push({ title: '', priority: 'MEDIUM', completed: false })">新增待办</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
import type { CalendarItem, EventItem, TagColor, User } from '../api/types'

const props = defineProps<{
  modelValue: boolean
  event?: EventItem | null
  calendars: CalendarItem[]
  users: User[]
  tags: TagColor[]
  currentUserId: number
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
const timeRange = ref<[Date, Date]>([new Date(), new Date(Date.now() + 60 * 60 * 1000)])

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
      tag: props.event?.tag ?? '',
      tag_color: props.event?.tag_color ?? '',
      all_day: props.event?.all_day ?? false,
      recurrence_rule: props.event?.recurrence_rule ?? '',
      allow_join: props.event?.allow_join ?? false,
      status: 'ACTIVE'
    })
    timeRange.value = [
      props.event ? new Date(props.event.start_time) : new Date(),
      props.event ? new Date(props.event.end_time) : new Date(Date.now() + 60 * 60 * 1000)
    ]
    participantIds.value = []
    todos.value = []
  }
)

async function submit() {
  if (!form.title || !form.calendar_id || !timeRange.value?.[0] || !timeRange.value?.[1]) {
    ElMessage.warning('请填写必填信息')
    return
  }
  const payload = {
    ...form,
    start_time: timeRange.value[0].toISOString(),
    end_time: timeRange.value[1].toISOString(),
    participantIds: participantIds.value,
    todos: todos.value.filter((item) => item.title),
    operatorUserId: props.currentUserId
  }
  if (form.id) {
    await api.put(`/events/${form.id}`, payload)
  } else {
    await api.post('/events', payload)
  }
  ElMessage.success('已保存')
  visible.value = false
  emit('saved')
}
</script>

<style scoped>
.tag-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 8px;
  border-radius: 50%;
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
  .todo-row {
    grid-template-columns: 1fr;
  }
}
</style>
