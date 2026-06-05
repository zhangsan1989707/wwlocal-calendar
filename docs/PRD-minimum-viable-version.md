# 企业微信日历/日程 H5 — 最小可用版本 PRD

---

## 1. 项目信息

| 项目 | 内容 |
|------|------|
| Language | 中文 |
| 技术栈（不动） | Vue 3 + TypeScript + Vite + Element Plus / Spring Boot 3 + JDK 17 / PostgreSQL |
| 项目名 | `wwlocal_calendar` |
| 原始需求 | 在已有代码基础上，1-2 天内修出可验收演示的最小可用版本，打通创建/编辑/删除/搜索/导出完整链路 |

---

## 2. 产品定义

### 2.1 产品目标

1. **数据真实持久化**：所有 CRUD 操作写入 PostgreSQL，刷新页面后数据不丢失，禁止「后端失败、前端提示成功」。
2. **核心链路可演示**：创建 → 编辑 → 删除 → 搜索 → 导出 五条链路全通，单用户流程可在 3 分钟内完整走完。
3. **字段对齐**：前端 payload → 后端 Service → 数据库表 三层字段名完全对齐，白名单过滤不再丢弃有效数据。

### 2.2 用户故事

| # | As a... | I want... | So that... |
|---|---------|-----------|------------|
| US1 | 员工 | 填写标题、时间、地点、参与人后保存日程 | 我能快速创建一个会议并通知参与人 |
| US2 | 日程发起人 | 编辑已有日程的所有字段（时间/地点/参与人/提醒/重复/附件/待办） | 变更计划后所有参与人看到最新信息 |
| US3 | 日程发起人 | 删除日程（普通→取消状态；重复→可选单次或系列） | 取消的日程不会继续出现在日历视图中 |
| US4 | 任意用户 | 按关键词搜索日程标题/地点/参与人，按日历筛选 | 快速定位到目标日程 |
| US5 | 管理员 | 将当前可见日程导出为 xlsx 文件并下载 | 用于汇报或离线存档 |

---

## 3. 需求池

### P0 — 必须修通（阻塞演示）

| ID | 需求 | 说明 | 对应问题 |
|----|------|------|----------|
| P0-1 | **EventForm 提交链路修通** | 前端 payload 字段名与后端 `EVENT_COLUMNS` 白名单 + 数据库 event 表字段名完全对齐；catch 块改为 `ElMessage.error` 并终止后续 `ElMessage.success` | 问题 1,2,3,5 |
| P0-2 | **创建日程真写入** | POST `/api/events` → CrudService.create → INSERT INTO event，所有字段（含 title, calendar_id, start_at, end_at, location, description, visibility, all_day, timezone）正确入库 | 问题 1,3,5 |
| P0-3 | **编辑日程真更新** | PUT `/api/events/:id` → CrudService.update → UPDATE event SET ... WHERE id=?，编辑后刷新页面数据仍存在 | 问题 1,3,5 |
| P0-4 | **删除日程正确取消** | DELETE `/api/events/:id` → status 改为 CANCELLED（不是 INACTIVE）；重复日程支持 scope=single（写 event_exception + 类型 CANCELLED）和 scope=series（改主 event status=CANCELLED） | 问题 6 |
| P0-5 | **搜索日程可用** | GET `/api/events?keyword=xxx` → ILIKE 匹配 title/location/description；GET `/api/events?calendarId=xxx` → 日历筛选 | — |
| P0-6 | **ID 生成机制修复** | event 及其他所有表的 BIGINT PRIMARY KEY 改为 `BIGSERIAL` 或添加 SEQUENCE + DEFAULT，确保 INSERT 不因缺 ID 而失败 | 问题 4 |
| P0-7 | **参与人管理修通** | event_participant 表添加 `UNIQUE(event_id, user_id)` 约束；respond() 的 ON CONFLICT 才能生效；保存日程时参与人正确写入 | 问题 7 |

### P1 — 应修通（增强演示效果）

