<template>
  <div class="page-shell">
    <header class="topbar">
      <div class="brand">日程搜索</div>
      <router-link to="/calendar"><el-button>返回日历</el-button></router-link>
    </header>
    <main class="content">
      <section class="panel search-panel">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="标题、地点、描述" clearable />
          <el-select v-model="calendarId" placeholder="日历" clearable>
            <el-option v-for="item in store.calendars" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="tagId" placeholder="标签" clearable>
            <el-option v-for="item in store.tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker v-model="range" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
        <el-table :data="events" stripe>
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="calendar_name" label="日历" min-width="140" />
          <el-table-column prop="tag_name" label="标签" width="100" />
          <el-table-column prop="location" label="地点" min-width="140" />
          <el-table-column label="开始时间" min-width="180">
            <template #default="{ row }">{{ format(row.start_at) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" min-width="180">
            <template #default="{ row }">{{ format(row.end_at) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

function format(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.search-panel {
  padding: 16px;
}

.toolbar > * {
  max-width: 260px;
}
</style>
