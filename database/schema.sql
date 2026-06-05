CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS sys_department (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  parent_id BIGINT,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  department_id BIGINT REFERENCES sys_department(id),
  email VARCHAR(160),
  phone VARCHAR(40),
  avatar_color VARCHAR(20) NOT NULL DEFAULT '#2563eb',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS calendar (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  type VARCHAR(30) NOT NULL,
  color VARCHAR(20) NOT NULL,
  owner_user_id BIGINT REFERENCES sys_user(id),
  visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS calendar_auto_subscribe_scope (
  id BIGSERIAL PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  scope_type VARCHAR(30) NOT NULL,
  department_id BIGINT REFERENCES sys_department(id),
  user_id BIGINT REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS calendar_shared_member (
  id BIGSERIAL PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(calendar_id, user_id)
);

CREATE TABLE IF NOT EXISTS calendar_subscription (
  id BIGSERIAL PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES sys_user(id),
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(calendar_id, user_id)
);

CREATE TABLE IF NOT EXISTS event (
  id BIGSERIAL PRIMARY KEY,
  calendar_id BIGINT NOT NULL REFERENCES calendar(id),
  organizer_user_id BIGINT NOT NULL REFERENCES sys_user(id),
  title VARCHAR(100) NOT NULL,
  location VARCHAR(300),
  description TEXT,
  tag VARCHAR(40),
  tag_color VARCHAR(20),
  start_time TIMESTAMPTZ NOT NULL,
  end_time TIMESTAMPTZ NOT NULL,
  all_day BOOLEAN NOT NULL DEFAULT FALSE,
  recurrence_rule VARCHAR(300),
  recurrence_end TIMESTAMPTZ,
  allow_join BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS event_participant (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES sys_user(id),
  response_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(event_id, user_id)
);

CREATE TABLE IF NOT EXISTS event_reminder (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  minutes_before INT NOT NULL
);

CREATE TABLE IF NOT EXISTS event_recurrence (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  rrule VARCHAR(300) NOT NULL,
  until_time TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS event_exception (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  instance_start TIMESTAMPTZ NOT NULL,
  action VARCHAR(20) NOT NULL,
  override_payload JSONB
);

CREATE TABLE IF NOT EXISTS event_attachment (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT REFERENCES event(id) ON DELETE SET NULL,
  file_name VARCHAR(240) NOT NULL,
  stored_name VARCHAR(240) NOT NULL,
  file_size BIGINT NOT NULL,
  content_type VARCHAR(120),
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by BIGINT REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS event_todo (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  title VARCHAR(160) NOT NULL,
  assignee_user_id BIGINT REFERENCES sys_user(id),
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS export_task (
  id BIGSERIAL PRIMARY KEY,
  task_name VARCHAR(160) NOT NULL,
  export_scope VARCHAR(60) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  file_path VARCHAR(500),
  failure_reason VARCHAR(500),
  created_by BIGINT REFERENCES sys_user(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  operator_user_id BIGINT REFERENCES sys_user(id),
  module VARCHAR(60) NOT NULL,
  action VARCHAR(60) NOT NULL,
  object_type VARCHAR(60) NOT NULL,
  object_id VARCHAR(80),
  change_summary TEXT,
  source VARCHAR(40) NOT NULL DEFAULT 'WEB',
  result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  failure_reason VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS backup_record (
  id BIGSERIAL PRIMARY KEY,
  backup_type VARCHAR(40) NOT NULL,
  file_path VARCHAR(500),
  file_size BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  operated_by BIGINT REFERENCES sys_user(id),
  restore_started_at TIMESTAMPTZ,
  restore_finished_at TIMESTAMPTZ,
  restore_result VARCHAR(40),
  verified_by VARCHAR(80),
  conclusion VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS system_config (
  config_key VARCHAR(80) PRIMARY KEY,
  config_value TEXT NOT NULL,
  description VARCHAR(300),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tag_color (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(40) NOT NULL,
  color VARCHAR(20) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX IF NOT EXISTS idx_event_time ON event(start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_event_calendar ON event(calendar_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON audit_log(created_at DESC);
