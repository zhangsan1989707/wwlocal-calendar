BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS backup_record;
DROP TABLE IF EXISTS export_task;
DROP TABLE IF EXISTS event_todo;
DROP TABLE IF EXISTS event_attachment;
DROP TABLE IF EXISTS event_exception;
DROP TABLE IF EXISTS event_recurrence;
DROP TABLE IF EXISTS event_reminder;
DROP TABLE IF EXISTS event_participant;
DROP TABLE IF EXISTS external_contact;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS calendar_subscription;
DROP TABLE IF EXISTS calendar_shared_member;
DROP TABLE IF EXISTS calendar_auto_subscribe_scope;
DROP TABLE IF EXISTS calendars;
DROP TABLE IF EXISTS calendar_tag;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;

-- 生成 UUID 的函数
CREATE OR REPLACE FUNCTION gen_uuid() RETURNS VARCHAR(36) AS $$
BEGIN
    RETURN lower(regexp_replace(gen_random_uuid()::text, '-', '', 'g'));
END;
$$ LANGUAGE plpgsql;

CREATE TABLE departments (
  id VARCHAR(36) PRIMARY KEY DEFAULT gen_uuid(),
  parent_id VARCHAR(36) REFERENCES departments(id),
  name VARCHAR(50) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY DEFAULT gen_uuid(),
  department_id VARCHAR(36) REFERENCES departments(id),
  name VARCHAR(50) NOT NULL,
  email VARCHAR(120),
  mobile VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 用户表（用于JWT认证）
CREATE TABLE app_user (
  id              VARCHAR(36) PRIMARY KEY REFERENCES users(id),
  username        VARCHAR(50) UNIQUE NOT NULL,
  password_hash   VARCHAR(200) NOT NULL,
  display_name    VARCHAR(100),
  role            VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
  enabled         BOOLEAN NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE calendar_tag (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(30) NOT NULL UNIQUE,
  color VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendars (
  id VARCHAR(36) PRIMARY KEY DEFAULT gen_uuid(),
  owner_user_id VARCHAR(36) REFERENCES users(id),
  name VARCHAR(80) NOT NULL,
  description TEXT,
  type VARCHAR(20) NOT NULL CHECK (type IN ('PERSONAL', 'SHARED', 'PUBLIC', 'ALL_MEMBER')),
  color VARCHAR(20) NOT NULL,
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_auto_subscribe_scope (
  id BIGSERIAL PRIMARY KEY,
  calendar_id VARCHAR(36) NOT NULL REFERENCES calendars(id) ON DELETE CASCADE,
  scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('COMPANY', 'DEPARTMENT', 'USER')),
  department_id VARCHAR(36) REFERENCES departments(id),
  user_id VARCHAR(36) REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (
    (scope_type = 'COMPANY' AND department_id IS NULL AND user_id IS NULL)
    OR (scope_type = 'DEPARTMENT' AND department_id IS NOT NULL AND user_id IS NULL)
    OR (scope_type = 'USER' AND department_id IS NULL AND user_id IS NOT NULL)
  )
);

CREATE TABLE calendar_shared_member (
  id BIGSERIAL PRIMARY KEY,
  calendar_id VARCHAR(36) NOT NULL REFERENCES calendars(id) ON DELETE CASCADE,
  user_id VARCHAR(36) NOT NULL REFERENCES users(id),
  role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'EDITOR')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (calendar_id, user_id)
);

CREATE TABLE calendar_subscription (
  id BIGSERIAL PRIMARY KEY,
  calendar_id VARCHAR(36) NOT NULL REFERENCES calendars(id) ON DELETE CASCADE,
  user_id VARCHAR(36) NOT NULL REFERENCES users(id),
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (calendar_id, user_id)
);

CREATE TABLE event (
  id BIGSERIAL PRIMARY KEY,
  calendar_id VARCHAR(36) NOT NULL REFERENCES calendars(id),
  tag_id BIGINT REFERENCES calendar_tag(id),
  organizer_user_id VARCHAR(36) NOT NULL REFERENCES users(id),
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

CREATE TABLE external_contact (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(200),
  company VARCHAR(200),
  contact_type VARCHAR(20) DEFAULT 'wechat' CHECK (contact_type IN ('wechat', 'client', 'partner')),
  phone VARCHAR(30),
  created_by VARCHAR(36) REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_participant (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  user_id VARCHAR(36) REFERENCES users(id),
  department_id VARCHAR(36) REFERENCES departments(id),
  external_contact_id BIGINT REFERENCES external_contact(id) ON DELETE SET NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'VIEWER' CHECK (role IN ('VIEWER', 'EDITOR')),
  response_status VARCHAR(20) NOT NULL DEFAULT 'NEEDS_ACTION'
    CHECK (response_status IN ('ACCEPTED', 'DECLINED', 'TENTATIVE', 'NEEDS_ACTION')),
  response_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (
    (user_id IS NOT NULL AND department_id IS NULL AND external_contact_id IS NULL)
    OR (user_id IS NULL AND department_id IS NOT NULL AND external_contact_id IS NULL)
    OR (user_id IS NULL AND department_id IS NULL AND external_contact_id IS NOT NULL)
  ),
  UNIQUE (event_id, user_id)
);

CREATE TABLE event_reminder (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  minutes_before INTEGER NOT NULL CHECK (minutes_before >= 0),
  method VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' CHECK (method IN ('SYSTEM', 'EMAIL')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_recurrence (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL UNIQUE REFERENCES event(id) ON DELETE CASCADE,
  rrule TEXT NOT NULL,
  end_at TIMESTAMPTZ,
  occurrence_count INTEGER CHECK (occurrence_count IS NULL OR occurrence_count > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_exception (
  id BIGSERIAL PRIMARY KEY,
  recurrence_id BIGINT NOT NULL REFERENCES event_recurrence(id) ON DELETE CASCADE,
  original_start_at TIMESTAMPTZ NOT NULL,
  exception_type VARCHAR(20) NOT NULL CHECK (exception_type IN ('CANCELLED', 'MOVED')),
  new_start_at TIMESTAMPTZ,
  new_end_at TIMESTAMPTZ,
  modified_data JSONB,
  reminder_override JSONB,
  reason VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (
    (exception_type = 'CANCELLED' AND new_start_at IS NULL AND new_end_at IS NULL)
    OR (exception_type = 'MOVED' AND new_start_at IS NOT NULL AND new_end_at IS NOT NULL AND new_end_at > new_start_at)
  )
);

CREATE TABLE event_attachment (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  uploaded_by VARCHAR(36) NOT NULL REFERENCES users(id),
  file_name VARCHAR(200) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  content_type VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_todo (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  assignee_user_id VARCHAR(36) REFERENCES users(id),
  title VARCHAR(120) NOT NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  due_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE export_task (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  scope VARCHAR(20) NOT NULL CHECK (scope IN ('PERSONAL', 'CALENDAR', 'MANAGEMENT')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'FINISHED', 'FAILED')),
  created_by VARCHAR(36) NOT NULL REFERENCES users(id),
  file_path VARCHAR(500),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE backup_record (
  id BIGSERIAL PRIMARY KEY,
  backup_type VARCHAR(20) NOT NULL CHECK (backup_type IN ('DATABASE', 'UPLOADS', 'FULL')),
  action VARCHAR(20) NOT NULL CHECK (action IN ('BACKUP', 'RESTORE_RECORD')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'FINISHED', 'FAILED')),
  file_path VARCHAR(500),
  file_size BIGINT CHECK (file_size IS NULL OR file_size >= 0),
  operator_user_id VARCHAR(36) NOT NULL REFERENCES users(id),
  verification_result TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ
);

CREATE TABLE notification (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_id BIGINT REFERENCES event(id) ON DELETE CASCADE,
  title VARCHAR(300) NOT NULL,
  message TEXT,
  notification_type VARCHAR(50) DEFAULT 'event_update',
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  operator_user_id VARCHAR(36) REFERENCES users(id),
  module VARCHAR(60) NOT NULL,
  action VARCHAR(60) NOT NULL,
  object_type VARCHAR(60) NOT NULL,
  object_id VARCHAR(64),
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE system_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(80) NOT NULL UNIQUE,
  config_value TEXT NOT NULL,
  description VARCHAR(200),
  updated_by VARCHAR(36) REFERENCES users(id),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_calendars_type_visible ON calendars(type, visible);
CREATE INDEX idx_calendar_subscription_user ON calendar_subscription(user_id, visible);
CREATE INDEX idx_event_time_range ON event(start_at, end_at);
CREATE INDEX idx_event_calendar_time ON event(calendar_id, start_at, end_at);
CREATE INDEX idx_event_participant_user ON event_participant(user_id, response_status);
CREATE INDEX idx_event_attachment_event ON event_attachment(event_id);
CREATE INDEX idx_notification_user ON notification(user_id, is_read, created_at DESC);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

-- 预设部门数据
INSERT INTO departments (id, name, sort_order) VALUES
  ('dept-001', '产品部', 1),
  ('dept-002', '研发部', 2),
  ('dept-003', '质量部', 3);

-- 预设用户数据
INSERT INTO users (id, department_id, name, email, mobile, status) VALUES
  ('user-001', 'dept-001', '李宇航', 'liyuhang@example.local', '13800000001', 'active'),
  ('user-002', 'dept-001', '张三', 'zhangsan@example.local', '13800000002', 'active'),
  ('user-003', 'dept-002', '李四', 'lisi@example.local', '13800000003', 'active'),
  ('user-004', 'dept-002', '王五', 'wangwu@example.local', '13800000004', 'active'),
  ('user-005', 'dept-003', '赵六', 'zhaoliu@example.local', '13800000005', 'active'),
  ('user-006', 'dept-001', '产品经理', 'pm@example.local', '13800000006', 'active'),
  ('user-007', 'dept-002', '研发负责人', 'rdlead@example.local', '13800000007', 'active'),
  ('user-008', 'dept-003', '质量人员', 'qa@example.local', '13800000008', 'active');

-- 预设应用用户数据（用于JWT认证）
-- 密码都是：admin123
INSERT INTO app_user(id, username, password_hash, display_name, role) VALUES
('user-001', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '李宇航', 'admin'),
('user-002', 'zhangsan', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '张三', 'user'),
('user-003', 'lisi', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '李四', 'user');

-- 预设日历标签
INSERT INTO calendar_tag (name, color) VALUES
  ('会议', '#3B82F6'),
  ('出差', '#F97316'),
  ('私事', '#A855F7'),
  ('值班', '#10B981'),
  ('培训', '#EF4444');

-- 预设日历数据
INSERT INTO calendars (id, owner_user_id, name, description, type, color, visible) VALUES
  ('cal-001', 'user-001', '李宇航个人日历', '个人工作安排', 'PERSONAL', '#2563EB', true),
  ('cal-002', 'user-002', '张三个人日历', '个人工作安排', 'PERSONAL', '#16A34A', true),
  ('cal-003', 'user-003', '李四个人日历', '个人工作安排', 'PERSONAL', '#9333EA', true),
  ('cal-004', 'user-004', '王五个人日历', '个人工作安排', 'PERSONAL', '#DC2626', true),
  ('cal-005', 'user-005', '赵六个人日历', '个人工作安排', 'PERSONAL', '#0891B2', true),
  ('cal-006', 'user-006', '产品经理个人日历', '个人工作安排', 'PERSONAL', '#F59E0B', true),
  ('cal-007', 'user-007', '研发负责人个人日历', '个人工作安排', 'PERSONAL', '#4F46E5', true),
  ('cal-008', 'user-008', '质量人员个人日历', '个人工作安排', 'PERSONAL', '#059669', true),
  ('cal-009', NULL, '跨部门共享日历', '跨部门协作安排', 'SHARED', '#0EA5E9', true),
  ('cal-010', NULL, '公共日历', '公开事项与公共安排', 'PUBLIC', '#64748B', true),
  ('cal-011', NULL, '公司全员日历', '公司级统一安排', 'ALL_MEMBER', '#DB2777', true);

-- 全员日历自动订阅范围
INSERT INTO calendar_auto_subscribe_scope (calendar_id, scope_type) VALUES
  ('cal-011', 'COMPANY');

-- 日历共享成员
INSERT INTO calendar_shared_member (calendar_id, user_id, role) VALUES
  ('cal-011', 'user-001', 'MANAGER'),
  ('cal-011', 'user-006', 'MANAGER'),
  ('cal-009', 'user-006', 'EDITOR'),
  ('cal-009', 'user-007', 'EDITOR');

-- 日历订阅
INSERT INTO calendar_subscription (calendar_id, user_id, visible, sort_order) VALUES
  ('cal-001', 'user-001', true, 1),
  ('cal-002', 'user-002', true, 1),
  ('cal-003', 'user-003', true, 1),
  ('cal-004', 'user-004', true, 1),
  ('cal-005', 'user-005', true, 1),
  ('cal-006', 'user-006', true, 1),
  ('cal-007', 'user-007', true, 1),
  ('cal-008', 'user-008', true, 1),
  ('cal-009', 'user-001', true, 2),
  ('cal-009', 'user-006', true, 2),
  ('cal-009', 'user-007', true, 2),
  ('cal-010', 'user-001', true, 3),
  ('cal-010', 'user-002', true, 3),
  ('cal-010', 'user-003', true, 3),
  ('cal-010', 'user-004', true, 3),
  ('cal-010', 'user-005', true, 3),
  ('cal-010', 'user-006', true, 3),
  ('cal-010', 'user-007', true, 3),
  ('cal-010', 'user-008', true, 3),
  ('cal-011', 'user-001', true, 4),
  ('cal-011', 'user-002', true, 4),
  ('cal-011', 'user-003', true, 4),
  ('cal-011', 'user-004', true, 4),
  ('cal-011', 'user-005', true, 4),
  ('cal-011', 'user-006', true, 4),
  ('cal-011', 'user-007', true, 4),
  ('cal-011', 'user-008', true, 4);

-- 预设日程数据
INSERT INTO event (calendar_id, tag_id, organizer_user_id, title, location, description, start_at, end_at, all_day, visibility) VALUES
  ('cal-001', 1, 'user-001', '产品需求评审', '三层会议室A', '确认日历首页与创建日程流程', '2026-06-08 09:30:00+08', '2026-06-08 10:30:00+08', false, 'DEFAULT'),
  ('cal-006', 1, 'user-006', '管理端字段确认', '线上会议', '确认用户、部门、日历和标签字段', '2026-06-08 10:00:00+08', '2026-06-08 11:00:00+08', false, 'DEFAULT'),
  ('cal-007', 1, 'user-007', '接口联调安排', '研发区', '确认日程与附件接口联调顺序', '2026-06-08 14:00:00+08', '2026-06-08 15:30:00+08', false, 'DEFAULT'),
  ('cal-011', 5, 'user-001', '公司制度培训', '大会议室', '全员参加的制度宣贯', '2026-06-09 09:00:00+08', '2026-06-09 11:30:00+08', false, 'PUBLIC'),
  ('cal-010', 4, 'user-006', '机房巡检', '机房', '基础设施例行巡检', '2026-06-09 00:00:00+08', '2026-06-10 00:00:00+08', true, 'PUBLIC'),
  ('cal-009', 2, 'user-007', '跨部门外出沟通', '客户现场', '产品与研发联合沟通', '2026-06-10 09:00:00+08', '2026-06-11 18:00:00+08', false, 'DEFAULT'),
  ('cal-003', 1, 'user-003', '研发站会', '研发区', '每日同步进度与风险', '2026-06-10 09:30:00+08', '2026-06-10 09:45:00+08', false, 'DEFAULT'),
  ('cal-008', 1, 'user-008', '质量用例评审', '质量部会议室', '确认关键业务路径覆盖', '2026-06-10 10:00:00+08', '2026-06-10 11:00:00+08', false, 'DEFAULT'),
  ('cal-002', 3, 'user-002', '个人事务', '外出', '个人时间安排', '2026-06-10 15:00:00+08', '2026-06-10 16:00:00+08', false, 'PRIVATE'),
  ('cal-011', 1, 'user-001', '月度经营会', '大会议室', '月度经营情况同步', '2026-06-12 14:00:00+08', '2026-06-12 16:00:00+08', false, 'PUBLIC'),
  ('cal-009', 1, 'user-006', '版本范围确认', '三层会议室B', '确认本期交付范围', '2026-06-15 09:30:00+08', '2026-06-15 10:30:00+08', false, 'DEFAULT'),
  ('cal-004', 4, 'user-004', '研发值班', '研发区', '线上问题跟进', '2026-06-15 18:00:00+08', '2026-06-16 09:00:00+08', false, 'DEFAULT'),
  ('cal-005', 5, 'user-005', '质量规范学习', '质量部会议室', '质量流程学习', '2026-06-16 13:30:00+08', '2026-06-16 15:00:00+08', false, 'DEFAULT'),
  ('cal-001', 1, 'user-001', '附件材料整理', '工位', '整理会议材料并上传', '2026-06-17 10:00:00+08', '2026-06-17 11:00:00+08', false, 'DEFAULT'),
  ('cal-006', 1, 'user-006', '待办拆解会', '线上会议', '拆分日程详情页待办事项', '2026-06-17 14:00:00+08', '2026-06-17 15:00:00+08', false, 'DEFAULT'),
  ('cal-007', 1, 'user-007', '忙闲冲突核对', '研发区', '核对同一时段参会冲突', '2026-06-17 14:30:00+08', '2026-06-17 15:30:00+08', false, 'DEFAULT'),
  ('cal-010', 5, 'user-001', '安全规范宣贯', '线上会议', '附件与导出安全要求说明', '2026-06-18 10:00:00+08', '2026-06-18 11:00:00+08', false, 'PUBLIC'),
  ('cal-011', 1, 'user-006', '全员日历维护例会', '三层会议室A', '维护全员日历共享成员和范围', '2026-06-19 09:00:00+08', '2026-06-19 09:30:00+08', false, 'PUBLIC'),
  ('cal-003', 1, 'user-003', '重复规则核对', '研发区', '核对每周重复日程展开', '2026-06-22 09:30:00+08', '2026-06-22 10:00:00+08', false, 'DEFAULT'),
  ('cal-008', 1, 'user-008', '回执状态核查', '质量部会议室', '核查接受、拒绝、待定和未响应状态', '2026-06-23 16:00:00+08', '2026-06-23 17:00:00+08', false, 'DEFAULT');

-- 日程参与者
INSERT INTO event_participant (event_id, user_id, department_id, response_status, response_at) VALUES
  (1, 'user-006', NULL, 'ACCEPTED', '2026-06-08 09:00:00+08'),
  (1, 'user-007', NULL, 'TENTATIVE', '2026-06-08 09:05:00+08'),
  (1, 'user-008', NULL, 'NEEDS_ACTION', NULL),
  (2, 'user-001', NULL, 'ACCEPTED', '2026-06-08 09:30:00+08'),
  (2, 'user-007', NULL, 'DECLINED', '2026-06-08 09:35:00+08'),
  (3, 'user-003', NULL, 'ACCEPTED', '2026-06-08 13:00:00+08'),
  (3, 'user-004', NULL, 'ACCEPTED', '2026-06-08 13:10:00+08'),
  (4, NULL, 'dept-001', 'NEEDS_ACTION', NULL),
  (4, NULL, 'dept-002', 'NEEDS_ACTION', NULL),
  (4, NULL, 'dept-003', 'NEEDS_ACTION', NULL),
  (6, 'user-006', NULL, 'ACCEPTED', '2026-06-09 18:00:00+08'),
  (6, 'user-007', NULL, 'ACCEPTED', '2026-06-09 18:05:00+08'),
  (8, 'user-006', NULL, 'ACCEPTED', '2026-06-10 09:00:00+08'),
  (8, 'user-007', NULL, 'NEEDS_ACTION', NULL),
  (10, NULL, 'dept-001', 'NEEDS_ACTION', NULL),
  (10, NULL, 'dept-002', 'NEEDS_ACTION', NULL),
  (10, NULL, 'dept-003', 'NEEDS_ACTION', NULL),
  (15, 'user-007', NULL, 'ACCEPTED', '2026-06-17 13:00:00+08'),
  (16, 'user-006', NULL, 'TENTATIVE', '2026-06-17 13:15:00+08'),
  (20, 'user-001', NULL, 'ACCEPTED', '2026-06-23 15:30:00+08'),
  (20, 'user-006', NULL, 'DECLINED', '2026-06-23 15:35:00+08'),
  (20, 'user-007', NULL, 'TENTATIVE', '2026-06-23 15:40:00+08'),
  (20, 'user-008', NULL, 'NEEDS_ACTION', NULL);

-- 日程提醒
INSERT INTO event_reminder (event_id, minutes_before, method) VALUES
  (1, 15, 'SYSTEM'),
  (4, 60, 'SYSTEM'),
  (6, 1440, 'SYSTEM'),
  (10, 30, 'SYSTEM'),
  (18, 15, 'SYSTEM');

-- 日程重复规则
INSERT INTO event_recurrence (event_id, rrule, end_at, occurrence_count) VALUES
  (7, 'FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR', '2026-07-31 09:30:00+08', NULL),
  (10, 'FREQ=MONTHLY;BYMONTHDAY=12', NULL, 6),
  (19, 'FREQ=WEEKLY;BYDAY=MO', '2026-08-31 09:30:00+08', NULL);

-- 日程异常
INSERT INTO event_exception (recurrence_id, original_start_at, exception_type, new_start_at, new_end_at, reason) VALUES
  (1, '2026-06-11 09:30:00+08', 'CANCELLED', NULL, NULL, '当天合并到专项沟通'),
  (3, '2026-06-29 09:30:00+08', 'MOVED', '2026-06-29 10:30:00+08', '2026-06-29 11:00:00+08', '会议室调整');

-- 日程附件
INSERT INTO event_attachment (event_id, uploaded_by, file_name, file_path, file_size, content_type) VALUES
  (1, 'user-006', '产品需求评审材料.pdf', '/app/uploads/2026/06/product-review.pdf', 245760, 'application/pdf'),
  (6, 'user-007', '外出沟通纪要.docx', '/app/uploads/2026/06/field-communication.docx', 98304, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
  (14, 'user-001', '附件材料清单.xlsx', '/app/uploads/2026/06/attachment-list.xlsx', 32768, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');

-- 日程待办
INSERT INTO event_todo (event_id, assignee_user_id, title, priority, completed, due_at) VALUES
  (1, 'user-006', '整理评审结论', 'HIGH', false, '2026-06-08 18:00:00+08'),
  (1, 'user-007', '确认接口字段影响', 'MEDIUM', true, '2026-06-09 12:00:00+08'),
  (15, 'user-001', '补充待办交互说明', 'MEDIUM', false, '2026-06-18 12:00:00+08'),
  (15, 'user-008', '核对待办状态展示', 'LOW', false, '2026-06-18 18:00:00+08'),
  (20, 'user-008', '整理回执核查记录', 'HIGH', false, '2026-06-24 12:00:00+08');

-- 导出任务
INSERT INTO export_task (name, scope, status, created_by, file_path, started_at, finished_at) VALUES
  ('六月日程导出', 'MANAGEMENT', 'FINISHED', 'user-001', '/app/backup/exports/events-202606.xlsx', '2026-06-05 10:00:00+08', '2026-06-05 10:01:00+08'),
  ('个人日历导出', 'PERSONAL', 'PENDING', 'user-006', NULL, NULL, NULL);

-- 备份记录
INSERT INTO backup_record (backup_type, action, status, file_path, file_size, operator_user_id, verification_result, created_at, finished_at) VALUES
  ('FULL', 'BACKUP', 'FINISHED', '/app/backup/full/full-20260605.tar.gz', 10485760, 'user-001', '备份文件已生成，核心数据条目完整。', '2026-06-05 10:10:00+08', '2026-06-05 10:12:00+08'),
  ('FULL', 'RESTORE_RECORD', 'FINISHED', NULL, NULL, 'user-001', '用户、日历、日程、附件、待办和审计日志核验通过。', '2026-06-05 10:30:00+08', '2026-06-05 10:45:00+08');

-- 审计日志
INSERT INTO audit_log (operator_user_id, module, action, object_type, object_id, detail, created_at) VALUES
  ('user-001', '系统用户', '新增', 'users', 1, '初始化系统用户', '2026-06-05 09:00:00+08'),
  ('user-001', '部门', '新增', 'departments', 1, '初始化部门数据', '2026-06-05 09:01:00+08'),
  ('user-001', '日历', '新增', 'calendars', 11, '初始化公司全员日历', '2026-06-05 09:02:00+08'),
  ('user-001', '全员日历', '共享成员变更', 'calendar_shared_member', 1, '配置全员日历共享成员', '2026-06-05 09:03:00+08'),
  ('user-006', '日程', '新增', 'event', 1, '创建产品需求评审日程', '2026-06-08 09:00:00+08'),
  ('user-007', '附件', '上传', 'event_attachment', 2, '上传外出沟通纪要', '2026-06-09 18:10:00+08'),
  ('user-001', '导出', '创建导出任务', 'export_task', 1, '生成六月日程导出文件', '2026-06-05 10:00:00+08'),
  ('user-001', '备份恢复', '备份', 'backup_record', 1, '执行完整备份', '2026-06-05 10:10:00+08'),
  ('user-001', '备份恢复', '恢复登记', 'backup_record', 2, '登记恢复核验结果', '2026-06-05 10:45:00+08'),
  ('user-001', '系统配置', '配置变更', 'system_config', 1, '设置附件上传限制', '2026-06-05 11:00:00+08');

-- 系统配置
INSERT INTO system_config (config_key, config_value, description, updated_by) VALUES
  ('attachment.max_size_mb', '20', '单个附件最大容量', 'user-001'),
  ('attachment.allowed_types', 'pdf,docx,xlsx,png,jpg', '允许上传的附件类型', 'user-001'),
  ('export.max_records', '5000', '单次导出最大记录数', 'user-001'),
  ('calendar.show_lunar', 'true', '日历展示农历信息', 'user-001'),
  ('calendar.show_holiday', 'true', '日历展示节假日信息', 'user-001'),
  ('backup.retention_days', '30', '备份文件保留天数', 'user-001');

COMMIT;
