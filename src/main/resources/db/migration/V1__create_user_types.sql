CREATE TABLE user_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO user_types (name, description) VALUES
    ('ADMIN',    'Sistem yöneticisi'),
    ('USER',     'Kulüp üyesi'),
    ('ANTRENÖR', 'Kulüp antrenörü');