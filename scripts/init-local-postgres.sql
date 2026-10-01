-- One-time setup for a native PostgreSQL install (matches docker-compose credentials).
-- Run as a superuser, e.g.:
--   sudo -u postgres psql -f scripts/init-local-postgres.sql

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'atoma') THEN
    CREATE ROLE atoma LOGIN PASSWORD 'atoma';
  END IF;
END
$$;

SELECT 'CREATE DATABASE atoma_marketplace OWNER atoma'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'atoma_marketplace')\gexec

GRANT ALL PRIVILEGES ON DATABASE atoma_marketplace TO atoma;