| ID | 需求 | 说明 |
|----|------|------|
| P1-1 | **附件上传/列表/删除** | 真实文件上传到后端存储，EventForm 中可看到已上传附件列表，可删除单个附件 |
| P1-2 | **待办增删改** | EventForm 中可添加/删除待办项、切换完成状态、设置优先级(LOW/MEDIUM/HIGH) |
| P1-3 | **提醒选择** | 支持 即时/5分钟/15分钟/30分钟/1小时/1天/自定义分钟，保存后写入 event_reminder 表 |
| P1-4 | **重复日程 RRULE** | 支持每日/工作日/每周/双周/每月/每年，保存后写入 event_recurrence 表（rrule + end_at 或 occurrence_count） |
| P1-5 | **导出 xlsx 真下载** | 修复 ExportTaskService/exportEvents 的字段名（task_name→name, export_scope→scope, completed_at→finished_at），生成真实 xlsx 文件并通过 HTTP 响应下载 |
| P1-6 | **闲忙查询** | POST `/api/freebusy/query` 按 userIds + 时间范围返回忙闲时段，供创建日程时参考 |
| P1-7 | **回执响应** | 参与人在日程详情中可点击 ACCEPTED/DECLINED/TENTATIVE，状态写入 event_participant.response_status |
| P1-8 | **统一日程模型** | 废弃 `/api/schedules` 路径，全部收敛到 `/api/events`，删除 ScheduleController/ScheduleService 中的重复逻辑 |

### P2 — 可延后（不影响本次演示）

| ID | 需求 | 说明 |
|----|------|------|
| P2-1 | 外部联系人文本录入 | 参与人选择器支持自由文本输入（非企微用户），存为 `name` 文本而非 user_id |
| P2-2 | 部门全员快捷添加 | 参与人选部门 → 展开为该部门下所有用户 |
| P2-3 | 权限控制 | 发起人可编辑删除、参与人只读；非参与人不可见 PRIVATE 日程 |
| P2-4 | 审计日志写入 | 所有 CRUD 操作写入 audit_log 表 |
| P2-5 | 管理端页面 | AdminResourcePage 等管理端页面可用（用户/部门/日历/标签 CRUD） |

---

## 4. 技术对齐清单（修 Bug 专用）

以下是对 team-lead 已诊断问题的精确修复指引：

| # | 问题 | 根因 | 修复方案 |
|---|------|------|----------|
| 1 | Pinia store 假数据 | store 中有硬编码 mock 数据 | 全部替换为 `api.get('/events', { params })` 真实请求 |
| 2 | EventForm.vue catch 为空 | 第 195-197 行 `catch { }` 加注释说「前端先闭环」 | 改为 `catch (e) { ElMessage.error('保存失败: ' + (e?.message || '未知错误')); return; }`，`ElMessage.success` 移入 try 块末尾 |
| 3 | EventService EVENT_COLUMNS 与 DB 不一致 | `start_time/end_time/tag/tag_color/recurrence_rule/recurrence_end/allow_join` 在 DB 中不存在 | 改为 DB 真实字段：`start_at, end_at, tag_id, timezone, visibility`；前端 payload 同步改 `start_time→start_at, end_time→end_at, tag→tag_id` |
| 4 | BIGINT 无自增 | 所有表 `id BIGINT PRIMARY KEY` 无序列 | 改为 `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` 或 `BIGSERIAL`；同时更新 init.sql 的 INSERT 语句去掉硬编码 id 或使用 `OVERRIDING SYSTEM VALUE` |
| 5 | CrudService.filtered() 白名单过滤 | 前端字段名不在白名单中被静默丢弃 | 修复 P0-3 字段对齐后自然解决；建议加日志 `log.warn("dropped field: {}", key)` |
| 6 | disable() 写 INACTIVE | event.status CHECK 只允许 ACTIVE/CANCELLED | `CrudService.disable()` 改为 `SET status = 'CANCELLED'`；或在 EventService.remove() 中不调 disable()，直接 jdbc.update |
| 7 | respond() ON CONFLICT 缺约束 | event_participant 表无 UNIQUE(event_id, user_id) | 添加 `ALTER TABLE event_participant ADD UNIQUE(event_id, user_id)` |
| 8 | ExportService 字段名不一致 | `task_name→name, export_scope→scope, completed_at→finished_at` | 修复 INSERT 语句列名对齐 export_task 表 |
| 9 | 两套日程模型 | `/api/events` 和 `/api/schedules` 并存 | 废弃 schedules，ScheduleController 返回 410 Gone 或直接删除 |

