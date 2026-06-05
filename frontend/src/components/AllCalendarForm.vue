<template>
  <el-dialog v-model="visible" :title="form.id ? '编辑全员日历' : '添加全员日历'" width="640px">
    <el-form :model="form" label-width="120px">
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type" class="full-width">
          <el-option label="个人日历" value="PERSONAL" />
          <el-option label="共享日历" value="SHARED" />
          <el-option label="公共日历" value="PUBLIC" />
          <el-option label="全员日历" value="ALL_MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="颜色">
        <el-color-picker v-model="form.color" />
      </el-form-item>
      <el-form-item label="可见性">
        <el-switch v-model="form.visible" active-text="可见" inactive-text="隐藏" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio-button label="ACTIVE">启用</el-radio-button>
          <el-radio-button label="INACTIVE">停用</el-radio-button>
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
import type { CalendarItem } from '@/api/types';

const props = defineProps<{
  modelValue: boolean;
  calendar?: CalendarItem;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [calendar: CalendarItem];
}>();

const visible = ref(props.modelValue);
const form = reactive({
  id: '',
  name: '',
  description: '',
  type: 'ALL_MEMBER' as 'PERSONAL' | 'SHARED' | 'PUBLIC' | 'ALL_MEMBER',
  color: '#2f7cf6',
  visible: true,
  status: 'ACTIVE',
  owner_user_id: ''
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
  form.type = calendar?.type || 'ALL_MEMBER';
  form.color = calendar?.color || '#2f7cf6';
  form.visible = calendar?.visible ?? true;
  form.status = calendar?.status || 'ACTIVE';
  form.owner_user_id = calendar?.owner_user_id || '';
}

function submit() {
  emit('save', {
    id: form.id || `cal-${Date.now()}`,
    name: form.name || '未命名日历',
    description: form.description,
    type: form.type,
    color: form.color,
    visible: form.visible,
    status: form.status,
    owner_user_id: form.owner_user_id
  });
  visible.value = false;
}
</script>
