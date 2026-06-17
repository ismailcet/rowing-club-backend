CREATE TABLE enrollments (
                             id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id       UUID    NOT NULL REFERENCES users(id),
                             session_id    UUID    NOT NULL REFERENCES sessions(id),
                             membership_id UUID    NOT NULL REFERENCES memberships(id),
                             enrolled_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                             status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE UNIQUE INDEX idx_enrollments_active_user_session
    ON enrollments (user_id, session_id)
    WHERE status = 'ACTIVE';