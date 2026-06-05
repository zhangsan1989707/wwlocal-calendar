<template>
  <el-container class="admin-shell">
    <el-aside width="232px" class="admin-aside">
      <div class="admin-title">协同日历管理</div>
      <el-menu router :default-active="$route.path" class="admin-menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div>
          <strong>管理工作台</strong>
          <span>企业日历、日程与权限统一维护</span>
        </div>
        <RouterLink to="/calendar">进入日历</RouterLink>
      </el-header>
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  Calendar,
  Clock,
  Document,
  FolderOpened,
  Operation,
  Setting,
  Tickets,
  User,
  OfficeBuilding,
  DataAnalysis
} from '@element-plus/icons-vue';

const navItems = [
  { path: '/admin', label: '概览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/departments', label: '部门管理', icon: OfficeBuilding },
  { path: '/admin/calendars', label: '个人日历', icon: Calendar },
  { path: '/admin/all-calendars', label: '全员日历', icon: Tickets },
  { path: '/admin/events', label: '日程管理', icon: Clock },
  { path: '/admin/attachments', label: '附件管理', icon: FolderOpened },
  { path: '/admin/exports', label: '导出管理', icon: Document },
  { path: '/admin/audit-logs', label: '审计日志', icon: Operation },
  { path: '/admin/backup', label: '备份恢复', icon: FolderOpened },
  { path: '/admin/settings', label: '系统设置', icon: Setting }
];
</script>
