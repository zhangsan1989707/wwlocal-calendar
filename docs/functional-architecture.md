# 企业协同日历 H5 系统 — 功能架构文档

> 生成时间：2026-06-05  
> 版本：v2.0（基于最新代码更新）

---

## 一、系统概述

企业协同日历 H5 系统面向 **PC Web** 与 **移动端 H5**，提供企业内部的日历协同能力。系统分为两大终端：

| 终端 | 入口 | 目标用户 |
|------|------|----------|
| **用户端** | `/calendar` | 全体员工 — 查看日程、创建/编辑日程、搜索日程、参会回执 |
| **管理端** | `/admin` | 管理员 — 组织数据维护、日历管理、审计日志、备份恢复、系统配置 |

**本期边界**：不包含登录认证、消息通知、会议室预约、外部联系人、第三方日历同步和完整权限体系。

---

## 二、技术架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                        用户浏览器                              │
│  ┌──────────────────────┐  ┌──────────────────────────────┐  │
│  │    用户端 (Calendar)   │  │     管理端 (Admin Console)     │  │
│  │  Vue 3 + Element Plus │  │   Vue 3 + Element Plus       │  │
│  │  Pinia Store          │  │   Pinia Store                │  │
│  └──────────┬───────────┘  └──────────────┬───────────────┘  │
│             │          HTTP /api/*         │                  │
└─────────────┼──────────────────────────────┼──────────────────┘
              │                              │
    ┌─────────▼──────────────────────────────▼──────────┐
    │                   Nginx (Port 80)                   │
    │  静态文件服务 + /api 反向代理 + /uploads 附件代理    │
    └─────────────────────┬─────────────────────────────┘
                          │
    ┌─────────────────────▼─────────────────────────────┐
    │         Spring Boot 3 (Port 8080, JDK 17)          │
    │  ┌───────────────────────────────────────────────┐ │
    │  │  Controller 层                                  │ │
    │  │  EventController / MasterDataController        │ │
    │  │  OpsController                                  │ │
    │  ├───────────────────────────────────────────────┤ │
    │  │  Service 层                                     │ │
    │  │  EventService / CrudService / AuditService     │ │
    │  │  CalendarService                                │ │
    │  ├───────────────────────────────────────────────┤ │
    │  │  数据层 (JdbcTemplate 直接 SQL)                 │ │
    │  └───────────────────────────────────────────────┘ │
    └─────────────────────┬─────────────────────────────┘
                          │
    ┌─────────────────────▼─────────────────────────────┐
    │              PostgreSQL (Port 5432)                 │
    │  16 个核心表 + 完整约束 (FK / CHECK / UNIQUE)       │
    │  标准 Schema：database/init.sql                      │
    └───────────────────────────────────────────────────┘
```

**技术栈清单**：

| 层面 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 3.5.x |
| 构建工具 | Vite | 6.0.x |
| UI 组件库 | Element Plus | 2.8.x |
| 状态管理 | Pinia | 2.2.x |
| 路由 | Vue Router | 4.5.x |
| E2E 测试 | Playwright | 1.60 |
| 后端框架 | Spring Boot | 3.3.6 |
| Java 版本 | JDK | 17 |
| 数据库 | PostgreSQL | 16 |
| Excel 导出 | Apache POI | 5.3.0 |
| 容器化 | Docker Compose | — |

---

## 三、数据库设计

### 3.1 ER 概要

```
departments ──┐
              ├── users ────────────┐
              │                     │
calendar_tag ─┤                     │
              │                     │
              ├── calendars ────────┤
              │    │               │
              │    ├── calendar_auto_subscribe_scope
              │    ├── calendar_shared_member
              │    └── calendar_subscription
              │                     │
              └── event ◄──────────┘
                   │
                   ├── event_participant
                   ├── event_reminder
                   ├── event_recurrence ── event_exception
                   ├── event_attachment
                   └── event_todo

运维辅助表：export_task / backup_record / audit_log / system_config
```

### 3.2 核心表清单 (16 张表)

| # | 表名 | 用途 | 主键类型 |
|---|------|------|----------|
| 1 | `departments` | 组织部门 | VARCHAR(36) UUID |
| 2 | `users` | 系统用户 | VARCHAR(36) UUID |
| 3 | `calendar_tag` | 日历标签（会议/出差等） | BIGSERIAL |
| 4 | `calendars` | 日历（个人/共享/公共/全员） | VARCHAR(36) UUID |
| 5 | `calendar_auto_subscribe_scope` | 全员日历自动订阅范围 | BIGSERIAL |
| 6 | `calendar_shared_member` | 日历共享成员 | BIGSERIAL |
| 7 | `calendar_subscription` | 用户日历订阅关系 | BIGSERIAL |
| 8 | `event` | 日程主体 | BIGSERIAL |
| 9 | `event_participant` | 日程参与人（含回执状态） | BIGSERIAL |
| 10 | `event_reminder` | 日程提醒设置 | BIGSERIAL |
| 11 | `event_recurrence` | 重复规则 (RRULE) | BIGSERIAL |
| 12 | `event_exception` | 重复日程异常（取消/移动） | BIGSERIAL |
| 13 | `event_attachment` | 日程附件 | BIGSERIAL |
| 14 | `event_todo` | 日程待办 | BIGSERIAL |
| 15 | `export_task` | 导出任务记录 | BIGSERIAL |
| 16 | `backup_record` | 备份/恢复记录 | BIGSERIAL |
| 17 | `audit_log` | 审计日志 | BIGSERIAL |
| 18 | `system_config` | 系统配置 (KV) | BIGSERIAL |

### 3.3 日历类型体系

| 类型 | 含义 | 典型场景 |
|------|------|----------|
| `PERSONAL` | 个人日历 | 每个员工默认拥有一个，管理个人工作安排 |
| `SHARED` | 共享日历 | 跨部门协作日历，可指定共享成员 |
| `PUBLIC` | 公共日历 | 全员可见但不可写的公共安排 |
| `ALL_MEMBER` | 全员日历 | 公司级统一安排，自动订阅给全员 |

### 3.4 关键约束

- **日程时间**: `CHECK (end_at > start_at)`
- **日程状态**: `CHECK (status IN ('ACTIVE', 'CANCELLED'))`
- **参会人唯一**: `UNIQUE (event_id, user_id)` — 防止重复添加
- **软删除**: 删除日程不物理删除，设置 `status = 'CANCELLED'`

---

## 四、API 接口清单

所有接口前缀：`/api`  
统一响应格式：`{ success: boolean, data: T, message: string }`

### 4.1 日程接口 (EventController)

| 方法 | 路径 | 说明 | 关键参数 |
|------|------|------|----------|
| GET | `/events` | 搜索日程 | keyword, calendarId, tag_id, start, end, userId |
| POST | `/events` | 创建日程 | title, calendar_id, start_at, end_at, participantIds, todos, reminders, rrule |
| PUT | `/events/:id` | 更新日程 | 同创建参数 |
| DELETE | `/events/:id` | 删除日程 | operatorUserId, scope (single/series) |
| POST | `/events/:id/respond` | 参会回执 | userId, status (ACCEPTED/DECLINED/TENTATIVE/NEEDS_ACTION) |
| POST | `/freebusy/query` | 忙闲查询 | userIds, startTime, endTime |
| GET | `/events/:id/attachments` | 获取日程附件 | — |
| GET | `/events/:id/participants` | 获取参与人列表 | — |
| GET | `/events/:id/reminders` | 获取提醒设置 | — |
| GET | `/events/:id/todos` | 获取待办列表 | — |
| PUT | `/todos/:todoId` | 切换待办完成状态 | completed, operatorUserId |
| POST | `/export/events` | 导出日程 Excel | operatorUserId |

### 4.2 主数据接口 (MasterDataController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users` | 用户列表（支持 department_id / status 筛选） |
| POST | `/users` | 新增用户 |
| PUT | `/users/:id` | 更新用户 |
| GET | `/departments` | 部门列表 |
| POST | `/departments` | 新增部门 |
| PUT | `/departments/:id` | 更新部门 |
| GET | `/calendars` | 日历列表 |
| POST | `/calendars` | 新增日历 |
| PUT | `/calendars/:id` | 更新日历 |
| GET | `/all-calendars` | 全员日历列表（含订阅范围和共享成员） |
| POST | `/all-calendars` | 新增全员日历 |
| PUT | `/all-calendars/:id` | 更新全员日历 |
| POST | `/all-calendars/:id/disable` | 停用全员日历 |
| GET | `/tags` | 标签列表 |
| POST | `/tags` | 新增标签 |
| PUT | `/tags/:id` | 更新标签 |
| GET | `/settings` | 系统配置列表 |
| PUT | `/settings` | 批量更新系统配置 |

### 4.3 运维接口 (OpsController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/attachments` | 附件列表 |
| POST | `/files/upload` | 上传附件 (multipart) |
| GET | `/files/:id/download` | 下载附件 |
| POST | `/attachments/:id/delete` | 删除附件 |
| GET | `/exports` | 导出任务列表 |
| GET | `/audit-logs` | 审计日志列表 |
| GET | `/backup-records` | 备份记录列表 |
| POST | `/backup` | 发起备份 |
| POST | `/backup/:id/restore-record` | 登记恢复核验结果 |
| GET | `/admin/overview` | 管理端概览数据 |

---

## 五、前端架构

### 5.1 目录结构

```
frontend/src/
├── api/
│   ├── http.ts          # HTTP 客户端 (fetch 封装，统一 ApiResult 解析)
│   └── types.ts         # TypeScript 类型定义 (User, Department, CalendarItem, EventItem 等)
├── components/
│   ├── EventForm.vue     # 日程创建/编辑表单（弹窗，含参与人/附件/待办/提醒/重复规则）
│   ├── EventDetail.vue   # 日程详情抽屉（含回执状态/附件/待办/参与人）
│   ├── CalendarGrid.vue  # 月视图网格组件
│   └── AllCalendarForm.vue # 全员日历编辑表单
├── stores/
│   ├── app.ts            # 主 Store（用户/部门/日历/标签/日程 + loadBase/loadEvents）
│   └── calendar.ts       # 旧日历 Store（已标记 deprecated，桥接兼容）
├── views/
│   ├── calendar/
│   │   ├── CalendarHome.vue    # 日历主页（月/周/日视图 + PC/移动端自适应）
│   │   └── CalendarSearch.vue  # 日程搜索页
│   └── admin/
│       ├── AdminLayout.vue     # 管理端布局（侧边栏导航 + 顶部栏）
│       ├── AdminHome.vue       # 管理首页概览
│       ├── AdminTablePage.vue  # 通用 CRUD 表格页（用户/部门/日历/日程/附件/导出/审计/备份/配置）
│       └── AllCalendars.vue    # 全员日历管理页
├── router/
│   └── index.ts          # 路由配置
├── styles/
│   └── global.css        # 全局样式与 CSS 变量
├── main.ts               # 应用入口
└── App.vue               # 根组件
```

### 5.2 路由架构

```
/ (重定向到 /calendar)
├── /calendar                  → CalendarHome（用户端日历主页）
│   └── /calendar/search        → CalendarSearch（日程搜索页）
└── /admin                      → AdminLayout（管理端布局）
    ├── /admin                  → AdminHome（概览）
    ├── /admin/users            → AdminTablePage（系统用户管理）
    ├── /admin/departments      → AdminTablePage（部门管理）
    ├── /admin/calendars        → AdminTablePage（日历管理）
    ├── /admin/all-calendars    → AllCalendars（全员日历）
    ├── /admin/events           → AdminTablePage（日程管理）
    ├── /admin/attachments      → AdminTablePage（附件管理）
    ├── /admin/exports          → AdminTablePage（导出任务管理）
    ├── /admin/audit-logs       → AdminTablePage（审计日志）
    ├── /admin/backup           → AdminTablePage（备份恢复）
    └── /admin/settings         → AdminTablePage（系统配置）
```

### 5.3 状态管理

**主 Store (`app.ts`)**：
- `users`, `departments`, `calendars`, `tags`, `events` — 基础数据缓存
- `currentUserId` — 当前操作人（无登录的简化模式）
- `loadBase()` — 并行加载用户/部门/日历/标签
- `loadEvents(query)` — 加载日程列表

**旧 Store (`calendar.ts`)**：
- 保留 `viewMode`、`selectedCalendarIds` 等日历视图状态
- `saveEvent` / `saveCalendar` 已标记 deprecated

### 5.4 响应式设计

- **桌面端 (≥920px)**：左侧边栏 + 右侧日历网格 / 管理表格
- **移动端 (<920px)**：全屏月视图 + 底部日程列表 + 设置面板
- 移动端特有：日/三日/周切换、日历设置页、底部 Sheet 添加日历

### 5.5 E2E 测试覆盖

使用 Playwright 编写，覆盖 13 个核心验收用例：

| 编号 | 用例 | 状态 |
|------|------|------|
| A-U-01 | 进入日历首页，页面正常加载 | ✅ |
| A-U-02 | 切换日/周/月/列表视图 | ✅ |
| A-U-03 | 创建普通日程 → 刷新后仍在 | ✅ |
| A-U-04 | 创建全天和跨天日程 | ✅ |
| A-U-05 | 编辑日程 — 修改标题 | ✅ |
| A-U-06 | 删除日程 | ✅ |
| A-U-07 | 添加参会人并触发忙闲冲突检测 | ✅ |
| A-U-08 | 参会回执 — 接受/拒绝/待定 | ✅ |
| A-U-09 | 按关键词搜索日程 | ✅ |
| A-A-02 | 管理端 — 系统用户管理 — 查看列表 | ✅ |
| A-A-03 | 管理端 — 部门管理 — 查看列表 | ✅ |
| A-A-04 | 管理端 — 日历管理 — 查看列表 | ✅ |
| A-A-06 | 管理端 — 审计日志 — 可查看 | ✅ |
| A-A-07 | 管理端 — 导出任务管理 — 可查看 | ✅ |

---

## 六、功能模块清单

### 6.1 用户端功能

| 功能模块 | 功能点 | 实现状态 |
|----------|--------|----------|
| **日历视图** | 月视图（含农历/节假日标注） | ✅ 已实现 |
| | 周视图（时间网格，8:00-18:00） | ✅ 已实现 |
| | 日视图（单天时间网格） | ✅ 已实现 |
| | 侧边栏迷你日历快速导航 | ✅ 已实现 |
| | 日历订阅筛选（个人/共享日历勾选） | ✅ 已实现 |
| **日程 CRUD** | 创建日程（标题/时间/地点/描述/日历/标签） | ✅ 已实现 |
| | 编辑日程 | ✅ 已实现 |
| | 删除日程（普通/重复系列/单次） | ✅ 已实现 |
| | 全天日程 | ✅ 已实现 |
| | 跨天日程 | ✅ 已实现 |
| | 日程详情抽屉查看 | ✅ 已实现 |
| **参会协同** | 添加参与人（按用户选择） | ✅ 已实现 |
| | 忙闲冲突检测 | ✅ 已实现 |
| | 参会回执（接受/拒绝/待定/未响应） | ✅ 已实现 |
| | 回执统计（接受/拒绝/待定/待确认计数） | ✅ 已实现 |
| **日程增强** | 附件上传/下载/删除 | ✅ 已实现 |
| | 待办事项（标题/优先级/完成切换） | ✅ 已实现 |
| | 提醒设置（多提醒时间） | ✅ 已实现 |
| | 重复规则（RRULE + 结束条件） | ✅ 已实现 |
| | 重复日程异常处理（取消/移动） | ✅ 已实现 |
| **搜索** | 关键词搜索（标题/地点/描述） | ✅ 已实现 |
| | 日历筛选 | ✅ 已实现 |
| | 标签筛选 | ✅ 已实现 |
| | 日期范围筛选 | ✅ 已实现 |
| | 桌面表格 + 移动端卡片双视图 | ✅ 已实现 |
| **导出** | 导出日程为 Excel | ✅ 已实现 |

### 6.2 管理端功能

| 功能模块 | 功能点 | 实现状态 |
|----------|--------|----------|
| **概览** | 系统统计数据（用户/部门/日历/日程/附件/待办数） | ✅ 已实现 |
| | 服务状态（后端/数据库/附件目录） | ✅ 已实现 |
| | 近期操作日志 | ✅ 已实现 |
| **基础数据** | 系统用户管理（CRUD + 状态管理） | ✅ 已实现 |
| | 部门管理（CRUD + 启用/停用） | ✅ 已实现 |
| | 日历管理（CRUD） | ✅ 已实现 |
| | 全员日历管理（含订阅范围 + 共享成员） | ✅ 已实现 |
| | 标签管理（CRUD） | ✅ 已实现 |
| **运维管理** | 日程管理（查看/导出） | ✅ 已实现 |
| | 附件管理（查看/删除） | ✅ 已实现 |
| | 导出任务管理（查看状态） | ✅ 已实现 |
| | 审计日志（按模块/动作/对象筛选） | ✅ 已实现 |
| | 备份恢复（发起备份 + 登记核验结果） | ✅ 已实现 |
| | 系统配置（附件限制/导出上限/农历开关/备份保留） | ✅ 已实现 |

---

## 七、部署架构

### 7.1 Docker Compose 三容器部署

```
┌──────────────────────────────────────────────────────┐
│                    Docker Compose                      │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │   frontend    │  │   backend    │  │  postgres    │  │
│  │  Nginx :80    │  │  SpringBoot  │  │  PG16 :5432  │  │
│  │               │  │  :8080       │  │              │  │
│  │  /api/* ──────┼─►│              │──│►             │  │
│  │  /uploads/ ───┤  │              │  │              │  │
│  │  /* (SPA)     │  │              │  │              │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
│                                                        │
│  Volumes:                                              │
│  - postgres_data → PG 数据持久化                        │
│  - ./uploads     → 附件持久化                           │
│  - ./backup      → 备份持久化                           │
└──────────────────────────────────────────────────────┘
```

### 7.2 端口映射

| 服务 | 容器端口 | 主机端口 (可配) | 环境变量 |
|------|----------|-----------------|----------|
| Nginx (前端) | 80 | 3007 | `FRONTEND_PORT` |
| Spring Boot | 8080 | 8080 | `BACKEND_PORT` |
| PostgreSQL | 5432 | 15432 | `POSTGRES_PORT` |

### 7.3 关键配置

**数据库初始化**：`database/init.sql` 在容器启动时自动执行（挂载到 `/docker-entrypoint-initdb.d/`），包含完整的建表 + 种子数据。

**Schema 迁移**：`database/migrate-v2.sql` 支持从旧版 Schema (`sys_department`/`sys_user`/`calendar`) 迁移到新版标准命名。

**附件存储**：通过 volume `./uploads` 挂载，后端 `app.upload-dir` 指向 `/app/uploads`。

---

## 八、关键设计决策

| 决策 | 内容 | 原因 |
|------|------|------|
| **Schema 标准** | `database/init.sql` 为唯一标准 Schema | 16 个核心表，完整约束，有种子数据 |
| **字段映射** | 全链路使用 DB 标准字段名（如 `start_at` 而非 `start_time`） | 避免中间映射层，降低复杂度 |
| **软删除** | `status = 'CANCELLED'` | 与 event 表 CHECK 约束一致 |
| **无需登录** | 前端固定 `currentUserId` | 本期不含认证体系 |
| **直接 SQL** | 使用 JdbcTemplate 而非 JPA/MyBatis | 灵活度高，代码量可控 |
| **通用 CRUD** | `CrudService` 封装动态 SQL 构建 | 减少重复代码，白名单防注入 |
| **响应式设计** | 920px 断点切换 PC/移动端 | 一套代码覆盖双端 |

---

## 九、文件清单总览

```
wwlocal-calendar/
├── README.md                     # 项目说明
├── backend/                      # Spring Boot 后端
│   ├── pom.xml                   # Maven 配置 (Spring Boot 3.3.6, JDK 17)
│   ├── Dockerfile                # 后端容器镜像
│   └── src/main/
│       ├── java/com/wwlocal/calendar/
│       │   ├── CalendarApplication.java        # 应用入口
│       │   ├── api/
│       │   │   ├── ApiResponse.java            # 统一响应封装
│       │   │   └── ApiExceptionHandler.java    # 全局异常处理
│       │   ├── common/
│       │   │   ├── GlobalExceptionHandler.java # 异常处理
│       │   │   └── NotFoundException.java      # 404 异常
│       │   ├── config/
│       │   │   ├── AppProperties.java          # 应用配置 (upload/backup/export dirs)
│       │   │   └── WebConfig.java              # CORS 配置
│       │   ├── controller/
│       │   │   ├── EventController.java        # 日程 API
│       │   │   ├── MasterDataController.java   # 主数据 API
│       │   │   └── OpsController.java          # 运维 API
│       │   └── service/
│       │       ├── EventService.java           # 日程业务逻辑
│       │       ├── CrudService.java            # 通用 CRUD
│       │       ├── AuditService.java           # 审计日志
│       │       └── CalendarService.java        # 全员日历管理
│       └── resources/
│           └── application.yml                 # Spring 配置
├── frontend/                     # Vue 3 前端
│   ├── package.json              # npm 依赖
│   ├── vite.config.ts            # Vite 构建配置
│   ├── playwright.config.ts      # Playwright E2E 测试配置
│   ├── Dockerfile                # 前端容器镜像 (Nginx)
│   ├── index.html                # HTML 入口
│   ├── tests/e2e/
│   │   ├── acceptance.spec.ts    # 验收测试用例
│   │   └── helpers.ts            # 测试辅助函数
│   └── src/
│       ├── main.ts               # Vue 应用入口
│       ├── App.vue               # 根组件
│       ├── router/index.ts       # 路由配置
│       ├── api/
│       │   ├── http.ts           # HTTP 客户端
│       │   └── types.ts          # TypeScript 类型
│       ├── stores/
│       │   ├── app.ts            # 主 Store
│       │   └── calendar.ts       # 日历 Store (deprecated)
│       ├── components/
│       │   ├── EventForm.vue     # 日程编辑表单
│       │   ├── EventDetail.vue   # 日程详情抽屉
│       │   ├── CalendarGrid.vue  # 日历网格
│       │   └── AllCalendarForm.vue # 全员日历表单
│       ├── views/
│       │   ├── calendar/
│       │   │   ├── CalendarHome.vue    # 日历主页
│       │   │   └── CalendarSearch.vue  # 搜索页
│       │   └── admin/
│       │       ├── AdminLayout.vue     # 管理端布局
│       │       ├── AdminHome.vue       # 管理首页
│       │       ├── AdminTablePage.vue  # 通用表格页
│       │       └── AllCalendars.vue    # 全员日历
│       └── types/calendar.ts   # 旧类型定义 (deprecated)
├── database/
│   ├── init.sql                 # 标准 Schema + 种子数据
│   └── migrate-v2.sql           # V2 迁移脚本
├── deploy/
│   ├── docker-compose.yml       # 容器编排
│   ├── nginx.conf               # Nginx 配置
│   └── uploads/                 # 附件持久化目录
├── docs/
│   ├── system_design.md         # 系统架构设计（上一版本）
│   ├── functional-architecture.md  # 本文档
│   ├── verification.md          # 编译运行验证记录
│   ├── PRD-minimum-viable-version.md  # PRD 文档
│   ├── sequence-diagram.mermaid # 时序图
│   └── class-diagram.mermaid    # 类图
├── 需求清单/                    # 产品需求文档
└── 设计稿/                      # UI 设计参考
```

---

## 十、代码质量与改进轨迹

### 近期重大更新 (V2)

| 变更项 | 描述 | 影响范围 |
|--------|------|----------|
| **Schema 标准化** | 统一使用 `database/init.sql`，废弃多套过期 Schema | 数据库 + 后端 SQL |
| **表重命名** | `sys_department`→`departments`, `sys_user`→`users`, `calendar`→`calendars` | 全栈 |
| **ID 类型迁移** | BIGINT → VARCHAR(36) UUID（用户/部门/日历） | 数据库 + 后端 + 前端 |
| **字段重命名** | `start_time`→`start_at`, `end_time`→`end_at`, `tag`→`tag_id`, `change_summary`→`detail` 等 | 全栈 |
| **假数据清理** | 移除前端硬编码假数据，全部走 API | 前端 Store + 组件 |
| **错误处理修复** | 修复空 `catch {}` 和假成功提示 | 前端组件 |
| **V2 迁移脚本** | 提供平滑的 Schema 升级路径 | 数据库 |
| **E2E 测试完善** | 覆盖 13 个验收用例 | 前端测试 |
| **移动端优化** | 新增移动端日历设置、三日视图、日历添加 Sheet | 前端视图 |
| **审计日志完善** | 所有 CRUD 操作自动记录 (operatorUserId + 模块 + 动作) | 后端 Service |
