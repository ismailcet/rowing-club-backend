CREATE TABLE branch_equipment (
                                  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  membership_type_id  UUID NOT NULL REFERENCES membership_types(id) ON DELETE CASCADE,
                                  equipment_type      VARCHAR(20) NOT NULL,
                                  quantity            INTEGER NOT NULL DEFAULT 0,
                                  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                  CONSTRAINT uq_branch_equipment UNIQUE (membership_type_id, equipment_type)
);

CREATE INDEX idx_branch_equipment_membership_type ON branch_equipment (membership_type_id);