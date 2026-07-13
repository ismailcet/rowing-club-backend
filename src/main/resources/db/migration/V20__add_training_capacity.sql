ALTER TABLE session_templates
    ADD COLUMN training_capacity INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sessions
    ADD COLUMN training_capacity INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sessions
    ADD COLUMN current_training_capacity INTEGER NOT NULL DEFAULT 0;

-- Kaydın hangi kovadan düştüğü; iptalde doğru kovaya iade için.
ALTER TABLE enrollments
    ADD COLUMN used_training_slot BOOLEAN NOT NULL DEFAULT FALSE;