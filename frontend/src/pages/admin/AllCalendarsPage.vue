<template>
  <section class="page-panel">
    <div class="page-heading inline-heading">
      <div>
        <h1>全员日历</h1>
        <p>创建全员日历，统一安排企业日程与共享范围。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openForm()">添加全员日历</el-button>
    </div>

    <el-table :data="calendars" border>
      <el-table-column label="颜色" width="80">
        <template #default="{ row }">
          <span class="color-dot" :style="{ background: row.color }"></span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="240" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="owner_user_id" label="所有者" width="120" />
      <el-table-column label="可见性" width="100">
        <template #default="{ row }">
          <el-tag :type="row.visible ? 'success' : 'info'">
            {{ row.visible ? '可见' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <AllCalendarForm v-model="formVisible" :calendar="activeCalendar" @save="handleSave" />
  </section>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import AllCalendarForm from '@/components/AllCalendarForm.vue';
import { useAppStore } from '@/stores/app';
import type { CalendarItem } from '@/api/types';

const store = useAppStore();
const formVisible = ref(false);
const activeCalendar = ref<CalendarItem | undefined>();

const calendars = computed(() => store.calendars.filter(c => c.type === 'ALL_MEMBER' || c.type === 'SHARED'));

function typeLabel(type: string) {
  const map: Record<string, string> = {
    PERSONAL: '个人',
    SHARED: '共享',
    PUBLIC: '公共',
    ALL_MEMBER: '全员'
  };
  return map[type] || type;
}

function openForm(calendar?: CalendarItem) {
  activeCalendar.value = calendar;
  formVisible.value = true;
}

async function handleSave(calendar: CalendarItem) {
  console.log('Save calendar:', calendar);
  // TODO: Call API to save calendar
  await store.loadBase();
  formVisible.value = false;
}
</script>
