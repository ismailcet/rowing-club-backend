CREATE TABLE notifications (
                               id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id    UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               title      VARCHAR(200)  NOT NULL,
                               body       VARCHAR(1000) NOT NULL,
                               is_read    BOOLEAN       NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);