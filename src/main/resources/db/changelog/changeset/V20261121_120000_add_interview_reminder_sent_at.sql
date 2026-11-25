-- liquibase formatted sql

-- changeset anastasia:V20261121_120000_add_interview_reminder_sent_at
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_interviews_reminder_sent_at ON interviews(reminder_sent_at);
