CREATE TABLE users (
                       id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_type_id UUID         NOT NULL REFERENCES user_types(id),
                       email        VARCHAR(255) NOT NULL UNIQUE,
                       password     VARCHAR(255) NOT NULL,
                       full_name    VARCHAR(255) NOT NULL,
                       phone        VARCHAR(20),
                       is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);