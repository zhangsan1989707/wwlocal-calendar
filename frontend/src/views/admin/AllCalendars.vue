<template>
  <div>
    <div class="toolbar">
      <h1>全员日历管理</h1>
      <el-button type="primary" @click="openCreate">添加全员日历</el-button>
    </div>
    <section class="panel table-panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="name" label="日历名称" min-width="160" />
        <el-table-column prop="description" label="日历描述" min-width="220" />
        <el-table-column prop="color" label="颜色" width="100">
          <template #default="{ row }"><span class="color" :style="{ background: row.color }" /></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="created_at" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="disable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑全员日历' : '添加全员日历'" width="680px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="日历名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="日历描述">
          <el-input v-model="form.description" type="textarea" maxlength="500" />
        </el-form-item>
        <el-form-item label="自动订阅范围" required>
          <el-select v-model="scopeType">
            <el-option label="全公司" value="ALL_COMPANY" />
            <el-option label="指定部门" value="DEPARTMENT" />
            <el-option label="多部门" value="DEPARTMENTS" />
            <el-option label="指定成员" value="MEMBERS" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围部门" v-if="scopeType === 'DEPARTMENT' || scopeType === 'DEPARTMENTS'">
          <el-select v-model="scopeDepartmentIds" multiple>
            <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围成员" v-if="scopeType === 'MEMBERS'">
          <el-select v-model="scopeUserIds" multiple>
            <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="共享成员">
          <el-select v-model="sharedMemberIds" multiple filterable>
            <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日历颜色" required>
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-select v-model="form.status">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/http'
import type { CalendarItem, Department, User } from '../../api/types'

const rows = ref<CalendarItem[]>([])
const users = ref<User[]>([])
const departments = ref<Department[]>([])
const dialogVisible = ref(false)
const form = reactive<Record<string, any>>({})
const scopeType = ref('ALL_COMPANY')
const scopeDepartmentIds = ref<number[]>([])
const scopeUserIds = ref<number[]>([])
const sharedMemberIds = ref<number[]>([])

onMounted(load)

async function load() {
  try {
    const [calendarRows, userRows, departmentRows] = await Promise.all([
      api.get<CalendarItem[]>('/all-calendars'),
      api.get<User[]>('/users'),
      api.get<Department[]>('/departments')
    ])
    rows.value = calendarRows
    users.value = userRows
    departments.value = departmentRows
  } catch {
    rows.value = []
    users.value = []
    departments.value = []
  }
}

function openCreate() {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, { name: '', description: '', color: '#f59e0b', status: 'ACTIVE' })
  scopeType.value = 'ALL_COMPANY'
  scopeDepartmentIds.value = []
  scopeUserIds.value = []
  sharedMemberIds.value = []
  dialogVisible.value = true
}

function openEdit(row: CalendarItem) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row)
  scopeType.value = 'ALL_COMPANY'
  scopeDepartmentIds.value = []
  scopeUserIds.value = []
  sharedMemberIds.value = []
  dialogVisible.value = true
}

async function save() {
  const scopes = buildScopes()
  const payload = { ...form, scopes, sharedMemberIds: sharedMemberIds.value }
  if (form.id) {
    await api.put(`/all-calendars/${form.id}`, payload)
  } else {
    await api.post('/all-calendars', payload)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await load()
}

async function disable(row: CalendarItem) {
  await ElMessageBox.confirm('确认停用该全员日历？')
  await api.post(`/all-calendars/${row.id}/disable`, {})
  ElMessage.success('已停用')
  await load()
}

function buildScopes() {
  if (scopeType.value === 'ALL_COMPANY') return [{ scopeType: 'ALL_COMPANY' }]
  if (scopeType.value === 'MEMBERS') return scopeUserIds.value.map((userId) => ({ scopeType: 'MEMBER', userId }))
  return scopeDepartmentIds.value.map((departmentId) => ({ scopeType: 'DEPARTMENT', departmentId }))
}
</script>

<style scoped>
h1 {
  margin: 0;
}

.table-panel {
  padding: 12px;
}

.color {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
}
</style>
