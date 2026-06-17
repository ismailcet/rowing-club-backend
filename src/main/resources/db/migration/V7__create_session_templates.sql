CREATE TABLE session_templates (
                                   id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                                   membership_type_id UUID    NOT NULL REFERENCES membership_types(id),
                                   name               VARCHAR(255) NOT NULL,
                                   day_of_week        INT     NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
                                   start_time         TIME    NOT NULL,
                                   end_time           TIME    NOT NULL,
                                   capacity           INT     NOT NULL,
                                   is_active          BOOLEAN NOT NULL DEFAULT TRUE,
                                   created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
                                   updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);