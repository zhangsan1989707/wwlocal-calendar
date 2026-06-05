<template>
  <el-dialog v-model="visible" :title="form.id ? '编辑全员日历' : '添加全员日历'" width="640px">
    <el-form :model="form" label-width="120px">
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </el-form-item>
      <el-form-item label="自动订阅范围">
        <el-select v-model="form.autoSubscribeScope" multiple filterable allow-create class="full-width">
          <el-option v-for="scope in scopes" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </el-form-item>
      <el-form-item label="共享成员">
        <el-select v-model="form.sharedMembers" multiple filterable allow-create class="full-width">
          <el-option v-for="member in members" :key="member" :label="member" :value="member" />
        </el-select>
      </el-form-item>
      <el-form-item label="颜色">
        <el-color-picker v-model="form.color" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio-button label="enabled">启用</el-radio-button>
          <el-radio-button label="disabled">停用</el-radio-button>
        </el-radio-group>
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
import type { CalendarItem, CalendarStatus } from '@/types/calendar';

const props = defineProps<{
  modelValue: boolean;
  calendar?: CalendarItem;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [calendar: CalendarItem];
}>();

const visible = ref(props.modelValue);
const members = ['李宇航', '周明', '陈晓', '王宁'];
const scopes = ['全公司', '销售中心', '运营部', '产品部', '成都市'];
const form = reactive({
  id: '',
  name: '',
  description: '',
  autoSubscribeScope: [] as string[],
  sharedMembers: [] as string[],
  color: '#2f7cf6',
  status: 'enabled' as CalendarStatus,
  owner: '系统管理员'
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
  const calendar = props.calendar;
  form.id = calendar?.id || '';
  form.name = calendar?.name || '';
  form.description = calendar?.description || '';
  form.autoSubscribeScope = [...(calendar?.autoSubscribeScope || [])];
  form.sharedMembers = [...(calendar?.sharedMembers || [])];
  form.color = calendar?.color || '#2f7cf6';
  form.status = calendar?.status || 'enabled';
  form.owner = calendar?.owner || '系统管理员';
}

function submit() {
  emit('save', {
    id: form.id || `cal-${Date.now()}`,
    name: form.name || '未命名日历',
    description: form.description,
    autoSubscribeScope: [...form.autoSubscribeScope],
    sharedMembers: [...form.sharedMembers],
    color: form.color,
    status: form.status,
    owner: form.owner
  });
  visible.value = false;
}
</script>
