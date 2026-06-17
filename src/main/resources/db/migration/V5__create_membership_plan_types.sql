CREATE TABLE membership_plan_types (
                                       plan_id            UUID NOT NULL REFERENCES membership_plans(id)  ON DELETE CASCADE,
                                       membership_type_id UUID NOT NULL REFERENCES membership_types(id)  ON DELETE CASCADE,
                                       PRIMARY KEY (plan_id, membership_type_id)
);