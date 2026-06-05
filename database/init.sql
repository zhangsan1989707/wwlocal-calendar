BEGIN;

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS backup_record;
DROP TABLE IF EXISTS export_task;
DROP TABLE IF EXISTS event_todo;
DROP TABLE IF EXISTS event_attachment;
DROP TABLE IF EXISTS event_exception;
DROP TABLE IF EXISTS event_recurrence;
DROP TABLE IF EXISTS event_reminder;
DROP TABLE IF EXISTS event_participant;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS calendar_subscription;
DROP TABLE IF EXISTS calendar_shared_member;
DROP TABLE IF EXISTS calendar_auto_subscribe_scope;
DROP TABLE IF EXISTS calendar;
DROP TABLE IF EXISTS calendar_tag;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_department;

CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT REFERENCES sys_department(id),
  name VARCHAR(50) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  department_id BIGINT REFERENCES sys_department(id),
  name VARCHAR(50) NOT NULL,
  email VARCHAR(120),
  phone VARCHAR(30),
  admin_flag BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_tag (
  id BIGINT PRIMARY KEY,
  name VARCHAR(30) NOT NULL UNIQUE,
  color VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar (
  id BIGINT PRIMARY KEY,
  owner_user_id BIGINT REFERENCES sys_user(id),
  name VARCHAR(80) NOT NULL,
  description TEXT,
  type VARCHAR(20) NOT NULL CHECK (type IN ('PERSONAL', 'SHARED', 'PUBLIC', 'ALL_MEMBER')),
  color VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_by BIGINT REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_auto_subscribe_scope (
  id BIGINT PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('COMPANY', 'DEPARTMENT', 'USER')),
  department_id BIGINT REFERENCES sys_department(id),
  user_id BIGINT REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (
    (scope_type = 'COMPANY' AND department_id IS NULL AND user_id IS NULL)
    OR (scope_type = 'DEPARTMENT' AND department_id IS NOT NULL AND user_id IS NULL)
    OR (scope_type = 'USER' AND department_id IS NULL AND user_id IS NOT NULL)
  )
);

CREATE TABLE calendar_shared_member (
  id BIGINT PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES sys_user(id),
  role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'EDITOR')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (calendar_id, user_id)
);

CREATE TABLE calendar_subscription (
  id BIGINT PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES sys_user(id),
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (calendar_id, user_id)
);

CREATE TABLE event (
  id BIGINT PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id),
  tag_id BIGINT REFERENCES calendar_tag(id),
  organizer_user_id BIGINT NOT NULL REFERENCES sys_user(id),
  title VARCHAR(120) NOT NULL,
  location VARCHAR(200),
  description TEXT,
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ NOT NULL,
  all_day BOOLEAN NOT NULL DEFAULT FALSE,
  timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Shanghai',
  visibility VARCHAR(20) NOT NULL DEFAULT 'DEFAULT' CHECK (visibility IN ('DEFAULT', 'PUBLIC', 'PRIVATE')),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (end_at > start_at)
);

CREATE TABLE event_participant (
  id BIGINT PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES sys_user(id),
  department_id BIGINT REFERENCES sys_department(id),
  response_status VARCHAR(20) NOT NULL DEFAULT 'NEEDS_ACTION'
    CHECK (response_status IN ('ACCEPTED', 'DECLINED', 'TENTATIVE', 'NEEDS_ACTION')),
  response_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK ((user_id IS NOT NULL AND department_id IS NULL) OR (user_id IS NULL AND department_id IS NOT NULL))
);

