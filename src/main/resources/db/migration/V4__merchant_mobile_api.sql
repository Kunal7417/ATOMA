-- Merchant mobile API: refresh tokens, application versioning, primary category

ALTER TABLE merchants ADD COLUMN IF NOT EXISTS primary_category_id UUID;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS application_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    replaced_by_hash VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    response_body TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
