<template>
  <section class="search-page">
    <header class="calendar-toolbar">
      <RouterLink to="/calendar">
        <el-button :icon="ArrowLeft">返回日历</el-button>
      </RouterLink>
      <el-input v-model="keyword" class="search-box" size="large" placeholder="搜索日程、地点或参与人" clearable />
    </header>
    <div class="event-list">
      <button v-for="event in results" :key="event.id" class="event-row">
        <i :style="{ background: event.color }"></i>
        <strong>{{ event.title }}</strong>
        <span>{{ event.start.replace('T', ' ') }}</span>
        <em>{{ event.location }}</em>
      </button>
      <el-empty v-if="results.length === 0" description="暂无匹配日程" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useCalendarStore } from '@/stores/calendar';

const store = useCalendarStore();
const keyword = ref('');
const results = computed(() => {
  const value = keyword.value.trim();
  if (!value) return store.visibleEvents;
  return store.visibleEvents.filter((event) =>
    [event.title, event.location, event.description, ...event.attendees].some((text) => text.includes(value))
  );
});
</script>
