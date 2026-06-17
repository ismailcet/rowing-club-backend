CREATE TABLE payments (
                          id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id      UUID           NOT NULL REFERENCES users(id),
                          plan_id      UUID           NOT NULL REFERENCES membership_plans(id),
                          membership_id UUID          REFERENCES memberships(id),
                          amount       NUMERIC(10, 2) NOT NULL,
                          status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
                          iyzico_ref   VARCHAR(255),
                          iyzico_token VARCHAR(500),
                          created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
                          updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
                          CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);