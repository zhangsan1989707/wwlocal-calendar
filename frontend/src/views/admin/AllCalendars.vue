<template>
  <div>
    <div class="toolbar all-calendar-toolbar">
      <div>
        <h1>全员日历</h1>
        <p>可创建全员日历，统一安排企业日程。</p>
      </div>
      <el-button type="primary" @click="openCreate">添加全员日历</el-button>
    </div>
    <section class="panel table-panel">
      <el-table :data="tableRows" stripe empty-text="暂无数据">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑全员日历' : '添加全员日历'" width="560px" class="admin-calendar-dialog">
      <el-form :model="form" label-position="top" class="all-calendar-form">
        <el-form-item label="日历名称" required>
          <el-input v-model="form.name" maxlength="50" placeholder="输入名称" />
        </el-form-item>
        <el-form-item label="日历描述">
          <el-input v-model="form.description" type="textarea" maxlength="500" :rows="3" placeholder="输入描述" />
        </el-form-item>
        <el-form-item label="共享成员">
          <button type="button" class="member-picker" @click="focusSharedSelect">
            <span>＋</span>
            添加成员
          </button>
          <el-select v-if="showMemberSelect" ref="sharedSelectRef" v-model="sharedMemberIds" multiple filterable class="member-select">
            <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div v-if="sharedMemberIds.length" class="selected-members">
            <el-tag v-for="id in sharedMemberIds" :key="id" closable @close="removeSharedMember(id)">
              {{ userName(id) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="自动订阅范围" required>
          <div class="scope-row">
            <span class="scope-badge">成都市</span>
            <el-button link type="primary" @click="showScopeSelect = !showScopeSelect">修改</el-button>
          </div>
          <div v-if="showScopeSelect" class="scope-selectors">
            <el-select v-model="scopeType">
              <el-option label="全公司" value="ALL_COMPANY" />
              <el-option label="指定部门" value="DEPARTMENT" />
              <el-option label="多部门" value="DEPARTMENTS" />
              <el-option label="指定成员" value="MEMBERS" />
            </el-select>
            <el-select v-if="scopeType === 'DEPARTMENT' || scopeType === 'DEPARTMENTS'" v-model="scopeDepartmentIds" multiple>
              <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-if="scopeType === 'MEMBERS'" v-model="scopeUserIds" multiple>
              <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
const showScopeSelect = ref(false)
const showMemberSelect = ref(false)
const sharedSelectRef = ref()
const localRows: CalendarItem[] = [
  {
    id: -1,
    name: '产品发布日历',
    description: '统一管理产品发布、评审和上线安排。',
    type: 'ALL_MEMBER',
    color: '#2f7cf6',
    status: 'ACTIVE'
  },
  {
    id: -2,
    name: '城市运营日历',
    description: '面向全员同步重点运营节点。',
    type: 'ALL_MEMBER',
    color: '#3f6fa8',
    status: 'ACTIVE'
  }
]
const tableRows = computed(() => (rows.value.length ? rows.value : localRows))

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
  showScopeSelect.value = false
  showMemberSelect.value = false
  dialogVisible.value = true
}

function openEdit(row: CalendarItem) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row)
  scopeType.value = 'ALL_COMPANY'
  scopeDepartmentIds.value = []
  scopeUserIds.value = []
  sharedMemberIds.value = []
  showScopeSelect.value = false
  showMemberSelect.value = false
  dialogVisible.value = true
}

async function save() {
  const scopes = buildScopes()
  const payload = { ...form, scopes, sharedMemberIds: sharedMemberIds.value }
  try {
    if (form.id && form.id > 0) {
      await api.put(`/all-calendars/${form.id}`, payload)
    } else if (!form.id) {
      await api.post('/all-calendars', payload)
    }
  } catch {
    rows.value = rows.value.length ? rows.value : localRows
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await load()
}

async function disable(row: CalendarItem) {
  await ElMessageBox.confirm('确认停用该全员日历？')
  if (row.id > 0) {
    await api.post(`/all-calendars/${row.id}/disable`, {})
  }
  ElMessage.success('已停用')
  await load()
}

function buildScopes() {
  if (scopeType.value === 'ALL_COMPANY') return [{ scopeType: 'ALL_COMPANY' }]
  if (scopeType.value === 'MEMBERS') return scopeUserIds.value.map((userId) => ({ scopeType: 'MEMBER', userId }))
  return scopeDepartmentIds.value.map((departmentId) => ({ scopeType: 'DEPARTMENT', departmentId }))
}

function focusSharedSelect() {
  showMemberSelect.value = true
  setTimeout(() => sharedSelectRef.value?.focus?.())
}

function removeSharedMember(id: number) {
  sharedMemberIds.value = sharedMemberIds.value.filter((item) => item !== id)
}

function userName(id: number) {
  return users.value.find((item) => item.id === id)?.name ?? `成员${id}`
}
</script>

<style scoped>
h1 {
  margin: 0;
}

.all-calendar-toolbar {
  align-items: flex-start;
}

.all-calendar-toolbar p {
  margin: 8px 0 0;
  color: var(--calendar-soft-text);
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

.all-calendar-form :deep(.el-form-item) {
  margin-bottom: 28px;
}

.all-calendar-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #555;
  font-size: 16px;
  font-weight: 700;
}

.member-picker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 0;
  color: #174a82;
  background: transparent;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.member-picker span {
  font-size: 28px;
  line-height: 1;
}

.member-select {
  width: 100%;
  margin-top: 12px;
}

.selected-members {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.scope-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.scope-badge {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #d8dce4;
  color: #1f2937;
  background: #fff;
  font-weight: 700;
}

.scope-badge::before {
  content: "";
  width: 18px;
  height: 18px;
  margin-right: 8px;
  border-radius: 3px;
  background: #7fa4d8;
}

.scope-selectors {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}
</style>
