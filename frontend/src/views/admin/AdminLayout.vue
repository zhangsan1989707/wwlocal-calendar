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
} from '@element-plus/icons-vue';

const menus = [
  { path: '/admin', label: '概览', icon: DataBoard },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/departments', label: '部门管理', icon: OfficeBuilding },
  { path: '/admin/calendars', label: '日历管理', icon: Calendar },
  { path: '/admin/all-calendars', label: '全员日历', icon: Tickets },
  { path: '/admin/events', label: '日程管理', icon: Clock },
  { path: '/admin/attachments', label: '附件管理', icon: Files },
  { path: '/admin/exports', label: '导出管理', icon: Document },
  { path: '/admin/audit-logs', label: '审计日志', icon: Operation },
  { path: '/admin/backup', label: '备份恢复', icon: DataBoard },
  { path: '/admin/settings', label: '系统配置', icon: Setting }
];
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
  padding: 20px 16px 24px;
  overflow: auto;
  border-right: 1px solid var(--calendar-border);
  background: var(--calendar-sidebar);
  color: var(--calendar-text);
  box-shadow: var(--calendar-shadow-sm);
}

.admin-title {
  min-height: 52px;
  display: grid;
  align-content: center;
  gap: 4px;
  padding: 0 12px 16px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--calendar-border-soft);
}

.admin-title strong {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.admin-title span {
  color: var(--calendar-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.admin-menu {
  display: grid;
  gap: 2px;
  padding-top: 4px;
}

.menu-link {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-radius: 10px;
  color: var(--calendar-soft-text);
  font-weight: 600;
  font-size: 14px;
  transition: all 0.18s ease;
}

.menu-link:hover {
  color: var(--calendar-text);
  background: var(--calendar-border-soft);
  transform: translateY(-1px);
}

.menu-link .el-icon {
  font-size: 18px;
  opacity: 0.8;
}

.menu-link.router-link-active {
  background: var(--calendar-primary-bg);
  color: var(--calendar-primary);
  font-weight: 600;
}

.menu-link.router-link-active .el-icon {
  opacity: 1;
}

.admin-main {
  min-width: 0;
  padding: 0;
  overflow: auto;
}

.admin-topbar {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 32px;
  border-bottom: 1px solid var(--calendar-border);
  background: var(--calendar-surface);
  backdrop-filter: blur(8px);
}

.admin-topbar div {
  display: grid;
  gap: 3px;
}

.admin-topbar span {
  color: var(--calendar-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.admin-topbar strong {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.admin-calendar-entry {
  height: 38px;
  display: inline-flex;
  align-items: center;
  padding: 0 18px;
  border: 1px solid var(--calendar-border);
  border-radius: var(--calendar-control-radius);
  color: var(--calendar-text);
  background: var(--calendar-surface);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: var(--calendar-shadow-sm);
}

.admin-calendar-entry:hover {
  border-color: var(--calendar-primary);
  color: var(--calendar-primary);
  box-shadow: var(--calendar-shadow);
  transform: translateY(-1px);
}

@media (max-width: 820px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-side {
    display: flex;
    overflow-x: auto;
    gap: 8px;
    padding: 12px 16px;
  }

  .admin-title {
    min-height: 40px;
    white-space: nowrap;
    padding: 0 16px 0 0;
    border-bottom: 0;
    border-right: 1px solid var(--calendar-border);
    margin-bottom: 0;
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
    min-height: 60px;
    padding: 12px 20px;
  }
}
</style>
