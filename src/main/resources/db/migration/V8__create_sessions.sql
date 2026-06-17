CREATE TABLE sessions (
                          id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                          template_id      UUID    NOT NULL REFERENCES session_templates(id),
                          session_date     DATE    NOT NULL,
                          start_time       TIME    NOT NULL,
                          end_time         TIME    NOT NULL,
                          current_capacity INT     NOT NULL DEFAULT 0,
                          max_capacity     INT     NOT NULL,
                          status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                          created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
                          CONSTRAINT chk_session_status   CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
                          CONSTRAINT chk_session_capacity CHECK (current_capacity >= 0 AND current_capacity <= max_capacity)
);

CREATE UNIQUE INDEX idx_sessions_template_date
    ON sessions (template_id, session_date);