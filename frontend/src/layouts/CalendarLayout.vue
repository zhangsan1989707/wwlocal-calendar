<template>
  <div class="calendar-shell">
    <aside class="calendar-sidebar">
      <div class="app-title">日程</div>
      <RouterLink class="search-entry" to="/calendar/search">
        <el-icon><Search /></el-icon>
        <span>搜索</span>
      </RouterLink>

      <section class="mini-month">
        <div class="section-title">{{ monthLabel }}</div>
        <div class="mini-week" v-for="week in miniWeeks" :key="week.join('-')">
          <span v-for="day in week" :key="day" :class="{ active: day === 5 }">{{ day }}</span>
        </div>
      </section>

      <section class="calendar-list">
        <div class="section-title">我的日历</div>
        <label v-for="item in store.calendars" :key="item.id" class="calendar-check">
          <input
            type="checkbox"
            :checked="store.selectedCalendarIds.includes(item.id)"
            @change="store.toggleCalendar(item.id)"
          />
          <i :style="{ background: item.color }"></i>
          <span>{{ item.name }}</span>
        </label>
      </section>
    </aside>
    <main class="calendar-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { useCalendarStore } from '@/stores/calendar';
import { formatMonth } from '@/utils/date';

const store = useCalendarStore();
const monthLabel = computed(() => formatMonth(store.currentDate));
const miniWeeks = [
  ['日', '一', '二', '三', '四', '五', '六'],
  [31, 1, 2, 3, 4, 5, 6],
  [7, 8, 9, 10, 11, 12, 13],
  [14, 15, 16, 17, 18, 19, 20],
  [21, 22, 23, 24, 25, 26, 27],
  [28, 29, 30, 1, 2, 3, 4]
];
</script>
