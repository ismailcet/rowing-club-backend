CREATE TABLE membership_types (
                                  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                  name        VARCHAR(100) NOT NULL UNIQUE,
                                  description VARCHAR(255),
                                  created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO membership_types (name, description) VALUES
                                                     ('ROWING',     'Kürek dersleri'),
                                                     ('FITNESS',    'Fitness salon erişimi'),
                                                     ('WATER_BIKE', 'Su bisikleti dersleri');