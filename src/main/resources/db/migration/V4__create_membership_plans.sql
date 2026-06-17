CREATE TABLE membership_plans (
                                  id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                                  name              VARCHAR(255)   NOT NULL,
                                  description       VARCHAR(500),
                                  sessions_included INT            NOT NULL,
                                  duration_days     INT            NOT NULL,
                                  price             NUMERIC(10, 2) NOT NULL,
                                  is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
                                  created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
                                  updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);