CREATE TABLE event_reminder (
  id BIGINT PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  minutes_before INTEGER NOT NULL CHECK (minutes_before >= 0),
  method VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' CHECK (method IN ('SYSTEM', 'EMAIL')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_recurrence (
  id BIGINT PRIMARY KEY,
  event_id BIGINT NOT NULL UNIQUE REFERENCES event(id) ON DELETE CASCADE,
  rrule TEXT NOT NULL,
  end_at TIMESTAMPTZ,
  occurrence_count INTEGER CHECK (occurrence_count IS NULL OR occurrence_count > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_exception (
  id BIGINT PRIMARY KEY,
  recurrence_id BIGINT NOT NULL REFERENCES event_recurrence(id) ON DELETE CASCADE,
  original_start_at TIMESTAMPTZ NOT NULL,
  exception_type VARCHAR(20) NOT NULL CHECK (exception_type IN ('CANCELLED', 'MOVED')),
  new_start_at TIMESTAMPTZ,
  new_end_at TIMESTAMPTZ,
  reason VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (
    (exception_type = 'CANCELLED' AND new_start_at IS NULL AND new_end_at IS NULL)
    OR (exception_type = 'MOVED' AND new_start_at IS NOT NULL AND new_end_at IS NOT NULL AND new_end_at > new_start_at)
  )
);

CREATE TABLE event_attachment (
  id BIGINT PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  uploaded_by BIGINT NOT NULL REFERENCES sys_user(id),
  file_name VARCHAR(200) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  content_type VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_todo (
  id BIGINT PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  assignee_user_id BIGINT REFERENCES sys_user(id),
  title VARCHAR(120) NOT NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  due_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE export_task (
  id BIGINT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  scope VARCHAR(20) NOT NULL CHECK (scope IN ('PERSONAL', 'CALENDAR', 'MANAGEMENT')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'FINISHED', 'FAILED')),
  created_by BIGINT NOT NULL REFERENCES sys_user(id),
  file_path VARCHAR(500),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE backup_record (
  id BIGINT PRIMARY KEY,
  backup_type VARCHAR(20) NOT NULL CHECK (backup_type IN ('DATABASE', 'UPLOADS', 'FULL')),
  action VARCHAR(20) NOT NULL CHECK (action IN ('BACKUP', 'RESTORE_RECORD')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'FINISHED', 'FAILED')),
  file_path VARCHAR(500),
  file_size BIGINT CHECK (file_size IS NULL OR file_size >= 0),
  operator_user_id BIGINT NOT NULL REFERENCES sys_user(id),
  verification_result TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ
);

CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY,
  operator_user_id BIGINT REFERENCES sys_user(id),
  module VARCHAR(60) NOT NULL,
  action VARCHAR(60) NOT NULL,
  object_type VARCHAR(60) NOT NULL,
  object_id BIGINT,
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE system_config (
  id BIGINT PRIMARY KEY,
  config_key VARCHAR(80) NOT NULL UNIQUE,
  config_value TEXT NOT NULL,
  description VARCHAR(200),
  updated_by BIGINT REFERENCES sys_user(id),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sys_user_department ON sys_user(department_id);
CREATE INDEX idx_calendar_type_enabled ON calendar(type, enabled);
CREATE INDEX idx_calendar_subscription_user ON calendar_subscription(user_id, visible);
CREATE INDEX idx_event_time_range ON event(start_at, end_at);
CREATE INDEX idx_event_calendar_time ON event(calendar_id, start_at, end_at);
CREATE INDEX idx_event_participant_user ON event_participant(user_id, response_status);
CREATE INDEX idx_event_attachment_event ON event_attachment(event_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

INSERT INTO sys_department (id, name, sort_order) VALUES
  (1, '产品部', 1),
  (2, '研发部', 2),
  (3, '质量部', 3);

INSERT INTO sys_user (id, department_id, name, email, phone, admin_flag) VALUES
  (1, 1, '李宇航', 'liyuhang@example.local', '13800000001', TRUE),
  (2, 1, '张三', 'zhangsan@example.local', '13800000002', FALSE),
  (3, 2, '李四', 'lisi@example.local', '13800000003', FALSE),
  (4, 2, '王五', 'wangwu@example.local', '13800000004', FALSE),
  (5, 3, '赵六', 'zhaoliu@example.local', '13800000005', FALSE),
  (6, 1, '产品经理', 'pm@example.local', '13800000006', TRUE),
  (7, 2, '研发负责人', 'rdlead@example.local', '13800000007', TRUE),
  (8, 3, '质量人员', 'qa@example.local', '13800000008', FALSE);

INSERT INTO calendar_tag (id, name, color) VALUES
  (1, '会议', '#3B82F6'),
  (2, '出差', '#F97316'),
  (3, '私事', '#A855F7'),
  (4, '值班', '#10B981'),
  (5, '培训', '#EF4444');

INSERT INTO calendar (id, owner_user_id, name, description, type, color, created_by) VALUES
  (1, 1, '李宇航个人日历', '个人工作安排', 'PERSONAL', '#2563EB', 1),
  (2, 2, '张三个人日历', '个人工作安排', 'PERSONAL', '#16A34A', 1),
  (3, 3, '李四个人日历', '个人工作安排', 'PERSONAL', '#9333EA', 1),
  (4, 4, '王五个人日历', '个人工作安排', 'PERSONAL', '#DC2626', 1),
  (5, 5, '赵六个人日历', '个人工作安排', 'PERSONAL', '#0891B2', 1),
  (6, 6, '产品经理个人日历', '个人工作安排', 'PERSONAL', '#F59E0B', 1),
  (7, 7, '研发负责人个人日历', '个人工作安排', 'PERSONAL', '#4F46E5', 1),
  (8, 8, '质量人员个人日历', '个人工作安排', 'PERSONAL', '#059669', 1),
  (9, NULL, '跨部门共享日历', '跨部门协作安排', 'SHARED', '#0EA5E9', 1),
  (10, NULL, '公共日历', '公开事项与公共安排', 'PUBLIC', '#64748B', 1),
  (11, NULL, '公司全员日历', '公司级统一安排', 'ALL_MEMBER', '#DB2777', 1);

INSERT INTO calendar_auto_subscribe_scope (id, calendar_id, scope_type) VALUES
  (1, 11, 'COMPANY');

INSERT INTO calendar_shared_member (id, calendar_id, user_id, role) VALUES
  (1, 11, 1, 'MANAGER'),
  (2, 11, 6, 'MANAGER'),
  (3, 9, 6, 'EDITOR'),
  (4, 9, 7, 'EDITOR');

INSERT INTO calendar_subscription (id, calendar_id, user_id, visible, sort_order) VALUES
  (1, 1, 1, TRUE, 1),
  (2, 2, 2, TRUE, 1),
  (3, 3, 3, TRUE, 1),
  (4, 4, 4, TRUE, 1),
  (5, 5, 5, TRUE, 1),
  (6, 6, 6, TRUE, 1),
  (7, 7, 7, TRUE, 1),
  (8, 8, 8, TRUE, 1),
  (9, 9, 1, TRUE, 2),
  (10, 9, 6, TRUE, 2),
  (11, 9, 7, TRUE, 2),
  (12, 10, 1, TRUE, 3),
  (13, 10, 2, TRUE, 3),
  (14, 10, 3, TRUE, 3),
  (15, 10, 4, TRUE, 3),
  (16, 10, 5, TRUE, 3),
  (17, 10, 6, TRUE, 3),
  (18, 10, 7, TRUE, 3),
  (19, 10, 8, TRUE, 3),
  (20, 11, 1, TRUE, 4),
  (21, 11, 2, TRUE, 4),
  (22, 11, 3, TRUE, 4),
  (23, 11, 4, TRUE, 4),
  (24, 11, 5, TRUE, 4),
  (25, 11, 6, TRUE, 4),
  (26, 11, 7, TRUE, 4),
  (27, 11, 8, TRUE, 4);

INSERT INTO event (id, calendar_id, tag_id, organizer_user_id, title, location, description, start_at, end_at, all_day, visibility) VALUES
  (1, 1, 1, 1, '产品需求评审', '三层会议室A', '确认日历首页与创建日程流程', '2026-06-08 09:30:00+08', '2026-06-08 10:30:00+08', FALSE, 'DEFAULT'),
  (2, 6, 1, 6, '管理端字段确认', '线上会议', '确认用户、部门、日历和标签字段', '2026-06-08 10:00:00+08', '2026-06-08 11:00:00+08', FALSE, 'DEFAULT'),
  (3, 7, 1, 7, '接口联调安排', '研发区', '确认日程与附件接口联调顺序', '2026-06-08 14:00:00+08', '2026-06-08 15:30:00+08', FALSE, 'DEFAULT'),
  (4, 11, 5, 1, '公司制度培训', '大会议室', '全员参加的制度宣贯', '2026-06-09 09:00:00+08', '2026-06-09 11:30:00+08', FALSE, 'PUBLIC'),
  (5, 10, 4, 6, '机房巡检', '机房', '基础设施例行巡检', '2026-06-09 00:00:00+08', '2026-06-10 00:00:00+08', TRUE, 'PUBLIC'),
  (6, 9, 2, 7, '跨部门外出沟通', '客户现场', '产品与研发联合沟通', '2026-06-10 09:00:00+08', '2026-06-11 18:00:00+08', FALSE, 'DEFAULT'),
  (7, 3, 1, 3, '研发站会', '研发区', '每日同步进度与风险', '2026-06-10 09:30:00+08', '2026-06-10 09:45:00+08', FALSE, 'DEFAULT'),
  (8, 8, 1, 8, '质量用例评审', '质量部会议室', '确认关键业务路径覆盖', '2026-06-10 10:00:00+08', '2026-06-10 11:00:00+08', FALSE, 'DEFAULT'),
  (9, 2, 3, 2, '个人事务', '外出', '个人时间安排', '2026-06-10 15:00:00+08', '2026-06-10 16:00:00+08', FALSE, 'PRIVATE'),
  (10, 11, 1, 1, '月度经营会', '大会议室', '月度经营情况同步', '2026-06-12 14:00:00+08', '2026-06-12 16:00:00+08', FALSE, 'PUBLIC'),
  (11, 9, 1, 6, '版本范围确认', '三层会议室B', '确认本期交付范围', '2026-06-15 09:30:00+08', '2026-06-15 10:30:00+08', FALSE, 'DEFAULT'),
  (12, 4, 4, 4, '研发值班', '研发区', '线上问题跟进', '2026-06-15 18:00:00+08', '2026-06-16 09:00:00+08', FALSE, 'DEFAULT'),
  (13, 5, 5, 5, '质量规范学习', '质量部会议室', '质量流程学习', '2026-06-16 13:30:00+08', '2026-06-16 15:00:00+08', FALSE, 'DEFAULT'),
  (14, 1, 1, 1, '附件材料整理', '工位', '整理会议材料并上传', '2026-06-17 10:00:00+08', '2026-06-17 11:00:00+08', FALSE, 'DEFAULT'),
  (15, 6, 1, 6, '待办拆解会', '线上会议', '拆分日程详情页待办事项', '2026-06-17 14:00:00+08', '2026-06-17 15:00:00+08', FALSE, 'DEFAULT'),
  (16, 7, 1, 7, '忙闲冲突核对', '研发区', '核对同一时段参会冲突', '2026-06-17 14:30:00+08', '2026-06-17 15:30:00+08', FALSE, 'DEFAULT'),
  (17, 10, 5, 1, '安全规范宣贯', '线上会议', '附件与导出安全要求说明', '2026-06-18 10:00:00+08', '2026-06-18 11:00:00+08', FALSE, 'PUBLIC'),
  (18, 11, 1, 6, '全员日历维护例会', '三层会议室A', '维护全员日历共享成员和范围', '2026-06-19 09:00:00+08', '2026-06-19 09:30:00+08', FALSE, 'PUBLIC'),
  (19, 3, 1, 3, '重复规则核对', '研发区', '核对每周重复日程展开', '2026-06-22 09:30:00+08', '2026-06-22 10:00:00+08', FALSE, 'DEFAULT'),
  (20, 8, 1, 8, '回执状态核查', '质量部会议室', '核查接受、拒绝、待定和未响应状态', '2026-06-23 16:00:00+08', '2026-06-23 17:00:00+08', FALSE, 'DEFAULT');

INSERT INTO event_participant (id, event_id, user_id, department_id, response_status, response_at) VALUES
  (1, 1, 6, NULL, 'ACCEPTED', '2026-06-08 09:00:00+08'),
  (2, 1, 7, NULL, 'TENTATIVE', '2026-06-08 09:05:00+08'),
  (3, 1, 8, NULL, 'NEEDS_ACTION', NULL),
  (4, 2, 1, NULL, 'ACCEPTED', '2026-06-08 09:30:00+08'),
  (5, 2, 7, NULL, 'DECLINED', '2026-06-08 09:35:00+08'),
  (6, 3, 3, NULL, 'ACCEPTED', '2026-06-08 13:00:00+08'),
  (7, 3, 4, NULL, 'ACCEPTED', '2026-06-08 13:10:00+08'),
  (8, 4, NULL, 1, 'NEEDS_ACTION', NULL),
  (9, 4, NULL, 2, 'NEEDS_ACTION', NULL),
  (10, 4, NULL, 3, 'NEEDS_ACTION', NULL),
  (11, 6, 6, NULL, 'ACCEPTED', '2026-06-09 18:00:00+08'),
  (12, 6, 7, NULL, 'ACCEPTED', '2026-06-09 18:05:00+08'),
  (13, 8, 6, NULL, 'ACCEPTED', '2026-06-10 09:00:00+08'),
  (14, 8, 7, NULL, 'NEEDS_ACTION', NULL),
  (15, 10, NULL, 1, 'NEEDS_ACTION', NULL),
  (16, 10, NULL, 2, 'NEEDS_ACTION', NULL),
  (17, 10, NULL, 3, 'NEEDS_ACTION', NULL),
  (18, 15, 7, NULL, 'ACCEPTED', '2026-06-17 13:00:00+08'),
  (19, 16, 6, NULL, 'TENTATIVE', '2026-06-17 13:15:00+08'),
  (20, 20, 1, NULL, 'ACCEPTED', '2026-06-23 15:30:00+08'),
  (21, 20, 6, NULL, 'DECLINED', '2026-06-23 15:35:00+08'),
  (22, 20, 7, NULL, 'TENTATIVE', '2026-06-23 15:40:00+08'),
  (23, 20, 8, NULL, 'NEEDS_ACTION', NULL);

INSERT INTO event_reminder (id, event_id, minutes_before, method) VALUES
  (1, 1, 15, 'SYSTEM'),
  (2, 4, 60, 'SYSTEM'),
  (3, 6, 1440, 'SYSTEM'),
  (4, 10, 30, 'SYSTEM'),
  (5, 18, 15, 'SYSTEM');

INSERT INTO event_recurrence (id, event_id, rrule, end_at, occurrence_count) VALUES
  (1, 7, 'FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR', '2026-07-31 09:30:00+08', NULL),
  (2, 10, 'FREQ=MONTHLY;BYMONTHDAY=12', NULL, 6),
  (3, 19, 'FREQ=WEEKLY;BYDAY=MO', '2026-08-31 09:30:00+08', NULL);

INSERT INTO event_exception (id, recurrence_id, original_start_at, exception_type, new_start_at, new_end_at, reason) VALUES
  (1, 1, '2026-06-11 09:30:00+08', 'CANCELLED', NULL, NULL, '当天合并到专项沟通'),
  (2, 3, '2026-06-29 09:30:00+08', 'MOVED', '2026-06-29 10:30:00+08', '2026-06-29 11:00:00+08', '会议室调整');

INSERT INTO event_attachment (id, event_id, uploaded_by, file_name, file_path, file_size, content_type) VALUES
  (1, 1, 6, '产品需求评审材料.pdf', '/app/uploads/2026/06/product-review.pdf', 245760, 'application/pdf'),
  (2, 6, 7, '外出沟通纪要.docx', '/app/uploads/2026/06/field-communication.docx', 98304, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
  (3, 14, 1, '附件材料清单.xlsx', '/app/uploads/2026/06/attachment-list.xlsx', 32768, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');

INSERT INTO event_todo (id, event_id, assignee_user_id, title, priority, completed, due_at) VALUES
  (1, 1, 6, '整理评审结论', 'HIGH', FALSE, '2026-06-08 18:00:00+08'),
  (2, 1, 7, '确认接口字段影响', 'MEDIUM', TRUE, '2026-06-09 12:00:00+08'),
  (3, 15, 1, '补充待办交互说明', 'MEDIUM', FALSE, '2026-06-18 12:00:00+08'),
  (4, 15, 8, '核对待办状态展示', 'LOW', FALSE, '2026-06-18 18:00:00+08'),
  (5, 20, 8, '整理回执核查记录', 'HIGH', FALSE, '2026-06-24 12:00:00+08');

INSERT INTO export_task (id, name, scope, status, created_by, file_path, started_at, finished_at) VALUES
  (1, '六月日程导出', 'MANAGEMENT', 'FINISHED', 1, '/app/backup/exports/events-202606.xlsx', '2026-06-05 10:00:00+08', '2026-06-05 10:01:00+08'),
  (2, '个人日历导出', 'PERSONAL', 'PENDING', 6, NULL, NULL, NULL);

INSERT INTO backup_record (id, backup_type, action, status, file_path, file_size, operator_user_id, verification_result, created_at, finished_at) VALUES
  (1, 'FULL', 'BACKUP', 'FINISHED', '/app/backup/full/full-20260605.tar.gz', 10485760, 1, '备份文件已生成，核心数据条目完整。', '2026-06-05 10:10:00+08', '2026-06-05 10:12:00+08'),
  (2, 'FULL', 'RESTORE_RECORD', 'FINISHED', NULL, NULL, 1, '用户、日历、日程、附件、待办和审计日志核验通过。', '2026-06-05 10:30:00+08', '2026-06-05 10:45:00+08');

INSERT INTO audit_log (id, operator_user_id, module, action, object_type, object_id, detail, created_at) VALUES
  (1, 1, '系统用户', '新增', 'sys_user', 1, '初始化系统用户', '2026-06-05 09:00:00+08'),
  (2, 1, '部门', '新增', 'sys_department', 1, '初始化部门数据', '2026-06-05 09:01:00+08'),
  (3, 1, '日历', '新增', 'calendar', 11, '初始化公司全员日历', '2026-06-05 09:02:00+08'),
  (4, 1, '全员日历', '共享成员变更', 'calendar_shared_member', 1, '配置全员日历共享成员', '2026-06-05 09:03:00+08'),
  (5, 6, '日程', '新增', 'event', 1, '创建产品需求评审日程', '2026-06-08 09:00:00+08'),
  (6, 7, '附件', '上传', 'event_attachment', 2, '上传外出沟通纪要', '2026-06-09 18:10:00+08'),
  (7, 1, '导出', '创建导出任务', 'export_task', 1, '生成六月日程导出文件', '2026-06-05 10:00:00+08'),
  (8, 1, '备份恢复', '备份', 'backup_record', 1, '执行完整备份', '2026-06-05 10:10:00+08'),
  (9, 1, '备份恢复', '恢复登记', 'backup_record', 2, '登记恢复核验结果', '2026-06-05 10:45:00+08'),
  (10, 1, '系统配置', '配置变更', 'system_config', 1, '设置附件上传限制', '2026-06-05 11:00:00+08');

INSERT INTO system_config (id, config_key, config_value, description, updated_by, updated_at) VALUES
  (1, 'attachment.max_size_mb', '20', '单个附件最大容量', 1, '2026-06-05 11:00:00+08'),
  (2, 'attachment.allowed_types', 'pdf,docx,xlsx,png,jpg', '允许上传的附件类型', 1, '2026-06-05 11:00:00+08'),
  (3, 'export.max_records', '5000', '单次导出最大记录数', 1, '2026-06-05 11:00:00+08'),
  (4, 'calendar.show_lunar', 'true', '日历展示农历信息', 1, '2026-06-05 11:00:00+08'),
  (5, 'calendar.show_holiday', 'true', '日历展示节假日信息', 1, '2026-06-05 11:00:00+08'),
  (6, 'backup.retention_days', '30', '备份文件保留天数', 1, '2026-06-05 11:00:00+08');

COMMIT;
