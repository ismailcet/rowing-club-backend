CREATE TABLE settings (
                          id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                          key     VARCHAR(100) NOT NULL UNIQUE,
                          value   VARCHAR(255) NOT NULL,
                          description VARCHAR(255),
                          updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO settings (key, value, description) VALUES
    ('CANCELLATION_DEADLINE_HOURS', '12', 'Ders başlamadan kaç saat önceye kadar iptal yapılabilir');