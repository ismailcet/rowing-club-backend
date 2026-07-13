CREATE TABLE expenses (
                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          category       VARCHAR(20)    NOT NULL, -- SUBE, PERSONEL, SABIT_GIDER, DIGER
                          branch_type_id UUID           REFERENCES membership_types(id) ON DELETE SET NULL,
                          trainer_id     UUID           REFERENCES users(id) ON DELETE SET NULL,
                          description    VARCHAR(500),
                          amount         NUMERIC(10, 2) NOT NULL,
                          expense_date   DATE           NOT NULL,
                          created_by     UUID           NOT NULL REFERENCES users(id),
                          created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_date ON expenses (expense_date);
CREATE INDEX idx_expenses_trainer ON expenses (trainer_id);
CREATE INDEX idx_expenses_category ON expenses (category);