<template>
  <section class="page-panel">
    <div class="page-heading inline-heading">
      <div>
        <h1>{{ config.title }}</h1>
        <p>{{ config.description }}</p>
      </div>
      <el-button type="primary" :icon="Plus">新增</el-button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="name" :label="config.nameLabel" min-width="180" />
      <el-table-column prop="owner" label="负责人" width="150" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="160">
        <template #default>
          <el-button link type="primary">编辑</el-button>
          <el-button link type="primary">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import type { TableRecord } from '@/types/calendar';

const props = defineProps<{ resource: keyof typeof pageConfigs }>();

const pageConfigs = {
  users: { title: '用户管理', description: '维护企业成员资料与账号状态。', nameLabel: '用户姓名' },
  departments: { title: '部门管理', description: '维护组织架构与部门负责人。', nameLabel: '部门名称' },
  calendars: { title: '个人日历', description: '管理成员个人日历的创建、共享与状态。', nameLabel: '日历名称' },
  events: { title: '日程管理', description: '查看与维护企业范围内的日程记录。', nameLabel: '日程标题' },
  attachments: { title: '附件管理', description: '管理日程关联附件与访问记录。', nameLabel: '附件名称' },
  exports: { title: '导出管理', description: '维护导出任务、文件状态与下载权限。', nameLabel: '导出任务' },
  auditLogs: { title: '审计日志', description: '查看关键操作记录与访问轨迹。', nameLabel: '操作事项' },
  backup: { title: '备份恢复', description: '维护备份策略与恢复记录。', nameLabel: '备份任务' },
  settings: { title: '系统设置', description: '配置系统参数与基础运行规则。', nameLabel: '配置项' }
};

const config = computed(() => pageConfigs[props.resource]);
const rows = computed<TableRecord[]>(() => [
  { id: '1', name: `${config.value.title}一`, owner: '李宇航', status: '启用', updatedAt: '2026-06-05 09:30' },
  { id: '2', name: `${config.value.title}二`, owner: '陈晓', status: '启用', updatedAt: '2026-06-04 16:20' },
  { id: '3', name: `${config.value.title}三`, owner: '周明', status: '停用', updatedAt: '2026-06-03 11:15' }
]);
</script>
