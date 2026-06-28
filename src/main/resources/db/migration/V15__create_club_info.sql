CREATE TABLE club_info (
                           id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           name       VARCHAR(255) NOT NULL,
                           about      TEXT,
                           phone      VARCHAR(30),
                           email      VARCHAR(255),
                           address    TEXT,
                           latitude   DOUBLE PRECISION,
                           longitude  DOUBLE PRECISION,
                           instagram  VARCHAR(255),
                           website    VARCHAR(255),
                           updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tek satır (singleton). Admin düzenleyene kadar boş başlasın.
INSERT INTO club_info (name, about, phone, email, address)
VALUES ('Adana Kürek Kulübü', '', '', '', '');