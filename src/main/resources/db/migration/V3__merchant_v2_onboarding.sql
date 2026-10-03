-- Merchant V2 Phase 2: onboarding wizard, application status, OTP-ready metadata

ALTER TABLE merchants ADD COLUMN IF NOT EXISTS application_number VARCHAR(20) UNIQUE;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS application_workflow_status VARCHAR(30);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS current_wizard_step INT NOT NULL DEFAULT 0;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS business_type VARCHAR(40);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS main_category VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS owner_full_name VARCHAR(200);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS representative_role VARCHAR(30);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS owner_email VARCHAR(120);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS building_number VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS street VARCHAR(200);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS landmark VARCHAR(200);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS payout_method VARCHAR(30);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS bank_account_hint VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS fix_by_deadline TIMESTAMP;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS reapply_after TIMESTAMP;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS decision_reference VARCHAR(30);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(1000);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS draft_saved_at TIMESTAMP;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS document_number VARCHAR(100);
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS expiry_date DATE;

CREATE TABLE IF NOT EXISTS merchant_application_status_history (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    workflow_status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    note VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_merchant_app_history_merchant ON merchant_application_status_history(merchant_id);

CREATE TABLE IF NOT EXISTS merchant_review_notes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    field_key VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS merchant_verification_checks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    check_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_checked_at TIMESTAMP,
    UNIQUE (merchant_id, check_type)
);

CREATE TABLE IF NOT EXISTS merchant_consents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    consent_key VARCHAR(80) NOT NULL,
    policy_version VARCHAR(40) NOT NULL,
    accepted_at TIMESTAMP NOT NULL,
    UNIQUE (merchant_id, consent_key)
);
