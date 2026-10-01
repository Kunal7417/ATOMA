# ATOMA Pay Marketplace Platform

Spring Boot monolithic backend for the ATOMA Pay Marketplace — a multi-surface digital commerce platform serving merchants, customers, and internal operations teams through a single shared API.

## Architecture

| Layer | Components |
|-------|------------|
| **Client Surfaces** | Merchant Mobile, Merchant Web, Admin Portal, Customer Website, Customer Mobile |
| **API Gateway** | JWT authentication, rate limiting (via Spring Security) |
| **Core Modules** | Auth, Product, Order, Payment |
| **Business Services** | Merchant, Search, Analytics, Notification, Compliance |
| **Data Layer** | PostgreSQL, Redis, RabbitMQ, S3-compatible object storage |

## Technology Stack

- **Java 17** + **Spring Boot 3.4**
- **PostgreSQL 15** (primary database)
- **Redis 7** (caching)
- **RabbitMQ 3.12** (async events)
- **JWT** (unified authentication across all surfaces)
- **OpenAPI / Swagger UI** (API documentation)

## Project Structure

```
src/main/java/com/atoma/marketplace/
├── auth/           # Registration, login, JWT, RBAC
├── merchant/       # Onboarding, KYC/KYB, merchant operations
├── product/        # Catalog, categories, reviews
├── order/          # Cart, checkout, wishlist, fulfillment
├── payment/        # ATOMA Pay Wallet, escrow, settlement
├── search/         # Product discovery and full-text search
├── analytics/      # GMV dashboards, merchant KPIs
├── notification/   # Push/SMS/email/in-app notifications
├── compliance/     # Disputes, audit logs, AML/KYC hooks
├── admin/          # Platform policies, category management
├── catalog/        # Public catalog endpoints
└── config/         # Security, OpenAPI, JPA configuration
```

## Quick Start (Development)

### Prerequisites

- JDK 17+
- Gradle (wrapper included)

### Run locally (H2 in-memory)

```bash
./gradlew bootRun
```

The app starts on **http://localhost:8090** with the `dev` profile (H2 database, no PostgreSQL required).

### Run with local PostgreSQL

Credentials and database name match `docker-compose.yml` and `.env.example`.

**Option A — Docker (recommended)**

```bash
docker compose up -d postgres          # database only
# optional: docker compose up -d       # postgres + redis + rabbitmq
./gradlew bootRun -Pprofile=local
```

**Option B — PostgreSQL installed on the host**

```bash
sudo -u postgres psql -f scripts/init-local-postgres.sql
./gradlew bootRun -Pprofile=local
```

Flyway applies `src/main/resources/db/migration/V1__init_schema.sql` on startup. Data persists in the Docker volume `postgres_data` or in your local cluster.

### Staging / production-style profile

```bash
docker compose up -d
./gradlew bootRun -Pprofile=prod
```

## API Documentation

Once running, open:

- **Swagger UI:** http://localhost:8090/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8090/v3/api-docs
- **Health:** http://localhost:8090/actuator/health

## API Surface Map

| Prefix | Surface | Description |
|--------|---------|-------------|
| `/api/v1/auth` | All | Register, login, JWT tokens |
| `/api/v1/catalog` | Customer Website | Public product browse/search |
| `/api/v1/customer` | Customer Web/Mobile | Cart, checkout, wallet pay, wishlist |
| `/api/v1/merchant` | Merchant Mobile/Web | Onboarding, listings, orders, payouts |
| `/api/v1/admin` | Admin Portal | Verification, disputes, policies, analytics |

## Sample Flow

1. **Register merchant:** `POST /api/v1/auth/register` with role `MERCHANT`
2. **Login:** `POST /api/v1/auth/login` → copy JWT token
3. **Onboard:** `POST /api/v1/merchant/onboard` (Bearer token)
4. **Create product:** `POST /api/v1/merchant/products`
5. **Admin approves:** `PUT /api/v1/admin/products/{id}/approve?approved=true`
6. **Customer checkout:** cart → checkout → pay via wallet

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | dev default | JWT signing key (set in production) |
| `DB_HOST` | localhost | PostgreSQL host (`local` / `prod`) |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | atoma_marketplace | Database name |
| `DB_USER` / `DB_PASSWORD` | atoma / atoma | Credentials (see `.env.example`) |
| `REDIS_HOST` | localhost | Redis host |
| `RABBITMQ_HOST` | localhost | RabbitMQ host |

## Profiles

| Profile | Database | Use Case |
|---------|----------|----------|
| `dev` | H2 in-memory | Quick start, no PostgreSQL |
| `local` | PostgreSQL + Flyway | Local development with real DB |
| `prod` | PostgreSQL + Flyway | Staging/production |
| `test` | H2 | Unit/integration tests |

## Integration Stubs

The `AtomaWalletClient` in `payment/integration/` is a stub for ATOMA Pay Wallet API integration. Replace with real wallet, identity, notification, and courier API clients during Month 4–5 integration phase per the project roadmap.

## Build & Test

```bash
./gradlew build
./gradlew test
```

## License

Proprietary — ATOMA Pay / Ismat Tarin IT Services Company
