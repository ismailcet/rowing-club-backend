CREATE TABLE incomes (
                         id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         category       VARCHAR(20)    NOT NULL, -- SUBE, DIGER
                         branch_type_id UUID           REFERENCES membership_types(id) ON DELETE SET NULL,
                         description    VARCHAR(500),
                         amount         NUMERIC(10, 2) NOT NULL,
                         income_date    DATE           NOT NULL,
                         created_by     UUID           NOT NULL REFERENCES users(id),
                         created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incomes_date ON incomes (income_date);