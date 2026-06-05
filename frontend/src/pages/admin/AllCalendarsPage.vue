<template>
  <section class="page-panel">
    <div class="page-heading inline-heading">
      <div>
        <h1>全员日历</h1>
        <p>创建全员日历，统一安排企业日程与共享范围。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openForm()">添加全员日历</el-button>
    </div>

    <el-table :data="store.calendars" border>
      <el-table-column label="颜色" width="80">
        <template #default="{ row }">
          <span class="color-dot" :style="{ background: row.color }"></span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="240" />
      <el-table-column label="自动订阅范围" min-width="180">
        <template #default="{ row }">{{ row.autoSubscribeScope.join('、') || '未设置' }}</template>
      </el-table-column>
      <el-table-column label="共享成员" min-width="180">
        <template #default="{ row }">{{ row.sharedMembers.join('、') || '未设置' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'info'">
            {{ row.status === 'enabled' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <AllCalendarForm v-model="formVisible" :calendar="activeCalendar" @save="store.saveCalendar" />
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import AllCalendarForm from '@/components/AllCalendarForm.vue';
import { useCalendarStore } from '@/stores/calendar';
import type { CalendarItem } from '@/types/calendar';

const store = useCalendarStore();
const formVisible = ref(false);
const activeCalendar = ref<CalendarItem | undefined>();

function openForm(calendar?: CalendarItem) {
  activeCalendar.value = calendar;
  formVisible.value = true;
}
</script>
