<template>
  <el-drawer v-model="visible" title="日程详情" size="420px">
    <template v-if="event">
      <h2>{{ event.title }}</h2>
      <div class="meta">
        <span>{{ format(event.start_at || event.start_time) }} 至 {{ format(event.end_at || event.end_time) }}</span>
        <span>{{ event.calendar_name }}</span>
        <span v-if="event.location">{{ event.location }}</span>
      </div>
      <p class="description">{{ event.description || '暂无描述' }}</p>
      <el-divider />
      <el-space wrap>
        <el-button type="primary" @click="$emit('edit', event)">编辑</el-button>
        <el-button @click="respond('ACCEPTED')">接受</el-button>
        <el-button @click="respond('DECLINED')">拒绝</el-button>
        <el-button @click="respond('TENTATIVE')">待定</el-button>
        <el-button type="danger" plain @click="$emit('remove', event)">删除</el-button>
      </el-space>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
import type { EventItem } from '../api/types'

const props = defineProps<{
  modelValue: boolean
  event?: EventItem | null
  currentUserId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  edit: [event: EventItem]
  remove: [event: EventItem]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

function format(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

async function respond(status: string) {
  if (!props.event) return
  await api.post(`/events/${props.event.id}/respond`, { userId: props.currentUserId, status })
  ElMessage.success('回执已更新')
}
</script>

<style scoped>
h2 {
  margin: 0 0 12px;
}

.meta {
  display: grid;
  gap: 8px;
  color: #5d667a;
  line-height: 1.5;
}

.description {
  margin-top: 18px;
  white-space: pre-wrap;
}
</style>
