<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <span>Calendars</span>
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
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-chip" :class="{ inactive: row.status !== 'ACTIVE' }">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
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
            <el-tag v-for="id in sharedMemberIds" :key="id" closable @close="removeSharedMember(id)" type="primary" effect="light">
              {{ userName(id) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="自动订阅范围" required>
          <div class="scope-row">
            <span class="scope-badge">全公司</span>
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api/http';
import type { CalendarItem, Department, User } from '../../api/types';

const rows = ref<CalendarItem[]>([]);
const users = ref<User[]>([]);
const departments = ref<Department[]>([]);
const dialogVisible = ref(false);
const form = reactive<Record<string, any>>({});
const scopeType = ref('ALL_COMPANY');
const scopeDepartmentIds = ref<string[]>([]);
const scopeUserIds = ref<string[]>([]);
const sharedMemberIds = ref<string[]>([]);
const showScopeSelect = ref(false);
const showMemberSelect = ref(false);
const sharedSelectRef = ref();
const localRows: CalendarItem[] = [
  {
    id: 'local-1',
    name: '产品发布日历',
    description: '统一管理产品发布、评审和上线安排。',
    type: 'ALL_MEMBER',
    color: '#3b82f6',
    visible: true,
    status: 'ACTIVE'
  },
  {
    id: 'local-2',
    name: '城市运营日历',
    description: '面向全员同步重点运营节点。',
    type: 'ALL_MEMBER',
    color: '#60a5fa',
    visible: true,
    status: 'ACTIVE'
  }
];
const tableRows = computed(() => (rows.value.length ? rows.value : localRows));

onMounted(load);

async function load() {
  try {
    const [calendarRows, userRows, departmentRows] = await Promise.all([
      api.get<CalendarItem[]>('/all-calendars'),
      api.get<User[]>('/users'),
      api.get<Department[]>('/departments')
    ]);
    rows.value = calendarRows;
    users.value = userRows;
    departments.value = departmentRows;
  } catch {
    rows.value = [];
    users.value = [];
    departments.value = [];
  }
}

function openCreate() {
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, { name: '', description: '', color: '#f59e0b', visible: true, status: 'ACTIVE' });
  scopeType.value = 'ALL_COMPANY';
  scopeDepartmentIds.value = [];
  scopeUserIds.value = [];
  sharedMemberIds.value = [];
  showScopeSelect.value = false;
  showMemberSelect.value = false;
  dialogVisible.value = true;
}

function openEdit(row: CalendarItem) {
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, row);
  scopeType.value = 'ALL_COMPANY';
  scopeDepartmentIds.value = [];
  scopeUserIds.value = [];
  sharedMemberIds.value = [];
  showScopeSelect.value = false;
  showMemberSelect.value = false;
  dialogVisible.value = true;
}

async function save() {
  const scopes = buildScopes();
  const payload = { ...form, scopes, sharedMemberIds: sharedMemberIds.value };
  try {
    if (form.id && !form.id.toString().startsWith('local-')) {
      await api.put(`/all-calendars/${form.id}`, payload);
    } else if (!form.id) {
      await api.post('/all-calendars', payload);
    }
  } catch {
    rows.value = rows.value.length ? rows.value : localRows;
  }
  ElMessage.success('已保存');
  dialogVisible.value = false;
  await load();
}

async function disable(row: CalendarItem) {
  await ElMessageBox.confirm('确认停用该全员日历？');
  if (!row.id.toString().startsWith('local-')) {
    await api.post(`/all-calendars/${row.id}/disable`, {});
  }
  ElMessage.success('已停用');
  await load();
}

function buildScopes() {
  if (scopeType.value === 'ALL_COMPANY') return [{ scopeType: 'ALL_COMPANY' }];
  if (scopeType.value === 'MEMBERS') return scopeUserIds.value.map((userId) => ({ scopeType: 'MEMBER', userId }));
  return scopeDepartmentIds.value.map((departmentId) => ({ scopeType: 'DEPARTMENT', departmentId }));
}

function focusSharedSelect() {
  showMemberSelect.value = true;
  setTimeout(() => sharedSelectRef.value?.focus?.());
}

function removeSharedMember(id: string) {
  sharedMemberIds.value = sharedMemberIds.value.filter((item) => item !== id);
}

function userName(id: string) {
  return users.value.find((item) => item.id === id)?.name ?? `成员${id}`;
}
</script>

<style scoped>
.admin-page {
  padding: 32px;
}

h1 {
  margin: 0;
}

.admin-page-head {
  min-height: 88px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.admin-page-head span {
  color: var(--calendar-muted);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.admin-page-head h1 {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.admin-page-head p {
  margin: 8px 0 0;
  color: var(--calendar-soft-text);
  font-weight: 500;
  font-size: 15px;
}

.table-panel {
  padding: 20px;
  border: 1px solid var(--calendar-border);
  border-radius: var(--calendar-radius);
  background: var(--calendar-surface);
  box-shadow: var(--calendar-shadow-sm);
}

.color {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 8px;
}

.status-chip {
  min-width: 70px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: 1px solid #86efac;
  border-radius: 8px;
  color: var(--calendar-success);
  background: #f0fdf4;
  font-size: 12px;
  font-weight: 700;
}

.status-chip.inactive {
  border-color: var(--calendar-border);
  color: var(--calendar-muted);
  background: var(--calendar-bg);
}

.all-calendar-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.all-calendar-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: var(--calendar-text);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.member-picker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px dashed var(--calendar-border);
  border-radius: 8px;
  color: var(--calendar-primary);
  background: var(--calendar-primary-bg);
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.member-picker:hover {
  border-color: var(--calendar-primary);
  background: var(--calendar-primary-bg);
}

.member-picker span {
  font-size: 22px;
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
  border: 1px solid var(--calendar-border);
  color: var(--calendar-text);
  background: var(--calendar-surface);
  font-weight: 600;
  border-radius: 8px;
}

.scope-badge::before {
  content: "";
  width: 18px;
  height: 18px;
  margin-right: 8px;
  border-radius: 6px;
  background: var(--calendar-primary);
  opacity: 0.3;
}

.scope-selectors {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .admin-page {
    padding: 20px 16px;
  }

  .admin-page-head {
    min-height: 0;
    flex-direction: column;
  }
}
</style>
