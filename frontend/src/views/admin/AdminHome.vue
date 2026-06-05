<template>
  <div>
    <div class="toolbar">
      <h1>管理首页</h1>
      <router-link to="/calendar"><el-button>进入用户端</el-button></router-link>
    </div>
    <div class="stats">
      <div v-for="item in statItems" :key="item.label" class="stat panel">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <section class="panel block">
      <h2>系统状态</h2>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="后端服务">{{ overview.serviceStatus }}</el-descriptions-item>
        <el-descriptions-item label="数据库连接">{{ overview.databaseStatus }}</el-descriptions-item>
        <el-descriptions-item label="附件目录">{{ overview.uploadStatus }}</el-descriptions-item>
      </el-descriptions>
    </section>
    <section class="panel block">
      <h2>近期操作</h2>
      <el-table :data="overview.recentLogs || []" stripe>
        <el-table-column prop="created_at" label="操作时间" min-width="180" />
        <el-table-column prop="module" label="模块" width="140" />
        <el-table-column prop="action" label="动作" width="140" />
        <el-table-column prop="object_type" label="对象" width="140" />
        <el-table-column prop="change_summary" label="摘要" min-width="220" />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api/http'

const overview = ref<Record<string, any>>({})
const statItems = computed(() => [
  { label: '系统用户', value: overview.value.users ?? 0 },
  { label: '部门', value: overview.value.departments ?? 0 },
  { label: '日历', value: overview.value.calendars ?? 0 },
  { label: '全员日历', value: overview.value.allCalendars ?? 0 },
  { label: '日程', value: overview.value.events ?? 0 },
  { label: '附件', value: overview.value.attachments ?? 0 },
  { label: '待办', value: overview.value.todos ?? 0 },
  { label: '导出任务', value: overview.value.exports ?? 0 }
])

onMounted(async () => {
  try {
    overview.value = await api.get<Record<string, any>>('/admin/overview')
  } catch {
    overview.value = {}
  }
})
</script>

<style scoped>
h1,
h2 {
  margin: 0;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.stat {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.stat strong {
  font-size: 28px;
}

.block {
  padding: 16px;
  margin-top: 14px;
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
