<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="admin-title">
        <strong>日程管理</strong>
        <span>Admin Console</span>
      </div>
      <nav class="admin-menu" aria-label="管理端导航">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="menu-link">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <span>企业协同日历</span>
          <strong>管理工作台</strong>
        </div>
        <router-link class="admin-calendar-entry" to="/calendar">进入用户端</router-link>
      </header>
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
import {
  Calendar,
  Clock,
  DataBoard,
  Document,
  Files,
  OfficeBuilding,
  Operation,
  Setting,
  Tickets,
  User
} from '@element-plus/icons-vue'

const menus = [
  { path: '/admin', label: '管理首页', icon: DataBoard },
  { path: '/admin/users', label: '系统用户管理', icon: User },
  { path: '/admin/departments', label: '部门管理', icon: OfficeBuilding },
  { path: '/admin/calendars', label: '日历管理', icon: Calendar },
  { path: '/admin/all-calendars', label: '全员日历管理', icon: Tickets },
  { path: '/admin/events', label: '日程管理', icon: Clock },
  { path: '/admin/attachments', label: '附件管理', icon: Files },
  { path: '/admin/exports', label: '导出任务管理', icon: Document },
  { path: '/admin/audit-logs', label: '审计日志', icon: Operation },
  { path: '/admin/backup', label: '备份恢复', icon: DataBoard },
  { path: '/admin/settings', label: '系统配置', icon: Setting }
]
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--calendar-sidebar-width) minmax(0, 1fr);
  background: var(--calendar-bg);
}

.admin-side {
  min-width: 0;
  padding: 16px 14px 28px;
  overflow: auto;
  border-right: 1px solid var(--calendar-border);
  background: var(--calendar-sidebar);
  color: var(--calendar-text);
}

.admin-title {
  min-height: 58px;
  display: grid;
  align-content: center;
  gap: 3px;
  padding: 0 12px;
  border-bottom: 1px solid #dfe5ee;
}

.admin-title strong {
  font-size: 20px;
  font-weight: 700;
}

.admin-title span {
  color: var(--calendar-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.admin-menu {
  display: grid;
  gap: 4px;
  padding-top: 14px;
}

.menu-link {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: var(--calendar-control-radius);
  color: var(--calendar-soft-text);
  font-weight: 700;
}

.menu-link:hover {
  color: var(--calendar-text);
  background: #e2e7ef;
}

.menu-link .el-icon {
  font-size: 17px;
}

.menu-link.router-link-active {
  background: var(--calendar-primary);
  color: #fff;
}

.admin-main {
  min-width: 0;
  padding: 0;
  overflow: auto;
}

.admin-topbar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 24px;
  border-bottom: 1px solid var(--calendar-border);
  background: #f1f4f8;
}

.admin-topbar div {
  display: grid;
  gap: 2px;
}

.admin-topbar span {
  color: var(--calendar-muted);
  font-size: 12px;
  font-weight: 700;
}

.admin-topbar strong {
  font-size: 18px;
}

.admin-calendar-entry {
  height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border: 1px solid #cfd6df;
  border-radius: var(--calendar-control-radius);
  color: #1f2937;
  background: #fff;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 820px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-side {
    display: flex;
    overflow-x: auto;
    gap: 8px;
    padding: 10px 12px;
  }

  .admin-title {
    min-height: 40px;
    white-space: nowrap;
    padding: 0 12px;
    border-bottom: 0;
    border-right: 1px solid #dfe5ee;
  }

  .admin-title span {
    display: none;
  }

  .admin-menu {
    display: flex;
    gap: 8px;
    padding-top: 0;
  }

  .menu-link {
    white-space: nowrap;
  }

  .admin-topbar {
    height: auto;
    min-height: 56px;
    padding: 10px 12px;
  }
}
</style>
