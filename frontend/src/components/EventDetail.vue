<template>
  <el-drawer v-model="visible" :show-close="false" size="440px" class="event-detail-drawer">
    <template #header>
      <div class="drawer-header">
        <span class="drawer-label">日程详情</span>
        <el-icon class="drawer-close" @click="visible = false"><Close /></el-icon>
      </div>
    </template>
    <template v-if="event">
      <div class="event-detail-body">
        <div class="event-title-row">
          <div class="event-color-bar" :style="{ background: event.calendar_color || event.tag_color || 'var(--calendar-primary)' }" />
          <h2>{{ event.title }}</h2>
        </div>

        <div class="meta-card">
          <div class="meta-row">
            <div class="meta-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="meta-content">
              <span class="meta-label">时间</span>
              <span class="meta-value">{{ format(event.start_at) }} - {{ format(event.end_at) }}</span>
              <span v-if="event.all_day" class="meta-tag">全天</span>
            </div>
          </div>

          <div v-if="event.calendar_name" class="meta-row">
            <div class="meta-icon">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="meta-content">
              <span class="meta-label">日历</span>
              <span class="meta-value">
                <i class="calendar-dot" :style="{ background: event.calendar_color || 'var(--calendar-primary)' }" />
                {{ event.calendar_name }}
              </span>
            </div>
          </div>

          <div v-if="event.location" class="meta-row">
            <div class="meta-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="meta-content">
              <span class="meta-label">地点</span>
              <span class="meta-value">{{ event.location }}</span>
            </div>
          </div>

          <div v-if="event.description" class="meta-row meta-row-description">
            <div class="meta-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="meta-content">
              <span class="meta-label">描述</span>
              <p class="meta-description">{{ event.description }}</p>
            </div>
          </div>
        </div>

        <div class="action-row">
          <el-button type="primary" @click="$emit('edit', event)">
            <el-icon><Edit /></el-icon>
            编辑日程
          </el-button>
        </div>

        <div class="respond-row">
          <span class="respond-label">快速回执</span>
          <div class="respond-buttons">
            <el-button @click="respond('ACCEPTED')">接受</el-button>
            <el-button @click="respond('TENTATIVE')">待定</el-button>
            <el-button @click="respond('DECLINED')">拒绝</el-button>
          </div>
        </div>

        <div class="danger-zone">
          <el-button type="danger" plain @click="$emit('remove', event)">
            <el-icon><Delete /></el-icon>
            删除日程
          </el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Close, Delete, Document, Edit, Location, Tickets } from '@element-plus/icons-vue'
import { api } from '../api/http'
import type { EventItem } from '../api/types'

const props = defineProps<{
  modelValue: boolean
  event?: EventItem | null
  currentUserId: string
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
  try {
    await api.post(`/events/${props.event.id}/respond`, { userId: props.currentUserId, status })
    ElMessage.success('回执已更新')
  } catch {
    ElMessage.error('回执更新失败')
  }
}
</script>

<style scoped>
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid var(--calendar-border-soft);
}

.drawer-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.drawer-close {
  cursor: pointer;
  color: var(--calendar-muted);
  font-size: 18px;
  transition: color 0.15s ease;
}

.drawer-close:hover {
  color: var(--calendar-text);
}

.event-detail-body {
  padding: 24px 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.event-title-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.event-color-bar {
  width: 4px;
  height: 32px;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 4px;
}

.event-title-row h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.meta-card {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid var(--calendar-border);
  border-radius: var(--calendar-radius);
  overflow: hidden;
  box-shadow: var(--calendar-shadow-sm);
}

.meta-row {
  display: grid;
  grid-template-columns: 40px 1fr;
  align-items: flex-start;
  gap: 0;
  padding: 14px 16px;
  background: var(--calendar-surface);
  border-bottom: 1px solid var(--calendar-border-soft);
}

.meta-row:last-child {
  border-bottom: 0;
}

.meta-row-description {
  align-items: flex-start;
}

.meta-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 2px;
  color: var(--calendar-muted);
  font-size: 16px;
}

.meta-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--calendar-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--calendar-primary-bg);
  color: var(--calendar-primary);
  font-size: 12px;
  font-weight: 700;
}

.meta-description {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--calendar-soft-text);
  line-height: 1.6;
  white-space: pre-wrap;
}

.calendar-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.action-row {
  display: flex;
}

.action-row .el-button {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
}

.respond-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.respond-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.respond-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.respond-buttons .el-button {
  font-weight: 600;
  height: 38px;
}

.danger-zone {
  display: flex;
  padding-top: 8px;
  border-top: 1px solid var(--calendar-border-soft);
}

.danger-zone .el-button {
  font-weight: 600;
}
</style>
