-- Idempotency request body fingerprint (same key + different body -> 422)
ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS request_body_hash VARCHAR(64);

ALTER TABLE merchants ADD COLUMN IF NOT EXISTS owner_father_name VARCHAR(200);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS owner_tazkira_number VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS owner_date_of_birth DATE;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS licence_expiry DATE;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000);

-- Standard merchant onboarding categories (English names; API localizes via Accept-Language)
INSERT INTO categories (id, created_at, updated_at, name, slug, active, sort_order)
SELECT gen_random_uuid(), NOW(), NOW(), v.name, v.slug, true, v.ord
FROM (VALUES
    ('Food & grocery', 'food-grocery', 1),
    ('Electronics', 'electronics', 2),
    ('Pharmacy', 'pharmacy', 3),
    ('Fashion', 'fashion', 4),
    ('Home & kitchen', 'home-kitchen', 5),
    ('Beauty', 'beauty', 6),
    ('Handicrafts & carpets', 'handicrafts-carpets', 7)
) AS v(name, slug, ord)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = v.slug);
