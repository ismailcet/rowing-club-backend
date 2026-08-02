ALTER TABLE daily_bookings ADD COLUMN payment_received_by UUID REFERENCES users(id);
ALTER TABLE daily_bookings ADD COLUMN payment_received_at TIMESTAMP;