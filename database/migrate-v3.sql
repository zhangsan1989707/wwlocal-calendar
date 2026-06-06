-- Migration: add modified_data column to event_exception for single-instance content overrides
-- Also add reminder_override column for per-instance reminder customization

BEGIN;

ALTER TABLE event_exception ADD COLUMN IF NOT EXISTS modified_data JSONB;

ALTER TABLE event_exception ADD COLUMN IF NOT EXISTS reminder_override JSONB;

COMMIT;