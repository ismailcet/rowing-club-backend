ALTER TABLE payments
    ADD COLUMN payment_method   VARCHAR(20)  NOT NULL DEFAULT 'CASH',
    ADD COLUMN receipt_path     VARCHAR(500),
    ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE memberships
    ALTER COLUMN start_date DROP NOT NULL,
ALTER COLUMN end_date   DROP NOT NULL;

ALTER TABLE memberships
DROP CONSTRAINT chk_membership_status;

ALTER TABLE memberships
    ADD CONSTRAINT chk_membership_status
        CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'EXPIRED', 'CANCELLED'));