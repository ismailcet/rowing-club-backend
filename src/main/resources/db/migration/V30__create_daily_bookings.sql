ALTER TABLE membership_types ADD COLUMN allows_daily_booking BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users ADD COLUMN can_manage_daily_bookings BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE daily_bookings (
                                id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                membership_type_id  UUID NOT NULL REFERENCES membership_types(id),
                                booking_date        DATE NOT NULL,
                                start_time          TIME NOT NULL,
                                end_time            TIME NOT NULL,
                                capacity            INTEGER NOT NULL,
                                customer_name       VARCHAR(150),
                                customer_phone      VARCHAR(30),
                                notes               VARCHAR(500),
                                created_by          UUID NOT NULL REFERENCES users(id),
                                created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_daily_bookings_date ON daily_bookings (booking_date);