---

## 5. 验收标准

### 5.1 功能验收

| 场景 | 验收步骤 | 预期结果 |
|------|----------|----------|
| **创建日程** | 打开日历 → 点击时间槽 → 填写标题/时间/地点/日历 → 保存 → 刷新页面 | 日程出现在正确日历和时间位置，数据来自 PostgreSQL |
| **编辑日程** | 点击已有日程 → 修改标题/时间/参与人 → 保存 → 刷新 | 修改后的信息持久存在 |
| **删除日程** | 点击日程 → 删除 → 确认 → 刷新 | 日程不再显示在日历中（status=CANCELLED，查询过滤掉） |
| **删除重复-单次** | 重复日程 → 删除 → 选择「仅本次」 | event_exception 写入 CANCELLED 记录，其他次数不受影响 |
| **删除重复-系列** | 重复日程 → 删除 → 选择「全部」 | 主 event status=CANCELLED，所有后续展开不再出现 |
| **搜索** | 搜索框输入关键词 → 回车 | 返回标题/地点/描述匹配的日程列表 |
| **日历筛选** | 在搜索页或主页切换日历勾选 | 只显示选中日历的日程 |
| **导出** | 点击导出 → 等待 → 下载 | 浏览器下载一个包含当前可见日程的 xlsx 文件 |
| **错误处理** | 断开后端 → 保存日程 | 前端显示「保存失败」红色提示，不显示「已保存」 |

### 5.2 非功能验收

- [ ] 创建 → 保存 → 刷新：数据不丢失
- [ ] 任何后端 4xx/5xx：前端必须红字报错
- [ ] 编辑已有日程：不能重复创建新记录
- [ ] 时区：start_at/end_at 使用 TIMESTAMPTZ，前端以 Asia/Shanghai 展示
- [ ] 分页/限制：搜索最多返回 500 条

---

## 6. 待确认问题

| # | 问题 | 影响范围 |
|---|------|----------|
| Q1 | 当前用户身份如何获取？前端是否有登录/Session 机制？还是先固定为 organizer_user_id=1（李宇航）？ | 创建日程时 organizer_user_id 和 operatorUserId |
| Q2 | 附件存储路径 `/app/uploads/` 是否需要在本地创建？文件大小限制沿用 system_config 中的 20MB？ | 附件上传功能 |
| Q3 | ID 自增方案：用 `BIGSERIAL` 修改 init.sql，还是保持 BIGINT + 应用层生成 ID？建议前者（改动最小） | 所有 INSERT 操作 |
| Q4 | 重复日程的「结束日期」和「重复次数」在 EventForm 中是否需要额外 UI？当前只选了 RRULE 字符串 | 重复日程编辑体验 |
| Q5 | 闲忙查询面板（EventForm 右侧 availability-pane）是否需要接真实数据？目前是静态「所有人都有空」 | 创建日程时展示参会人忙闲 |
| Q6 | 管理端页面（AdminResourcePage 等）是否纳入本次验收范围？还是仅修通用户端日历功能？ | P2 范围界定 |

---

## 7. 不做（明确边界）

- ❌ 真实企业微信消息推送 → 本地记录即可
- ❌ 外部联系人企微同步 → 文本自由录入
- ❌ 复杂 RBAC 权限 → 发起人可编辑、其他人只读
- ❌ 第三方日历同步（Google/Outlook）→ 不做
- ❌ 农历/节假日展示 → 不做（system_config 中的配置忽略）
- ❌ 备份恢复功能 → 不做
- ❌ 国际化/多语言 → 仅中文
