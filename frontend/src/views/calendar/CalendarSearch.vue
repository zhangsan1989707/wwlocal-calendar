<template>
  <div class="page-shell">
    <header class="topbar">
      <router-link class="back-btn" to="/calendar">
        <el-icon><ArrowLeft /></el-icon>
      </router-link>
      <div class="brand">日程搜索</div>
    </header>
    <main class="content">
      <section class="panel search-panel">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="标题、地点、描述"
            clearable
            :prefix-icon="Search"
            class="search-input"
          />
          <div class="filter-row">
            <el-select v-model="calendarId" placeholder="选择日历" clearable class="filter-select">
              <el-option v-for="item in store.calendars" :key="item.id" :label="item.name" :value="item.id">
                <i class="option-dot" :style="{ background: item.color }" />{{ item.name }}
              </el-option>
            </el-select>
            <el-select v-model="tagId" placeholder="选择标签" clearable class="filter-select">
              <el-option v-for="item in store.tags" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </div>
          <el-date-picker
            v-model="range"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-range"
          />
          <el-button type="primary" :icon="Search" @click="search" class="search-btn">查询</el-button>
        </div>

        <!-- 桌面端表格视图 -->
        <div class="desktop-table">
          <el-table v-if="events.length" :data="events" stripe empty-text="暂无搜索结果">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <div class="table-title-cell">
                  <i class="table-dot" :style="{ background: row.calendar_color || 'var(--calendar-primary)' }" />
                  <span>{{ row.title }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="calendar_name" label="日历" min-width="140">
              <template #default="{ row }">
                <span class="table-calendar">{{ row.calendar_name || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="tag_name" label="标签" width="100">
              <template #default="{ row }">
                <span v-if="row.tag_name" class="table-tag">{{ row.tag_name }}</span>
                <span v-else class="table-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="地点" min-width="140">
              <template #default="{ row }">
                <span class="table-muted">{{ row.location || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" min-width="170">
              <template #default="{ row }">{{ format(row.start_at) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="170">
              <template #default="{ row }">{{ format(row.end_at) }}</template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-state">
            <el-icon class="empty-icon"><Search /></el-icon>
            <span>输入条件开始搜索日程</span>
          </div>
        </div>

        <!-- 移动端卡片视图 -->
        <div class="mobile-cards">
          <div v-if="events.length === 0" class="mobile-empty">
            <span>暂无搜索结果</span>
          </div>
          <div v-for="event in events" :key="event.id" class="event-card">
            <div class="card-header">
              <i class="card-dot" :style="{ background: event.calendar_color || 'var(--calendar-primary)' }" />
              <span class="card-title">{{ event.title }}</span>
            </div>
            <div class="card-details">
              <div v-if="event.calendar_name" class="detail-line">
                <span class="detail-label">日历</span>
                <span class="detail-value">{{ event.calendar_name }}</span>
              </div>
              <div class="detail-line">
                <span class="detail-label">时间</span>
                <span class="detail-value">{{ format(event.start_at) }} - {{ format(event.end_at) }}</span>
              </div>
              <div v-if="event.location" class="detail-line">
                <span class="detail-label">地点</span>
                <span class="detail-value">{{ event.location }}</span>
              </div>
              <div v-if="event.tag_name" class="detail-line">
                <span class="detail-label">标签</span>
                <span class="table-tag">{{ event.tag_name }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import type { EventItem } from '../../api/types'
import { useAppStore } from '../../stores/app'

const store = useAppStore()
const keyword = ref('')
const calendarId = ref<number>()
const tagId = ref<number>()
const range = ref<[Date, Date]>()
const events = ref<EventItem[]>([])

onMounted(async () => {
  await store.loadBase()
  await search()
})

async function search() {
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  if (calendarId.value) params.set('calendarId', String(calendarId.value))
  if (tagId.value) params.set('tag_id', String(tagId.value))
  if (range.value) {
    params.set('start', range.value[0].toISOString())
    params.set('end', range.value[1].toISOString())
  }
  try {
    events.value = await api.get<EventItem[]>(`/events?${params.toString()}`)
  } catch {
    events.value = []
  }
}

function format(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}
</script>

<style scoped>
.page-shell {
  min-height: 100vh;
  background: var(--calendar-bg);
}

.topbar {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 20px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
  box-shadow: var(--calendar-shadow-sm);
}

.back-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--calendar-text);
  background: var(--calendar-bg);
  border: 1px solid var(--calendar-border);
  transition: all 0.15s ease;
}

.back-btn:hover {
  border-color: var(--calendar-primary);
  color: var(--calendar-primary);
  background: var(--calendar-primary-bg);
}

.brand {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.content {
  padding: 20px;
}

.search-panel {
  border-radius: var(--calendar-radius);
  overflow: hidden;
  box-shadow: var(--calendar-shadow-sm);
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border-bottom: 1px solid var(--calendar-border-soft);
  background: var(--calendar-surface);
}

.search-input {
  font-size: 15px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.date-range {
  width: 100%;
}

.search-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
}

/* Table */
.desktop-table {
  background: var(--calendar-surface);
}

.table-title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.table-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.table-calendar {
  font-weight: 600;
  color: var(--calendar-text);
}

.table-tag {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--calendar-primary-bg);
  color: var(--calendar-primary);
  font-size: 12px;
  font-weight: 700;
}

.table-muted {
  color: var(--calendar-muted);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 200px;
  color: var(--calendar-muted);
}

.empty-icon {
  font-size: 40px;
  opacity: 0.4;
}

/* Mobile Cards */
.mobile-cards {
  display: none;
  padding: 16px;
  gap: 12px;
  background: var(--calendar-bg);
}

.mobile-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: var(--calendar-muted);
  font-weight: 600;
}

.event-card {
  border-radius: var(--calendar-control-radius);
  background: var(--calendar-surface);
  border: 1px solid var(--calendar-border);
  overflow: hidden;
  box-shadow: var(--calendar-shadow-sm);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--calendar-border-soft);
}

.card-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-details {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-line {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.detail-label {
  width: 52px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--calendar-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--calendar-soft-text);
  word-break: break-all;
}

@media (max-width: 720px) {
  .topbar {
    height: 56px;
    padding: 0 16px;
  }

  .content {
    padding: 12px;
  }

  .toolbar {
    padding: 16px;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .desktop-table {
    display: none;
  }

  .mobile-cards {
    display: flex;
    flex-direction: column;
  }
}
</style>
