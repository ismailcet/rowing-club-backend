CREATE TABLE trainer_branches (
                                  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  trainer_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  membership_type_id  UUID NOT NULL REFERENCES membership_types(id) ON DELETE CASCADE,
                                  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                  CONSTRAINT uq_trainer_branches UNIQUE (trainer_id, membership_type_id)
);

CREATE INDEX idx_trainer_branches_trainer ON trainer_branches (trainer_id);