<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <span>Overview</span>
        <h1>管理首页</h1>
        <p>查看组织日历、成员与系统运行概况。</p>
      </div>
    </div>
    <div class="stats">
      <div v-for="item in statItems" :key="item.label" class="stat panel">
        <span class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <div class="admin-home-grid">
      <section class="panel block status-panel">
        <div class="block-head">
          <span>Service</span>
          <h2>系统状态</h2>
        </div>
        <div class="status-list">
          <div v-for="item in statusItems" :key="item.label" class="status-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>
      <section class="panel block logs-panel">
        <div class="block-head">
          <span>Recent Activity</span>
          <h2>近期操作</h2>
        </div>
        <el-table :data="overview.recentLogs || []" stripe empty-text="暂无近期操作">
          <el-table-column prop="created_at" label="操作时间" min-width="180" />
          <el-table-column prop="module" label="模块" width="140" />
          <el-table-column prop="action" label="动作" width="140" />
          <el-table-column prop="object_type" label="对象" width="140" />
          <el-table-column prop="change_summary" label="摘要" min-width="220" />
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Calendar, Clock, DataBoard, Files, OfficeBuilding, Tickets, User } from '@element-plus/icons-vue';
import { api } from '../../api/http';

const overview = ref<Record<string, any>>({});
const statItems = computed(() => [
  { label: '系统用户', value: overview.value.users ?? 0, icon: User },
  { label: '部门', value: overview.value.departments ?? 0, icon: OfficeBuilding },
  { label: '日历', value: overview.value.calendars ?? 0, icon: Calendar },
  { label: '全员日历', value: overview.value.allCalendars ?? 0, icon: Tickets },
  { label: '日程', value: overview.value.events ?? 0, icon: Clock },
  { label: '附件', value: overview.value.attachments ?? 0, icon: Files },
  { label: '待办', value: overview.value.todos ?? 0, icon: DataBoard },
  { label: '导出任务', value: overview.value.exports ?? 0, icon: DataBoard }
]);
const statusItems = computed(() => [
  { label: '后端服务', value: overview.value.serviceStatus ?? 'UNKNOWN' },
  { label: '数据库连接', value: overview.value.databaseStatus ?? 'UNKNOWN' },
  { label: '附件目录', value: overview.value.uploadStatus ?? 'UNKNOWN' }
]);

onMounted(async () => {
  try {
    overview.value = await api.get<Record<string, any>>('/admin/overview');
  } catch {
    overview.value = {};
  }
});
</script>

<style scoped>
.admin-page {
  padding: 32px;
}

h1,
h2 {
  margin: 0;
}

.admin-page-head {
  min-height: 88px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.admin-page-head span,
.block-head span {
  color: var(--calendar-muted);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.admin-page-head h1 {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.admin-page-head p {
  margin: 8px 0 0;
  color: var(--calendar-soft-text);
  font-weight: 500;
  font-size: 15px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat {
  min-height: 132px;
  display: grid;
  gap: 10px;
  padding: 20px;
  border: 1px solid var(--calendar-border);
  border-radius: var(--calendar-radius);
  background: var(--calendar-surface);
  box-shadow: var(--calendar-shadow-sm);
  transition: all 0.2s ease;
}

.stat:hover {
  transform: translateY(-2px);
  box-shadow: var(--calendar-shadow);
}

.stat-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: var(--calendar-primary);
  background: var(--calendar-primary-bg);
}

.stat > span:not(.stat-icon) {
  color: var(--calendar-soft-text);
  font-weight: 600;
  font-size: 14px;
}

.stat strong {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.admin-home-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
}

.block {
  padding: 20px;
  border: 1px solid var(--calendar-border);
  border-radius: var(--calendar-radius);
  background: var(--calendar-surface);
  box-shadow: var(--calendar-shadow-sm);
}

.block-head {
  display: grid;
  gap: 5px;
  margin-bottom: 16px;
}

.block-head h2 {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.status-list {
  display: grid;
  gap: 10px;
}

.status-row {
  min-height: 56px;
  display: grid;
  align-content: center;
  gap: 5px;
  padding: 0 14px;
  border: 1px solid var(--calendar-border-soft);
  border-radius: 10px;
  background: var(--calendar-bg);
  transition: all 0.2s ease;
}

.status-row:hover {
  border-color: var(--calendar-border);
  background: var(--calendar-surface);
}

.status-row span {
  color: var(--calendar-muted);
  font-size: 12px;
  font-weight: 600;
}

.status-row strong {
  color: var(--calendar-success);
  font-size: 15px;
  font-weight: 700;
}

.logs-panel {
  min-width: 0;
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .admin-page {
    padding: 20px 16px;
  }

  .admin-page-head {
    min-height: 0;
    flex-direction: column;
  }

  .admin-home-grid {
    grid-template-columns: 1fr;
  }
}
</style>
