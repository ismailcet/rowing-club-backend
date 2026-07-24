ALTER TABLE session_templates ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_session_templates_deleted ON session_templates (deleted);