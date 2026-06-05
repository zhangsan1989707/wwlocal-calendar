-- Migration: rename sys_department→departments, sys_user→users, calendar→calendars
-- Also fix column names and types to match init.sql

BEGIN;

-- ========== 1. Drop foreign keys referencing old tables ==========

ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_department_id_fkey;
ALTER TABLE sys_department DROP CONSTRAINT IF EXISTS sys_department_parent_id_fkey;
ALTER TABLE calendar DROP CONSTRAINT IF EXISTS calendar_owner_user_id_fkey;
ALTER TABLE calendar DROP CONSTRAINT IF EXISTS calendar_created_by_fkey;
ALTER TABLE calendar_auto_subscribe_scope DROP CONSTRAINT IF EXISTS calendar_auto_subscribe_scope_calendar_id_fkey;
ALTER TABLE calendar_auto_subscribe_scope DROP CONSTRAINT IF EXISTS calendar_auto_subscribe_scope_department_id_fkey;
ALTER TABLE calendar_auto_subscribe_scope DROP CONSTRAINT IF EXISTS calendar_auto_subscribe_scope_user_id_fkey;
ALTER TABLE calendar_shared_member DROP CONSTRAINT IF EXISTS calendar_shared_member_calendar_id_fkey;
ALTER TABLE calendar_shared_member DROP CONSTRAINT IF EXISTS calendar_shared_member_user_id_fkey;
ALTER TABLE calendar_subscription DROP CONSTRAINT IF EXISTS calendar_subscription_calendar_id_fkey;
ALTER TABLE calendar_subscription DROP CONSTRAINT IF EXISTS calendar_subscription_user_id_fkey;
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_calendar_id_fkey;
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_organizer_user_id_fkey;
ALTER TABLE event_attachment DROP CONSTRAINT IF EXISTS event_attachment_uploaded_by_fkey;
ALTER TABLE event_participant DROP CONSTRAINT IF EXISTS event_participant_department_id_fkey;
ALTER TABLE event_participant DROP CONSTRAINT IF EXISTS event_participant_user_id_fkey;
ALTER TABLE event_todo DROP CONSTRAINT IF EXISTS event_todo_assignee_user_id_fkey;
ALTER TABLE export_task DROP CONSTRAINT IF EXISTS export_task_created_by_fkey;
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_operator_user_id_fkey;
ALTER TABLE backup_record DROP CONSTRAINT IF EXISTS backup_record_operator_user_id_fkey;
ALTER TABLE system_config DROP CONSTRAINT IF EXISTS system_config_updated_by_fkey;

-- ========== 2. Rename tables ==========

ALTER TABLE sys_department RENAME TO departments;
ALTER TABLE sys_user RENAME TO users;
ALTER TABLE calendar RENAME TO calendars;

-- ========== 3. Convert IDs from bigint to varchar(36) ==========

-- departments
ALTER TABLE departments ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE departments ALTER COLUMN parent_id TYPE VARCHAR(36) USING parent_id::text;

-- users
ALTER TABLE users ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE users ALTER COLUMN department_id TYPE VARCHAR(36) USING department_id::text;
ALTER TABLE users RENAME COLUMN phone TO mobile;
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'active';
UPDATE users SET status = CASE WHEN enabled THEN 'active' ELSE 'inactive' END;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
ALTER TABLE users DROP COLUMN enabled;
ALTER TABLE users DROP COLUMN IF EXISTS admin_flag;

-- calendars
ALTER TABLE calendars ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE calendars ALTER COLUMN owner_user_id TYPE VARCHAR(36) USING owner_user_id::text;
ALTER TABLE calendars RENAME COLUMN enabled TO visible;
ALTER TABLE calendars DROP COLUMN IF EXISTS created_by;

-- ========== 4. Convert FK columns in dependent tables ==========

ALTER TABLE calendar_auto_subscribe_scope ALTER COLUMN calendar_id TYPE VARCHAR(36) USING calendar_id::text;
ALTER TABLE calendar_auto_subscribe_scope ALTER COLUMN department_id TYPE VARCHAR(36) USING department_id::text;
ALTER TABLE calendar_auto_subscribe_scope ALTER COLUMN user_id TYPE VARCHAR(36) USING user_id::text;

ALTER TABLE calendar_shared_member ALTER COLUMN calendar_id TYPE VARCHAR(36) USING calendar_id::text;
ALTER TABLE calendar_shared_member ALTER COLUMN user_id TYPE VARCHAR(36) USING user_id::text;

