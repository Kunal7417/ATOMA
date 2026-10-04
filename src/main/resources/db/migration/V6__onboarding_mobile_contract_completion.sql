-- Uploaded onboarding documents (merchant mobile multipart)
CREATE TABLE IF NOT EXISTS onboarding_uploaded_documents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    kind VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    preview_url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_docs_merchant ON onboarding_uploaded_documents(merchant_id);

ALTER TABLE merchants ADD COLUMN IF NOT EXISTS checks_updated_at TIMESTAMP;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS payout_bank_id VARCHAR(80);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS payout_wallet_provider_id VARCHAR(80);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS payout_account_name VARCHAR(200);

ALTER TABLE merchant_review_notes ADD COLUMN IF NOT EXISTS step_key VARCHAR(40);
ALTER TABLE merchant_review_notes ADD COLUMN IF NOT EXISTS note_title_key VARCHAR(80);
