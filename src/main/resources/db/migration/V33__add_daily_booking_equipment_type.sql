ALTER TABLE daily_bookings ADD COLUMN equipment_type VARCHAR(20);

CREATE INDEX idx_daily_bookings_equipment_lookup
    ON daily_bookings (membership_type_id, booking_date, equipment_type);