# Postman — Full Merchant Flow

## Prerequisites

| Mode | What you need |
|------|----------------|
| **Quick (no Docker)** | `./gradlew bootRun` only — **dev** profile creates `admin@postman.local` / `Password1!` on startup |
| **PostgreSQL** | Docker **or** system Postgres + `psql` — see below |

### Quick start (no Docker, no psql)

```bash
./gradlew bootRun
```

Wait for log line: `Dev admin created for Postman: admin@postman.local / Password1!` (first run only).  
Then import this collection and run **Full Merchant Flow**.

### PostgreSQL (optional)

1. Start the API: `./gradlew bootRun -Pprofile=local`
2. **Admin user:** run once:
   ```bash
   docker compose up -d postgres
   psql "postgresql://atoma:atoma@localhost:5432/atoma_marketplace" -f scripts/seed-postman-admin.sql
   ```
   Credentials: `admin@postman.local` / `Password1!`
3. In Postman: **Import** → `postman/ATOMA-Merchant-Flow.postman_collection.json`
4. Open collection **Variables** → confirm `baseUrl` = `http://localhost:8090`
5. **Collection Runner** → select folder **Full Merchant Flow (run in order)** → **Run**

## H2 dev profile note

Admin cannot register via API. On H2-only dev, either switch to `local` profile + seed SQL above, or create an admin row manually in H2 console before running step **02 Admin Login**.

## What the run does

Register merchant → onboard → 4× KYC → admin verify → category → product → approve → customer order & pay → merchant ship & deliver → settlements & dashboard.

Collection scripts save `merchantToken`, `adminToken`, `customerToken`, `merchantId`, `categoryId`, `productId`, and `orderId` automatically.
