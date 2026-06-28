ALTER TABLE memberships
DROP CONSTRAINT chk_membership_status;

ALTER TABLE memberships
    ADD CONSTRAINT chk_membership_status
        CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'COMPLETED'));