ALTER TABLE calendar_subscription ALTER COLUMN calendar_id TYPE VARCHAR(36) USING calendar_id::text;
ALTER TABLE calendar_subscription ALTER COLUMN user_id TYPE VARCHAR(36) USING user_id::text;

ALTER TABLE event ALTER COLUMN calendar_id TYPE VARCHAR(36) USING calendar_id::text;
ALTER TABLE event ALTER COLUMN organizer_user_id TYPE VARCHAR(36) USING organizer_user_id::text;

ALTER TABLE event_participant ALTER COLUMN user_id TYPE VARCHAR(36) USING user_id::text;
ALTER TABLE event_participant ALTER COLUMN department_id TYPE VARCHAR(36) USING department_id::text;

ALTER TABLE event_attachment ALTER COLUMN uploaded_by TYPE VARCHAR(36) USING uploaded_by::text;

ALTER TABLE event_todo ALTER COLUMN assignee_user_id TYPE VARCHAR(36) USING assignee_user_id::text;

ALTER TABLE export_task ALTER COLUMN created_by TYPE VARCHAR(36) USING created_by::text;
ALTER TABLE audit_log ALTER COLUMN operator_user_id TYPE VARCHAR(36) USING operator_user_id::text;
ALTER TABLE backup_record ALTER COLUMN operator_user_id TYPE VARCHAR(36) USING operator_user_id::text;
ALTER TABLE system_config ALTER COLUMN updated_by TYPE VARCHAR(36) USING updated_by::text;

-- ========== 5. Re-add foreign keys ==========

ALTER TABLE users ADD CONSTRAINT users_department_id_fkey
  FOREIGN KEY (department_id) REFERENCES departments(id);
ALTER TABLE departments ADD CONSTRAINT departments_parent_id_fkey
  FOREIGN KEY (parent_id) REFERENCES departments(id);
ALTER TABLE calendars ADD CONSTRAINT calendars_owner_user_id_fkey
  FOREIGN KEY (owner_user_id) REFERENCES users(id);
ALTER TABLE calendar_auto_subscribe_scope ADD CONSTRAINT calendar_auto_subscribe_scope_calendar_id_fkey
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE;
ALTER TABLE calendar_auto_subscribe_scope ADD CONSTRAINT calendar_auto_subscribe_scope_department_id_fkey
  FOREIGN KEY (department_id) REFERENCES departments(id);
ALTER TABLE calendar_auto_subscribe_scope ADD CONSTRAINT calendar_auto_subscribe_scope_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE calendar_shared_member ADD CONSTRAINT calendar_shared_member_calendar_id_fkey
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE;
ALTER TABLE calendar_shared_member ADD CONSTRAINT calendar_shared_member_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE calendar_subscription ADD CONSTRAINT calendar_subscription_calendar_id_fkey
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE;
ALTER TABLE calendar_subscription ADD CONSTRAINT calendar_subscription_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE event ADD CONSTRAINT event_calendar_id_fkey
  FOREIGN KEY (calendar_id) REFERENCES calendars(id);
ALTER TABLE event ADD CONSTRAINT event_organizer_user_id_fkey
  FOREIGN KEY (organizer_user_id) REFERENCES users(id);
ALTER TABLE event_participant ADD CONSTRAINT event_participant_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE event_participant ADD CONSTRAINT event_participant_department_id_fkey
  FOREIGN KEY (department_id) REFERENCES departments(id);
ALTER TABLE event_attachment ADD CONSTRAINT event_attachment_uploaded_by_fkey
  FOREIGN KEY (uploaded_by) REFERENCES users(id);
ALTER TABLE event_todo ADD CONSTRAINT event_todo_assignee_user_id_fkey
  FOREIGN KEY (assignee_user_id) REFERENCES users(id);
ALTER TABLE export_task ADD CONSTRAINT export_task_created_by_fkey
  FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE audit_log ADD CONSTRAINT audit_log_operator_user_id_fkey
  FOREIGN KEY (operator_user_id) REFERENCES users(id);
ALTER TABLE backup_record ADD CONSTRAINT backup_record_operator_user_id_fkey
  FOREIGN KEY (operator_user_id) REFERENCES users(id);
ALTER TABLE system_config ADD CONSTRAINT system_config_updated_by_fkey
  FOREIGN KEY (updated_by) REFERENCES users(id);

-- ========== 6. Update indexes ==========

DROP INDEX IF EXISTS idx_users_department;
CREATE INDEX idx_users_department ON users(department_id);

DROP INDEX IF EXISTS idx_calendars_type_visible;
CREATE INDEX idx_calendars_type_visible ON calendars(type, visible);

COMMIT;
