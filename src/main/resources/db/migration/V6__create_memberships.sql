CREATE TABLE memberships (
                             id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id            UUID    NOT NULL REFERENCES users(id),
                             plan_id            UUID    NOT NULL REFERENCES membership_plans(id),
                             sessions_remaining INT     NOT NULL,
                             start_date         DATE    NOT NULL,
                             end_date           DATE    NOT NULL,
                             status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             created_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
                             updated_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
                             CONSTRAINT chk_membership_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE UNIQUE INDEX idx_memberships_active_user_plan
    ON memberships (user_id, plan_id)
    WHERE status = 'ACTIVE';