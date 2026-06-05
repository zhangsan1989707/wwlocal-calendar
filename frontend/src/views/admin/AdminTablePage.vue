<template>
  <div>
    <div class="toolbar">
      <h1>{{ title }}</h1>
      <el-button type="primary" @click="openCreate" v-if="canCreate">新增</el-button>
      <el-button @click="createBackup" v-if="resource === 'backup'">发起备份</el-button>
      <el-button @click="exportEvents" v-if="resource === 'events'">导出日程</el-button>
    </div>
    <section class="panel table-panel">
      <el-table :data="rows" stripe>
        <el-table-column v-for="column in columns" :key="column.prop" :prop="column.prop" :label="column.label" min-width="140" />
        <el-table-column label="操作" width="160" fixed="right" v-if="canCreate">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing?.id ? '编辑' : '新增'" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item v-for="field in fields" :key="field.key" :label="field.label" :required="field.required">
          <el-input v-if="field.type === 'text'" v-model="form[field.key]" />
          <el-input v-else-if="field.type === 'textarea'" v-model="form[field.key]" type="textarea" />
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.key]" :min="0" />
          <el-select v-else-if="field.type === 'select'" v-model="form[field.key]">
            <el-option v-for="item in field.options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-color-picker v-else-if="field.type === 'color'" v-model="form[field.key]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'

const props = defineProps<{ resource: string; title: string }>()
const rows = ref<Record<string, any>[]>([])
const dialogVisible = ref(false)
const editing = ref<Record<string, any> | null>(null)
const form = reactive<Record<string, any>>({})

const canCreate = computed(() => ['users', 'departments', 'calendars'].includes(props.resource))
const columns = computed(() => columnMap[props.resource] || defaultColumns)
const fields = computed(() => fieldMap[props.resource] || [])

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' }
]
const calendarTypeOptions = [
  { label: '个人日历', value: 'PERSONAL' },
  { label: '共享日历', value: 'SHARED' },
  { label: '公共日历', value: 'PUBLIC' }
]
const visibilityOptions = [
  { label: '个人', value: 'PRIVATE' },
  { label: '共享', value: 'SHARED' },
  { label: '公开', value: 'PUBLIC' }
]

const defaultColumns = [
  { prop: 'id', label: '编号' },
  { prop: 'status', label: '状态' },
  { prop: 'created_at', label: '创建时间' }
]

const columnMap: Record<string, Array<{ prop: string; label: string }>> = {
  users: [
    { prop: 'id', label: '编号' },
    { prop: 'name', label: '姓名' },
    { prop: 'department_id', label: '部门编号' },
    { prop: 'email', label: '邮箱' },
    { prop: 'phone', label: '手机号' },
    { prop: 'status', label: '状态' }
  ],
  departments: [
    { prop: 'id', label: '编号' },
    { prop: 'name', label: '部门名称' },
    { prop: 'sort_order', label: '排序' },
    { prop: 'status', label: '状态' }
  ],
  calendars: [
    { prop: 'id', label: '编号' },
    { prop: 'name', label: '日历名称' },
    { prop: 'type', label: '类型' },
    { prop: 'color', label: '颜色' },
    { prop: 'status', label: '状态' }
  ],
  events: [
    { prop: 'id', label: '编号' },
    { prop: 'title', label: '标题' },
    { prop: 'calendar_name', label: '日历' },
    { prop: 'organizer_name', label: '发起人' },
    { prop: 'start_time', label: '开始时间' },
    { prop: 'end_time', label: '结束时间' }
  ],
  attachments: [
    { prop: 'id', label: '编号' },
    { prop: 'file_name', label: '文件名' },
    { prop: 'file_size', label: '大小' },
    { prop: 'event_title', label: '关联日程' },
    { prop: 'uploader_name', label: '上传人' }
  ],
  exports: [
    { prop: 'id', label: '编号' },
    { prop: 'task_name', label: '任务名称' },
    { prop: 'export_scope', label: '导出范围' },
    { prop: 'status', label: '状态' },
    { prop: 'file_path', label: '文件路径' }
  ],
  'audit-logs': [
    { prop: 'id', label: '编号' },
    { prop: 'module', label: '模块' },
    { prop: 'action', label: '动作' },
    { prop: 'object_type', label: '对象类型' },
    { prop: 'change_summary', label: '摘要' },
    { prop: 'created_at', label: '操作时间' }
  ],
  backup: [
    { prop: 'id', label: '编号' },
    { prop: 'backup_type', label: '备份类型' },
    { prop: 'status', label: '状态' },
    { prop: 'file_path', label: '文件路径' },
    { prop: 'restore_result', label: '恢复结果' },
    { prop: 'conclusion', label: '结论' }
  ],
  settings: [
    { prop: 'config_key', label: '配置项' },
    { prop: 'config_value', label: '配置值' },
    { prop: 'description', label: '说明' },
    { prop: 'updated_at', label: '更新时间' }
  ]
}

const fieldMap: Record<string, Array<any>> = {
  users: [
    { key: 'name', label: '姓名', type: 'text', required: true },
    { key: 'department_id', label: '部门编号', type: 'number' },
    { key: 'email', label: '邮箱', type: 'text' },
    { key: 'phone', label: '手机号', type: 'text' },
    { key: 'avatar_color', label: '头像颜色', type: 'color' },
    { key: 'status', label: '状态', type: 'select', options: statusOptions }
  ],
  departments: [
    { key: 'name', label: '部门名称', type: 'text', required: true },
    { key: 'sort_order', label: '排序', type: 'number' },
    { key: 'status', label: '状态', type: 'select', options: statusOptions }
  ],
  calendars: [
    { key: 'name', label: '日历名称', type: 'text', required: true },
    { key: 'description', label: '日历描述', type: 'textarea' },
    { key: 'type', label: '类型', type: 'select', options: calendarTypeOptions },
    { key: 'color', label: '颜色', type: 'color' },
    { key: 'visibility', label: '展示范围', type: 'select', options: visibilityOptions },
    { key: 'status', label: '状态', type: 'select', options: statusOptions }
  ]
}

onMounted(load)
watch(() => props.resource, load)

async function load() {
  const path = props.resource === 'backup' ? '/backup-records' : `/${props.resource}`
  try {
    rows.value = await api.get<Record<string, any>[]>(path)
  } catch {
    rows.value = []
  }
}

function openCreate() {
  editing.value = null
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, { status: 'ACTIVE', color: '#2563eb', avatar_color: '#2563eb', type: 'PUBLIC', visibility: 'PUBLIC' })
  dialogVisible.value = true
}

function openEdit(row: Record<string, any>) {
  editing.value = row
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  if (editing.value?.id) {
    await api.put(`/${props.resource}/${editing.value.id}`, form)
  } else {
    await api.post(`/${props.resource}`, form)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await load()
}

async function exportEvents() {
  await api.post('/export/events', {})
  ElMessage.success('导出任务已创建')
}

async function createBackup() {
  await api.post('/backup', {})
  ElMessage.success('备份记录已生成')
  await load()
}
</script>

<style scoped>
h1 {
  margin: 0;
}

.table-panel {
  padding: 12px;
}
</style>
