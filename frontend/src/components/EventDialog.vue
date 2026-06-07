<template>
  <el-dialog v-model="visible" title="日程" width="560px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="日历">
        <el-select v-model="form.calendarId" class="full-width">
          <el-option v-for="item in calendars" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker v-model="form.start" type="datetime" value-format="YYYY-MM-DDTHH:mm" placeholder="选择开始时间" class="full-width" />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker v-model="form.end" type="datetime" value-format="YYYY-MM-DDTHH:mm" placeholder="选择结束时间" class="full-width" />
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.location" placeholder="请输入地点" />
      </el-form-item>
      <el-form-item label="参与人">
        <el-select v-model="form.attendees" multiple filterable allow-create class="full-width">
          <el-option v-for="name in members" :key="name" :label="name" :value="name" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { CalendarEvent, CalendarItem } from '@/types/calendar';
import { formatDate } from '@/utils/date';

const props = defineProps<{
  modelValue: boolean;
  date: Date;
  calendars: CalendarItem[];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [event: CalendarEvent];
}>();

const visible = ref(props.modelValue);
const members = ['李宇航', '周明', '陈晓', '王宁'];
const form = reactive({
  title: '',
  calendarId: '',
  start: '',
  end: '',
  location: '',
  attendees: [] as string[],
  description: ''
});

watch(
  () => props.modelValue,
  (value) => {
    visible.value = value;
    if (value) resetForm();
  }
);

watch(visible, (value) => emit('update:modelValue', value));

function resetForm() {
  const date = formatDate(props.date);
  form.title = '';
  form.calendarId = props.calendars[0]?.id || '';
  form.start = `${date}T09:00`;
  form.end = `${date}T10:00`;
  form.location = '';
  form.attendees = [];
  form.description = '';
}

function submit() {
  const calendar = props.calendars.find((item) => item.id === form.calendarId);
  emit('save', {
    id: `evt-${Date.now()}`,
    calendarId: form.calendarId,
    title: form.title || '未命名日程',
    location: form.location,
    start: form.start,
    end: form.end,
    attendees: [...form.attendees],
    description: form.description,
    color: calendar?.color || '#2f7cf6'
  });
  visible.value = false;
}
</script>
