-- Migration: external contacts table and notification table

BEGIN;

-- 外部联系人表
CREATE TABLE IF NOT EXISTS external_contact (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(200),
    company         VARCHAR(200),
    contact_type    VARCHAR(20) DEFAULT 'wechat' CHECK (contact_type IN ('wechat', 'client', 'partner')),
    phone           VARCHAR(30),
    created_by      VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 日程参与人表增加 role 字段（用于权限分级）
ALTER TABLE event_participant ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'VIEWER' CHECK (role IN ('VIEWER', 'EDITOR'));

-- 日程参与人表增加 external_contact_id 字段
ALTER TABLE event_participant ADD COLUMN IF NOT EXISTS external_contact_id BIGINT REFERENCES external_contact(id) ON DELETE SET NULL;

-- 站内通知表
CREATE TABLE IF NOT EXISTS notification (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(50) NOT NULL,
    event_id        BIGINT REFERENCES event(id) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    message         TEXT,
    notification_type VARCHAR(50) DEFAULT 'event_update',
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id, is_read, created_at);

-- 用户表（用于JWT认证）
CREATE TABLE IF NOT EXISTS app_user (
    id              VARCHAR(50) PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(200) NOT NULL,
    display_name    VARCHAR(100),
    role            VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMIT;