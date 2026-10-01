-- One-time admin user for Postman "Full Merchant Flow" collection
-- Run against PostgreSQL (local/prod profile), e.g.:
--   docker compose up -d postgres
--   psql "postgresql://atoma:atoma@localhost:5432/atoma_marketplace" -f scripts/seed-postman-admin.sql
--
-- Login in Postman: admin@postman.local / Password1!

INSERT INTO users (
    id, created_at, updated_at, email, phone, password_hash,
    first_name, last_name, status, mfa_enabled
) VALUES (
    'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
    NOW(), NOW(),
    'admin@postman.local',
    '+9779800000001',
    '$2b$10$JL3TyFY1A/waqHcOIDW/cews/4Bc2rHLVfsKEEJoa7n7.cFFktQLK',
    'Postman',
    'Admin',
    'ACTIVE',
    FALSE
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles WHERE user_id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' AND role = 'ADMIN'
);
