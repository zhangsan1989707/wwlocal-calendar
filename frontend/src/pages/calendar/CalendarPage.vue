<template>
  <section class="calendar-page">
    <header class="calendar-toolbar">
      <el-button type="primary" :icon="CalendarIcon" @click="openDialog(new Date(store.currentDate))">创建日程</el-button>
      <div class="period-switch">
        <el-button :icon="ArrowLeft" @click="move(-1)" />
        <strong>{{ periodLabel }}</strong>
        <el-button :icon="ArrowRight" @click="move(1)" />
      </div>
      <el-segmented v-model="store.viewMode" :options="viewOptions" />
    </header>

    <CalendarGrid :date="store.currentDate" :mode="store.viewMode" :events="store.visibleEvents" @create="openDialog" />
    <EventDialog v-model="dialogVisible" :date="selectedDate" :calendars="store.calendars" @save="store.saveEvent" />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ArrowLeft, ArrowRight, Calendar as CalendarIcon } from '@element-plus/icons-vue';
import CalendarGrid from '@/components/CalendarGrid.vue';
import EventDialog from '@/components/EventDialog.vue';
import { useCalendarStore } from '@/stores/calendar';
import { addDays, addMonths, formatMonth } from '@/utils/date';

const store = useCalendarStore();
const dialogVisible = ref(false);
const selectedDate = ref(new Date(store.currentDate));
const viewOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '列表', value: 'list' }
];
const periodLabel = computed(() => formatMonth(store.currentDate));

function move(direction: number) {
  store.currentDate =
    store.viewMode === 'month'
      ? addMonths(store.currentDate, direction)
      : addDays(store.currentDate, direction * (store.viewMode === 'week' ? 7 : 1));
}

function openDialog(date: Date) {
  selectedDate.value = date;
  dialogVisible.value = true;
}
</script>
