<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="admin-title">管理端</div>
      <router-link v-for="item in menus" :key="item.path" :to="item.path" class="menu-link">
        {{ item.label }}
      </router-link>
    </aside>
    <section class="admin-main">
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
const menus = [
  { path: '/admin', label: '管理首页' },
  { path: '/admin/users', label: '系统用户管理' },
  { path: '/admin/departments', label: '部门管理' },
  { path: '/admin/calendars', label: '日历管理' },
  { path: '/admin/all-calendars', label: '全员日历管理' },
  { path: '/admin/events', label: '日程管理' },
  { path: '/admin/attachments', label: '附件管理' },
  { path: '/admin/exports', label: '导出任务管理' },
  { path: '/admin/audit-logs', label: '审计日志' },
  { path: '/admin/backup', label: '备份恢复' },
  { path: '/admin/settings', label: '系统配置' }
]
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  background: #f6f8fb;
}

.admin-side {
  padding: 18px 12px;
  background: #172033;
  color: #fff;
}

.admin-title {
  padding: 0 12px 18px;
  font-size: 18px;
  font-weight: 700;
}

.menu-link {
  display: block;
  padding: 10px 12px;
  border-radius: 6px;
  color: #dbe4f2;
}

.menu-link.router-link-active {
  background: #2563eb;
  color: #fff;
}

.admin-main {
  min-width: 0;
  padding: 18px;
}

@media (max-width: 820px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-side {
    display: flex;
    overflow-x: auto;
    gap: 8px;
  }

  .admin-title {
    white-space: nowrap;
    padding: 10px 12px;
  }

  .menu-link {
    white-space: nowrap;
  }
}
</style>
