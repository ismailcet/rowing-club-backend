CREATE TABLE reminder_rules (
                                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                title          VARCHAR(200)  NOT NULL,
                                message        VARCHAR(1000) NOT NULL,
                                target_type    VARCHAR(20)   NOT NULL,
                                target_role    VARCHAR(20),
                                target_user_id UUID          REFERENCES users(id) ON DELETE CASCADE,
                                times          VARCHAR(200)  NOT NULL,
                                days_of_week   VARCHAR(50),
                                is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
                                created_by     UUID          NOT NULL REFERENCES users(id),
                                created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
                                last_sent_at   TIMESTAMP
);

CREATE INDEX idx_reminder_rules_active ON reminder_rules (is_active);