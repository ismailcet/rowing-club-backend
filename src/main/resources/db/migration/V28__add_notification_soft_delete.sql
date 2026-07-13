ALTER TABLE notifications ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_notifications_user_active ON notifications (user_id, is_deleted, created_at DESC);