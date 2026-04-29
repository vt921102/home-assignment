# Flash Sale Backend Service

A production-grade Spring Boot 3 backend for time-limited flash sales. Covers user authentication (register → OTP → JWT), flash sale purchases with stock/balance atomicity, async event delivery via the Transactional Outbox pattern, and distributed coordination via Redis leader election.

# Flash Sale & Authentication Backend

Backend service for user authentication and flash sale management.

**Stack:** Java 21 · Spring Boot 3.5.14 · PostgreSQL 16 · Redis 7

---

## Quick Start

### Prerequisites
- Docker and Docker Compose v2
- Java 21 (for local development only)
- Maven 3.9+ (or use `./mvnw`)

### 1. Generate JWT keystore

```bash
chmod +x scripts/gen-keystore.sh
./scripts/gen-keystore.sh
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set:

```bash
JWT_KEYSTORE_PASSWORD=changeit
OTP_HASH_PEPPER=$(openssl rand -hex 32)
```

### 3. Start services

```bash
docker compose up -d
```

### 4. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Multi-instance demo

```bash
docker compose --profile multi-instance up --scale app=3
```

Traffic is routed through Nginx on port 80.
All 3 instances share PostgreSQL and Redis — stateless by design.

---

## Build & run locally

```bash
# Build
./mvnw clean package -DskipTests

# Run
java -jar target/flash-sale-backend.jar

# With specific profile
SPRING_PROFILES_ACTIVE=dev java -jar target/flash-sale-backend.jar
```

---

## Run tests

```bash
# Unit tests only
./mvnw test

# Unit + integration tests (requires Docker for Testcontainers)
./mvnw verify
```

---

## API Summary

### Authentication (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register with email or phone. Sends OTP. |
| POST | `/api/v1/auth/verify-otp` | Verify OTP. Issues access + refresh tokens. |
| POST | `/api/v1/auth/login` | Login. Returns token pair. |
| POST | `/api/v1/auth/refresh` | Rotate refresh token. |
| POST | `/api/v1/auth/logout` | Revoke refresh token. |
| POST | `/api/v1/auth/resend-otp` | Re-send OTP (rate limited: 5/hour). |

### Products (public read)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/products` | List active products. Supports `?categoryId=` and `?search=`. |
| GET | `/api/v1/products/{id}` | Get product by ID. |
| GET | `/api/v1/categories` | List all categories. |

### Flash Sale

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/flash-sale/current` | Public | Current active flash sale items. Cached 2s. |
| POST | `/api/v1/flash-sale/purchase` | User | Purchase item. Requires `X-Idempotency-Key` header. |

### Orders

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/orders/my` | User | Order history. |
| GET | `/api/v1/orders/my/{orderId}` | User | Order detail. |
| GET | `/api/v1/orders/my/transactions` | User | Balance transaction history. |

### Admin

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/admin/products` | Create product. |
| PUT | `/api/v1/admin/products/{id}` | Update product. |
| PATCH | `/api/v1/admin/products/{id}/status` | Change product status. |
| POST | `/api/v1/admin/categories` | Create category. |
| GET | `/api/v1/admin/flash-sale/sessions` | List sessions by date. |
| POST | `/api/v1/admin/flash-sale/sessions` | Create flash sale session. |
| POST | `/api/v1/admin/flash-sale/sessions/{id}/items` | Add item to session. |
| DELETE | `/api/v1/admin/flash-sale/sessions/{id}` | Deactivate session. |
| GET | `/api/v1/admin/inventory/{productId}` | Get inventory. |
| POST | `/api/v1/admin/inventory/{productId}/restock` | Restock product. |

---

## Architecture

### Bounded contexts

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Database Schema](#database-schema)
4. [Configuration Reference](#configuration-reference)
5. [Architecture Overview](#architecture-overview)
6. [Bounded Contexts](#bounded-contexts)
7. [Key Flows](#key-flows)
8. [Outbox Pattern](#outbox-pattern)
9. [Security Design](#security-design)
10. [Test Suite — What Each Test Verifies and Why It Passes](#test-suite)
11. [Running the Project](#running-the-project)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21, Spring Boot 3.5.14 |
| Web | Spring MVC (virtual threads via Project Loom) |
| Persistence | Spring Data JPA, Hibernate, Flyway, PostgreSQL 16 |
| Caching / Locking | Redis 7 (Lettuce), Redisson 3.34.1 |
| Security | Spring Security, JJWT 0.12.6 (RS256/PKCS12) |
| Resilience | Spring Retry + AOP |
| Messaging | Transactional Outbox (poll-based, in-process) |
| API Docs | SpringDoc OpenAPI 2.8.14 |
| Mapping | MapStruct 1.6.3 |
| Code Generation | Lombok |
| Nullability | JSpecify 1.0.0 |
| Monitoring | Spring Actuator + Micrometer |
| Infrastructure | Docker, Docker Compose, Nginx (load balancer) |
| Code Quality | Spotless + Google Java Format 1.35.0 |
| Testing | JUnit 5, Mockito, Testcontainers 1.20.4, ArchUnit 1.3.0 |

---

## Project Structure

```
src/main/java/com/toanlv/flashsale/
├── FlashSaleApplication.java          # @SpringBootApplication entry point
│
├── auth/                              # Authentication bounded context
│   ├── controller/
│   │   └── AuthController.java        # POST /api/v1/auth/*
│   ├── domain/
│   │   ├── User.java                  # Entity: identifier, password hash, balance, status
│   │   ├── OtpVerification.java       # Entity: code_hash (SHA-256+pepper), expires_at, attempt_count
│   │   ├── RefreshToken.java          # Entity: token_hash, revoked, expires_at
│   │   ├── IdentifierType.java        # Enum: EMAIL | PHONE
│   │   ├── OtpPurpose.java            # Enum: REGISTRATION | PASSWORD_RESET | LOGIN_2FA | CHANGE_IDENTIFIER
│   │   └── UserStatus.java            # Enum: PENDING_VERIFICATION | ACTIVE | SUSPENDED | DELETED
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── LogoutRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ResendOtpRequest.java
│   │   └── VerifyOtpRequest.java
│   ├── outbox/
│   │   └── OtpDispatchHandler.java    # Outbox handler for OTP_DISPATCH events
│   ├── repository/
│   │   ├── UserRepository.java        # + deductBalance (atomic WHERE balance >= amount)
│   │   ├── OtpVerificationRepository.java  # + findActive, invalidateActive
│   │   └── RefreshTokenRepository.java     # + findByTokenHash, revokeAllForUser
│   ├── service/
│   │   ├── IAuthService.java
│   │   ├── IJwtService.java
│   │   ├── IOtpGenerator.java
│   │   ├── IOtpService.java
│   │   ├── IRefreshTokenService.java
│   │   └── impl/
│   │       ├── AuthService.java       # register / verifyOtp / login / logout / refresh / resendOtp
│   │       ├── JwtService.java        # issue / parse JWT (JJWT, PKCS12 keystore)
│   │       ├── OtpService.java        # issueOtp / verifyOtp (rate-limit + constant-time compare)
│   │       ├── OtpGenerator.java      # generate() → 6-digit string
│   │       └── RefreshTokenService.java  # issue / rotate (reuse detection) / revoke
│   └── strategy/
│       └── IdentifierDetector.java    # detect(email|phone) + normalize
│
├── flashsale/                         # Flash sale purchase bounded context
│   ├── controller/
│   │   ├── FlashSaleController.java        # GET /current, POST /purchase
│   │   └── FlashSaleAdminController.java   # Admin session management
│   ├── domain/
│   │   ├── FlashSaleSession.java      # Entity: saleDate, startTime, endTime, isActive
│   │   ├── FlashSaleSessionItem.java  # Entity: salePrice, totalQty, soldQty, @Version
│   │   └── UserDailyPurchaseLimit.java # Entity: UNIQUE(userId, purchaseDate)
│   ├── dto/
│   │   ├── AddSessionItemRequest.java
│   │   ├── CreateSessionRequest.java
│   │   ├── FlashSaleItemDto.java
│   │   ├── PurchaseRequest.java
│   │   ├── PurchaseResponse.java
│   │   └── SessionDto.java
│   ├── repository/
│   │   ├── FlashSaleSessionItemRepository.java  # + findByIdForPurchase (PESSIMISTIC_READ), decrementSold
│   │   ├── FlashSaleSessionRepository.java
│   │   └── UserDailyPurchaseLimitRepository.java # + insertIfAbsent (atomic INSERT OR NOTHING)
│   ├── service/
│   │   ├── IFlashSaleQueryService.java
│   │   ├── IIdempotencyService.java
│   │   ├── IPurchaseService.java
│   │   ├── ISessionAdminService.java
│   │   └── impl/
│   │       ├── PurchaseService.java       # purchase() + doPurchase() with retry + idempotency
│   │       ├── FlashSaleQueryService.java # getCurrentItems() @Cacheable 2s
│   │       ├── SessionAdminService.java   # createSession / addSessionItem
│   │       └── IdempotencyService.java    # Redis cache lookup + store
│   └── strategy/
│       ├── PurchaseEligibilityRule.java  # Sealed interface: check(PurchaseContext)
│       ├── PurchaseContext.java          # Immutable record: userId, item, balance, clock
│       ├── TimeWindowRule.java           # order=1: session active + within time window
│       ├── StockRule.java                # order=2: sold < total
│       ├── BalanceRule.java              # order=3: balance >= salePrice
│       └── DailyLimitRule.java           # order=4: INSERT OR NOTHING on user+date
│
├── order/                             # Order management bounded context
│   ├── controller/
│   │   └── OrderController.java       # GET /api/v1/orders (history, transactions)
│   ├── domain/
│   │   ├── Order.java                 # Entity: userId, status, totalAmount, idempotencyKey UNIQUE
│   │   ├── OrderItem.java             # Entity: product snapshot, unitPrice, subtotal
│   │   ├── OrderStatus.java           # Enum: PENDING | COMPLETED | FAILED | REFUNDED
│   │   └── BalanceTransaction.java    # Immutable audit log: amount, direction, reason, balance_after
│   ├── dto/
│   │   ├── BalanceTransactionDto.java
│   │   ├── OrderDto.java
│   │   ├── OrderItemDto.java
│   │   └── OrderSummaryDto.java
│   ├── repository/
│   │   ├── OrderRepository.java       # + findByUserId, findByIdempotencyKey
│   │   └── BalanceTransactionRepository.java
│   └── service/
│       ├── IOrderService.java
│       └── impl/
│           └── OrderService.java      # createFlashSaleOrder / query history
│
├── inventory/                         # Inventory management bounded context
│   ├── controller/
│   │   └── InventoryAdminController.java  # GET /{productId}, POST /{productId}/restock
│   ├── domain/
│   │   ├── Inventory.java             # Entity: totalQty, reservedQty, availableQty, @Version
│   │   └── InventoryAuditLog.java     # Audit: source_event_id UNIQUE (idempotency guard)
│   ├── dto/
│   │   ├── InventoryDto.java
│   │   └── RestockRequest.java
│   ├── outbox/
│   │   ├── PurchaseSyncHandler.java   # Handles FLASH_SALE_PURCHASED events
│   │   └── RestockSyncHandler.java    # Handles PRODUCT_RESTOCKED events
│   ├── repository/
│   │   ├── InventoryRepository.java   # + decrementAvailable, incrementAvailable (atomic WHERE)
│   │   └── InventoryAuditLogRepository.java
│   ├── service/
│   │   ├── IInventorySyncService.java
│   │   └── impl/
│   │       └── InventorySyncService.java  # handlePurchase / handleRestock / adminRestock / getInventory
│   └── worker/
│       ├── DeadLetterCleanupWorker.java   # Scheduled cleanup of dead-letter events
│       └── ReconciliationWorker.java      # Periodic inventory reconciliation
│
├── product/                           # Product catalog bounded context
│   ├── controller/
│   │   ├── ProductController.java     # GET /api/v1/products
│   │   └── ProductAdminController.java # POST/PUT/DELETE
│   ├── domain/
│   │   ├── Product.java               # Entity: sku UNIQUE, name, basePrice, category, status
│   │   ├── ProductCategory.java       # Self-referencing categories
│   │   └── ProductStatus.java
│   ├── dto/
│   │   ├── CategoryDto.java
│   │   ├── CreateCategoryRequest.java
│   │   ├── CreateProductRequest.java
│   │   ├── ProductDto.java
│   │   └── UpdateProductRequest.java
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   └── CategoryRepository.java
│   └── service/
│       ├── ICategoryService.java
│       ├── IProductService.java
│       └── impl/
│           ├── ProductService.java
│           └── CategoryService.java
│
├── config/                            # Application-wide Spring bean configuration
│   ├── SecurityConfig.java            # SecurityFilterChain, CORS, filter ordering
│   ├── JwtConfig.java                 # PKCS12 keystore → PrivateKey + PublicKey beans
│   ├── CacheConfig.java               # Redis CacheManager
│   ├── RedisConfig.java               # Lettuce connection factory, RedisTemplate
│   ├── JpaConfig.java                 # JPA / Hibernate settings
│   ├── AsyncConfig.java               # Virtual thread executor
│   ├── SchedulingConfig.java          # Enables @Scheduled
│   ├── ClockConfig.java               # Clock bean (system in prod, fixed in tests)
│   ├── JacksonConfig.java             # ObjectMapper customization
│   ├── OpenApiConfig.java             # Swagger / SpringDoc configuration
│   └── WebConfig.java                 # MVC interceptors and formatters
│
└── common/                            # Shared infrastructure (no domain imports)
    ├── config/
    │   └── ApplicationProperties.java  # @ConfigurationProperties (jwt, otp, rateLimit, flashSale)
    ├── exception/
    │   ├── BusinessException.java      # Runtime exception carrying ErrorCode
    │   ├── ErrorCode.java              # Enum: code + HTTP status + message
    │   ├── ApiError.java               # Error response DTO
    │   └── GlobalExceptionHandler.java # @ControllerAdvice
    ├── lock/
    │   └── LeaderLock.java             # Redisson-backed tryAcquire / release / runIfLeader
    ├── outbox/
    │   ├── domain/
    │   │   ├── OutboxEvent.java        # Entity: eventType, aggregateId, payload JSONB, status
    │   │   └── OutboxStatus.java       # Enum: PENDING | PROCESSING | COMPLETED | DEAD_LETTER
    │   ├── handler/
    │   │   └── OutboxEventHandler.java # Strategy interface: supportedType() + handle(event)
    │   ├── repository/
    │   │   └── OutboxEventRepository.java  # + fetchPendingBatch (FOR UPDATE SKIP LOCKED)
    │   ├── service/
    │   │   ├── IOutboxPublisher.java
    │   │   └── impl/
    │   │       └── OutboxPublisher.java    # publish() @Transactional(MANDATORY)
    │   └── worker/
    │       └── OutboxDispatchWorker.java   # Scheduled poller with leader election
    ├── security/
    │   ├── JwtAuthFilter.java          # Extract JWT → SecurityContext
    │   ├── RateLimitFilter.java        # Redis sliding-window rate limiter
    │   ├── IdempotencyFilter.java      # Extract X-Idempotency-Key header
    │   ├── RateLimitService.java       # tryAcquire(key, limit, window)
    │   ├── AuthenticatedUser.java      # Record: userId, roles
    │   ├── ApiAuthEntryPoint.java      # 401 handler
    │   └── ApiAccessDeniedHandler.java # 403 handler
    └── util/
        └── HashUtils.java             # sha256, hashOtp (salt+pepper), hashRefreshToken, constantTimeEquals

src/main/resources/
├── application.yml                    # Base configuration
├── application-dev.yml
├── application-docker.yml
├── application-prod.yml
└── db/migration/
    ├── V1__create_users_otp_refresh.sql
    ├── V2__create_product_category.sql
    ├── V3__create_inventory_audit.sql
    ├── V4__create_flash_sale_tables.sql
    ├── V5__create_order_tables.sql
    ├── V6__create_outbox_events.sql
    └── V7__seed_dev_data.sql

src/test/java/com/toanlv/flashsale/
├── architecture/ArchitectureTest.java
├── auth/
│   ├── AuthServiceTest.java
│   └── OtpServiceTest.java
├── common/
│   ├── lock/LeaderLockTest.java
│   ├── outbox/service/OutboxPublisherTest.java
│   └── util/HashUtilsTest.java
├── flashsale/
│   ├── FlashSalePurchaseIntegrationTest.java
│   └── PurchaseServiceTest.java
├── inventory/InventorySyncServiceTest.java
└── FlashSaleApplicationTests.java
```

---

## Database Schema

Seven Flyway migrations applied in order on startup.

### V1 — Users, OTP, Refresh Tokens

```sql
users (
  id UUID PK,
  identifier       VARCHAR UNIQUE with identifier_type,  -- email or phone
  identifier_type  VARCHAR(10),                           -- EMAIL | PHONE
  password_hash    VARCHAR,                               -- BCrypt cost=12
  balance          DECIMAL(19,4) DEFAULT 0 CHECK(>=0),
  status           VARCHAR(30),                           -- PENDING_VERIFICATION | ACTIVE | ...
  is_verified      BOOLEAN DEFAULT false,
  version          BIGINT DEFAULT 0                       -- optimistic lock
)

otp_verifications (
  id            UUID PK,
  user_id       UUID FK → users,
  code_hash     VARCHAR,        -- SHA-256(otp:userId:pepper)
  purpose       VARCHAR(30),    -- REGISTRATION | PASSWORD_RESET | ...
  attempt_count INT DEFAULT 0,
  is_used       BOOLEAN DEFAULT false,
  expires_at    TIMESTAMP
)

refresh_tokens (
  id          UUID PK,
  user_id     UUID FK → users,
  token_hash  VARCHAR UNIQUE,  -- SHA-256(rawToken)
  revoked     BOOLEAN DEFAULT false,
  expires_at  TIMESTAMP
)
```

### V2 — Products and Categories

```sql
product_categories (
  id        UUID PK,
  name      VARCHAR UNIQUE,
  parent_id UUID FK → product_categories  -- self-referencing hierarchy
)

products (
  id          UUID PK,
  sku         VARCHAR UNIQUE,
  name        VARCHAR,
  description TEXT,
  base_price  DECIMAL(19,4),
  image_url   VARCHAR,
  category_id UUID FK → product_categories,
  status      VARCHAR(20),  -- ACTIVE | INACTIVE | DISCONTINUED
  version     BIGINT DEFAULT 0
)
```

### V3 — Inventory

```sql
inventories (
  id                 UUID PK,
  product_id         UUID FK UNIQUE → products,
  total_quantity     INT CHECK(>=0),
  reserved_quantity  INT CHECK(>=0),
  available_quantity INT CHECK(>=0),
  CHECK (total_quantity = available_quantity + reserved_quantity),
  version BIGINT DEFAULT 0
)

inventory_audit_logs (
  id              UUID PK,
  product_id      UUID FK → products,
  source_event_id UUID UNIQUE,  -- prevents double-processing outbox events
  delta           INT,          -- positive = add, negative = remove
  reason          VARCHAR(40)
)
```

### V4 — Flash Sale

```sql
flash_sale_sessions (
  id         UUID PK,
  name       VARCHAR,
  sale_date  DATE,
  start_time TIME,
  end_time   TIME,
  is_active  BOOLEAN DEFAULT true,
  INDEX (sale_date, is_active)
)

flash_sale_session_items (
  id             UUID PK,
  session_id     UUID FK → flash_sale_sessions,
  product_id     UUID FK → products,
  sale_price     DECIMAL(19,4),
  total_quantity INT,
  sold_quantity  INT DEFAULT 0,
  per_user_limit INT DEFAULT 1,
  version        BIGINT DEFAULT 0,
  UNIQUE (session_id, product_id)
)

user_daily_purchase_limits (
  id             UUID PK,
  user_id        UUID FK → users,
  purchase_date  DATE,
  purchase_count INT DEFAULT 1,
  UNIQUE (user_id, purchase_date)   -- atomic enforcement: INSERT OR NOTHING
)
```

### V5 — Orders and Balance Transactions

```sql
orders (
  id               UUID PK,
  user_id          UUID FK → users,
  status           VARCHAR(20),  -- PENDING | COMPLETED | FAILED | REFUNDED
  total_amount     DECIMAL(19,4),
  idempotency_key  VARCHAR UNIQUE,  -- DB-level idempotency guard (Layer 2)
  order_type       VARCHAR(20),
  version          BIGINT DEFAULT 0
)

order_items (
  id              UUID PK,
  order_id        UUID FK → orders,
  product_id      UUID FK → products,
  product_name    VARCHAR,      -- snapshot at time of purchase
  quantity        INT,
  unit_price      DECIMAL(19,4),
  subtotal        DECIMAL(19,4),
  source_ref_id   UUID          -- FlashSaleSessionItem id
)

balance_transactions (
  id               UUID PK,
  user_id          UUID FK → users,
  order_id         UUID FK nullable → orders,
  amount           DECIMAL(19,4),
  direction        VARCHAR(10),  -- DEBIT | CREDIT
  balance_after    DECIMAL(19,4),
  reason           VARCHAR(40),  -- FLASH_SALE_PURCHASE | ORDER_REFUND | ...
  created_at       TIMESTAMP     -- immutable audit log
)
```

### V6 — Outbox Events

```sql
outbox_events (
  id              UUID PK,
  event_type      VARCHAR(50),     -- OTP_DISPATCH | FLASH_SALE_PURCHASED | PRODUCT_RESTOCKED
  aggregate_type  VARCHAR(30),     -- USER | ORDER | INVENTORY
  aggregate_id    UUID,
  payload         JSONB,           -- flexible event payload
  status          VARCHAR(20),     -- PENDING | PROCESSING | COMPLETED | DEAD_LETTER
  retry_count     INT DEFAULT 0,
  next_retry_at   TIMESTAMP,
  created_at      TIMESTAMP,
  processed_at    TIMESTAMP,
  INDEX (status, next_retry_at),   -- for efficient polling
  INDEX (aggregate_type, aggregate_id)
)
```

### V7 — Development Seed Data

- 3 users (email/phone/admin), all with BCrypt("Password123")
- 5 categories (Electronics → Smartphones/Laptops; Fashion → Shoes)
- 4 products (iPhone 15, Samsung S24, MacBook Air M3, Nike Air Max 90)
- 2 flash sale sessions (Morning 08:00–10:00, Evening 20:00–22:00)
- 4 session items (products at discounted prices with 10–50 units each)
- Corresponding inventory records

---

## Configuration Reference

`ApplicationProperties` (prefix `app`) binds all application-specific config:

```yaml
app:
  jwt:
    issuer: flash-sale-backend
    access-token-ttl: PT15M          # 15 minutes
    refresh-token-ttl: P7D           # 7 days
    keystore-path: keys/jwt.p12      # PKCS12 keystore
    keystore-password: ${JWT_KEYSTORE_PASSWORD}
    key-alias: jwt-signing

  otp:
    length: 6
    ttl: PT5M                        # 5 minutes
    pepper: ${OTP_HASH_PEPPER}       # Server-side secret for SHA-256 keying
    max-attempts: 5
    resend-limit: 5
    resend-window: PT1H

  rate-limit:
    login:    { limit: 5,  window: PT15M }
    otp:      { limit: 5,  window: PT1H  }
    purchase: { limit: 10, window: PT1M  }

  flash-sale:
    cache-ttl: PT2S                  # Current items cache (Redis)
    purchase-retry:
      max-attempts: 3
      initial-delay: PT0.05S
      multiplier: 1.5                # Exponential backoff on optimistic lock retry

  outbox:
    poll-interval-ms: 1000           # Worker poll frequency
    batch-size: 100
    max-retry-attempts: 5
```

**Environment variables required at runtime:**

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `REDIS_HOST` / `REDIS_PORT` | Redis coordinates |
| `JWT_KEYSTORE_PASSWORD` | PKCS12 keystore password |
| `OTP_HASH_PEPPER` | Secret pepper for OTP hashing |

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────┐
│  HTTP Request                                                   │
│                                                                 │
│  RateLimitFilter → JwtAuthFilter → IdempotencyFilter           │
│         ↓                                                       │
│  Controller → Service → Repository → PostgreSQL                │
│                 ↓                                               │
│           OutboxPublisher (within same @Transactional)         │
│                 ↓                                               │
│         outbox_events table ← commits atomically with data     │
│                                                                 │
│  OutboxDispatchWorker (every 1s, leader-elected via Redis)     │
│    → fetches PENDING events (FOR UPDATE SKIP LOCKED)           │
│    → routes to OutboxEventHandler implementations              │
│    → retry with exponential backoff; dead-letter after 5 tries │
└────────────────────────────────────────────────────────────────┘
```

**Layering rules enforced by ArchUnit:**
- Controllers → Services → Repositories (no skipping layers)
- `auth` cannot depend on `flashsale`, `inventory`, or `order`
- `product` cannot depend on `flashsale` or `order`
- `common` cannot depend on any domain bounded context
- No cyclic dependencies between top-level packages

---

## Bounded Contexts

### auth

Handles user lifecycle: register, OTP verification, login, logout, token refresh.

**Domain invariants:**
- A user can only be activated once (via OTP or idempotent re-verify)
- OTP is single-use, has max 5 attempts, expires in 5 minutes
- Refresh tokens are rotated on every use; reuse triggers full session revocation
- Password hash is BCrypt cost=12; never stored in plain text

### flashsale

Handles time-limited purchases. The purchase critical path is:

1. Check Redis idempotency cache (Layer 1 — fast path)
2. Acquire `PESSIMISTIC_READ` lock on the session item
3. Run eligibility rules in order: TimeWindow → Stock → Balance → DailyLimit
4. Atomic stock decrement via optimistic lock (`UPDATE WHERE version = :v`)
5. Atomic balance deduction (`UPDATE WHERE balance >= :price`)
6. Create `Order + OrderItem + BalanceTransaction` (same transaction)
7. Publish `FLASH_SALE_PURCHASED` to outbox (same transaction)
8. Post-commit: cache result in Redis (Layer 2 — 24h TTL)

On optimistic lock collision (concurrent buyers), the service retries up to 3 times with exponential backoff (50ms, 75ms, 112ms).

**Strategy pattern — `PurchaseEligibilityRule` (sealed interface):**

| Rule | Order | Guard | Error |
|------|-------|-------|-------|
| `TimeWindowRule` | 1 | Session active and within `[startTime, endTime)` | `FLASH_SALE_NOT_ACTIVE` |
| `StockRule` | 2 | `soldQty < totalQty` | `OUT_OF_STOCK` |
| `BalanceRule` | 3 | `balance >= salePrice` | `INSUFFICIENT_BALANCE` |
| `DailyLimitRule` | 4 | `INSERT OR NOTHING` on `(userId, date)` | `DAILY_LIMIT_EXCEEDED` |

Rules 2 and 3 are pre-flight checks only; the DB operations in steps 4 and 5 are the authoritative guards.

### inventory

Consumes outbox events to decrement/increment available stock. The `InventoryAuditLog.source_event_id` column has a `UNIQUE` constraint — this is the idempotency key that prevents double-processing if the outbox worker retries.

### order

Creates `Order + OrderItem + BalanceTransaction` atomically. The `orders.idempotency_key` unique constraint provides the DB-level idempotency guarantee when the Redis cache is cold.

### common

Shared infrastructure that domain packages depend on. Must not import from `auth`, `flashsale`, `inventory`, `order`, or `product`.

---

## Key Flows

### Registration and OTP Verification

```
POST /api/v1/auth/register { identifier, password }
│
├─ IdentifierDetector.detect() → EMAIL or PHONE
├─ Normalize identifier (lowercase email, standardize phone)
├─ BCrypt(password, 12) → hash
├─ UserRepository.findByIdentifierAndIdentifierType()
│   ├─ Not found          → User.create() + save
│   ├─ Found, unverified  → updatePasswordHash() + save
│   └─ Found, verified    → throw REGISTRATION_FAILED (no user enumeration)
└─ OtpService.issueOtp():
    ├─ RateLimitService.tryAcquire("otp:resend:{identifier}", 5, 1h)
    ├─ OtpRepository.invalidateActive(userId, REGISTRATION)
    ├─ OtpGenerator.generate() → "123456"
    ├─ HashUtils.hashOtp(otp, userId, pepper) → SHA-256 hex
    ├─ OtpRepository.save(OtpVerification{hash, expiresAt=now+5m})
    └─ OutboxPublisher.publish("OTP_DISPATCH", "USER", userId, {otp, channel, purpose})
        └─ [async] OtpDispatchHandler logs mock delivery

POST /api/v1/auth/verify-otp { identifier, otp }
│
├─ Find user by identifier
├─ If already verified → skip OTP check, issue tokens (idempotent)
└─ OtpService.verifyOtp():
    ├─ OtpRepository.findActive(userId, REGISTRATION)  → not found → OTP_INVALID
    ├─ attemptCount >= 5 → markUsed + save → OTP_MAX_ATTEMPTS_EXCEEDED
    ├─ isExpired(clock)  → OTP_EXPIRED
    ├─ isUsed()          → OTP_INVALID
    ├─ hashOtp(input) != stored hash → incrementAttempt + save → OTP_INVALID
    └─ Passed: markUsed() + save
        └─ User.activate() + issue JWT access token + refresh token
```

### Login and Token Refresh

```
POST /api/v1/auth/login { identifier, password }
│
├─ Find user (if not found: do fake BCrypt compare, then throw INVALID_CREDENTIALS)
├─ passwordEncoder.matches(raw, hash) → false → INVALID_CREDENTIALS
├─ !user.isVerified() → ACCOUNT_NOT_VERIFIED
├─ user.status == SUSPENDED → ACCOUNT_SUSPENDED
└─ Issue tokens

POST /api/v1/auth/refresh { refreshToken }
│
├─ RefreshTokenService.rotate(rawToken):
│   ├─ SHA-256(rawToken) → lookup DB
│   ├─ Token not found   → INVALID_REFRESH_TOKEN
│   ├─ Token revoked     → revokeAllForUser (reuse detected) → INVALID_REFRESH_TOKEN
│   ├─ Token expired     → INVALID_REFRESH_TOKEN
│   └─ Mark current revoked + issue new token (UUID v4) → RotationResult
└─ Issue new access JWT
```

### Flash Sale Purchase

```
POST /api/v1/flash-sale/purchase  (Header: X-Idempotency-Key: {uuid})
│
├─ [Layer 1] IdempotencyService.lookup(key) → hit → return cached response immediately
│
└─ PurchaseService.purchase() with @Retryable(OptimisticLockingFailureException, max=3)
    └─ doPurchase() [@Transactional]
        ├─ itemRepository.findByIdForPurchase(id) [PESSIMISTIC_READ, timeout 3s]
        │   └─ Not found → SESSION_ITEM_NOT_FOUND
        ├─ userRepository.findBalanceById(userId)
        ├─ Run rules: TimeWindowRule → StockRule → BalanceRule → DailyLimitRule
        ├─ itemRepository.decrementSold(id, version) [native UPDATE WHERE version=:v AND sold<total]
        │   └─ Returns 0 → OptimisticLockingFailureException → retry
        ├─ userRepository.deductBalance(userId, price) [native UPDATE WHERE balance>=price]
        │   └─ Returns 0 → INSUFFICIENT_BALANCE
        ├─ orderService.createFlashSaleOrder(...)  [Order + OrderItem + BalanceTransaction]
        ├─ outboxPublisher.publish("FLASH_SALE_PURCHASED", "ORDER", orderId, {...})
        └─ [post-commit] idempotencyService.cache(key, response, 24h)
```

### Outbox Event Dispatch

```
OutboxDispatchWorker.dispatch()  [every 1 second, @Scheduled]
│
├─ LeaderLock.tryAcquire("outbox-dispatch-leader", 30s) → false → skip
└─ fetchPendingBatch() [SELECT ... FOR UPDATE SKIP LOCKED, LIMIT 100]
    └─ for each event:
        └─ processOne(event) [@Transactional REQUIRES_NEW]
            ├─ handler = handlers.get(event.eventType)
            ├─ handler.handle(event)
            │   ├─ OTP_DISPATCH         → OtpDispatchHandler (logs; in production: send SMS/email)
            │   ├─ FLASH_SALE_PURCHASED → PurchaseSyncHandler:
            │   │     InventoryAuditLog.save(source_event_id=eventId)  [UNIQUE guard]
            │   │     inventoryRepository.decrementAvailable(productId, qty)
            │   └─ PRODUCT_RESTOCKED    → RestockSyncHandler:
            │         inventoryRepository.incrementAvailable(productId, qty)
            ├─ On success: event.markCompleted()
            └─ On failure:
                ├─ retryCount < maxAttempts → scheduleRetry() [nextRetryAt = now + 2^n sec]
                └─ retryCount >= maxAttempts → markDeadLetter()
```

### Inventory Reconciliation

```
ReconciliationWorker  [periodic @Scheduled, leader-elected]
│
└─ For each inventory record:
    ├─ Verify: total = available + reserved  (invariant from DB CHECK constraint)
    └─ On violation: log + alert (manual resolution required)
```

---

## Outbox Pattern

The Transactional Outbox pattern guarantees that business state changes and the events they trigger are committed atomically.

**Why it works:**

1. `OutboxPublisher.publish()` has `@Transactional(propagation = MANDATORY)` — it refuses to run outside an existing transaction. This means the `OutboxEvent` row is written in the same DB transaction as the business operation (e.g., order creation). Either both commit or both roll back.

2. `OutboxDispatchWorker` polls the `outbox_events` table every second. It uses `FOR UPDATE SKIP LOCKED` to prevent multiple worker instances from picking up the same event simultaneously, even under horizontal scaling.

3. Each handler processes its event in a `REQUIRES_NEW` transaction. The event row is only marked `COMPLETED` after the handler succeeds. If the process crashes mid-dispatch, the event remains `PENDING` and is reprocessed on the next poll.

4. Idempotency is guaranteed at the handler level. `PurchaseSyncHandler` inserts an `InventoryAuditLog` row whose `source_event_id` is the outbox event's UUID — the `UNIQUE` constraint on that column causes the second insert attempt to fail (or be skipped), preventing double-decrements on retry.

5. Failed events are retried with exponential backoff (2^retryCount seconds, capped at 600s). After `maxAttempts` (default 5), the event is moved to `DEAD_LETTER` status for manual review.

---

## Security Design

### JWT Access Tokens

- RS256 algorithm, PKCS12 keystore (private key for signing, public key for verification)
- 15-minute TTL — short enough that no server-side blocklist is needed
- Claims: `sub` (userId), `identifier`, `role`, `iss` (flash-sale-backend), `exp`

### Refresh Tokens

- Random UUID v4 — never stored in plain text, only its SHA-256 hash
- 7-day TTL, rotation on every use
- Reuse detection: if a previously revoked token is presented, all sessions for that user are immediately revoked

### Passwords

- BCrypt cost=12 (~150ms per hash) — slow enough to make brute-force impractical
- Fake BCrypt compare on user-not-found prevents timing-based user enumeration

### OTP

- 6-digit code, stored as `SHA-256(otp + ":" + userId + ":" + pepper)`
- Per-user salt (userId) ensures same code produces a different hash for different users
- Global pepper (server secret) means the DB alone is not sufficient to crack OTPs
- Constant-time comparison (`MessageDigest.isEqual`) prevents timing attacks
- 5-minute TTL, 5 maximum attempts, 5 resend per hour per identifier

### Rate Limiting

Redis sliding-window counters, checked at `RateLimitFilter` level:

| Endpoint | Limit |
|----------|-------|
| `/auth/login` | 5 / 15 min |
| OTP resend | 5 / 1 hour |
| `/flash-sale/purchase` | 10 / 1 min |

### Idempotency

- Client generates UUID v4 `X-Idempotency-Key` header
- Layer 1: Redis cache (24h TTL) — fast path, returns immediately
- Layer 2: `orders.idempotency_key UNIQUE` — authoritative guard when cache is cold

---

## Test Suite

### `HashUtilsTest` — 13 unit tests

Tests the cryptographic utility layer with no Spring context (pure JUnit 5).

| Test | What it verifies | Why it passes |
|------|-----------------|---------------|
| `sha256_producesDeterministicOutput` | Same input → same hash on repeated calls | SHA-256 is deterministic; `HashUtils.sha256` always produces the same 64-char hex string |
| `sha256_producesHexStringOf64Chars` | Output is 64 hex characters | SHA-256 produces 32 bytes = 64 hex digits; regex `[0-9a-f]{64}` validates both |
| `sha256_differentInputsProduceDifferentHashes` | Different inputs → different hashes | Collision resistance of SHA-256 |
| `sha256_knownVector` | `SHA-256("abc")` matches the known test vector | Implementation uses `java.security.MessageDigest("SHA-256")`; expected value is `ba7816bf…015ad` |
| `hashOtp_sameInputProducesSameHash` | Deterministic for same otp+userId+pepper | Same inputs to `sha256(otp:userId:pepper)` always produce the same hash |
| `hashOtp_differentUserProducesDifferentHash` | Per-user salt isolation | The userId embedded in the payload means identical OTPs hash differently across users |
| `hashOtp_differentPepperProducesDifferentHash` | Pepper changes hash | Pepper is part of the hashed string; changing it produces a completely different hash |
| `hashOtp_differentOtpProducesDifferentHash` | Code differences change hash | Different raw OTP codes produce different SHA-256 outputs |
| `hashRefreshToken_producesDeterministicOutput` | Stable hash for a given token | Implemented as `sha256(rawToken)` — deterministic |
| `hashRefreshToken_producesHexStringOf64Chars` | Correct output format | Same SHA-256 properties |
| `constantTimeEquals_returnsTrue_forEqualStrings` | Matching strings return true | `MessageDigest.isEqual` on identical byte arrays |
| `constantTimeEquals_returnsFalse_forDifferentStrings` | Non-matching strings return false | Byte arrays differ |
| `constantTimeEquals_returnsTrue_forBothNull` | Null-safe equality | Explicit null check returns true when both are null |
| `constantTimeEquals_returnsFalse_forOneNull` | One-null returns false | Explicit null check short-circuits before byte comparison |
| `constantTimeEquals_worksWithHashedValues` | Integration: sha256 + constantTimeEquals | Same sha256 input → identical byte arrays → `isEqual` confirms match |

---

### `OtpServiceTest` — 8 unit tests

Uses `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(LENIENT)`. A fixed `Clock` at `2026-01-01T10:00:00Z` is injected so OTP expiry tests are deterministic regardless of when the test runs.

**Why `LENIENT` strictness is needed:** `@BeforeEach` stubs all config properties (`ttl`, `pepper`, `maxAttempts`, `resendLimit`, `resendWindow`) but individual tests that exercise error paths (e.g., `issueOtp_throwsRateLimitExceeded`) return before reading most of those stubs. Lenient mode avoids `UnnecessaryStubbingException` on the unused stubs.

**Why the fixed clock is required:** `OtpVerification.isExpired(Clock clock)` calls `Instant.now(clock)`. If the system wall clock were used instead (as it was before the fix), tests creating OTPs expiring 5 minutes from the *fixed* clock would see them as expired when run on a machine whose current date is past that window.

| Test | What it verifies |
|------|-----------------|
| `issueOtp_invalidatesPreviousOtp_savesNew_andPublishesEvent` | Full issue flow: invalidates old OTP, hashes new one with pepper, saves, publishes `OTP_DISPATCH` event with correct payload fields |
| `issueOtp_throwsRateLimitExceeded_whenLimitReached` | Rate limit check fires before any OTP is saved or event published |
| `verifyOtp_success_marksOtpAsUsed` | Valid OTP: hash matches, not expired, not used → `markUsed()` called and entity saved |
| `verifyOtp_throwsInvalid_whenOtpNotFound` | Empty `findActive` result → immediate `OTP_INVALID` |
| `verifyOtp_throwsInvalid_whenWrongCode` | Hash mismatch → `OTP_INVALID` + `attemptCount` incremented to 1 |
| `verifyOtp_throwsExpired_whenOtpExpired` | OTP with `expiresAt = now(fixedClock) - 1s` → `OTP_EXPIRED` |
| `verifyOtp_throwsMaxAttempts_andInvalidatesOtp_whenAttemptsExceeded` | 5 pre-loaded attempts → check fires before hash comparison → `OTP_MAX_ATTEMPTS_EXCEEDED` + `isUsed=true` |
| `verifyOtp_throwsInvalid_whenAlreadyUsed` | `otp.markUsed()` before call → `OTP_INVALID` |

---

### `AuthServiceTest` — 13 unit tests

Uses `@MockitoSettings(LENIENT)` for the same reason as `OtpServiceTest`: `setUp()` stubs `jwtProps.accessTokenTtl()` but several tests (logout, error paths) never reach `buildLoginResponse()`.

| Test | What it verifies |
|------|-----------------|
| `register_newUser_createsUserAndIssuesOtp` | No existing user → `User.create()` saved + `otpService.issueOtp()` called |
| `register_existingUnverifiedUser_updatesPasswordAndReissuesOtp` | Existing unverified user → password hash updated + OTP re-issued |
| `register_existingVerifiedUser_throwsRegistrationFailed` | Already activated user → `REGISTRATION_FAILED` + `otpService` never called |
| `verifyOtp_activatesUserAndIssuesTokens` | New user verifies OTP → `user.isVerified()=true`, `status=ACTIVE`, access+refresh tokens returned, `expiresInSeconds=900` |
| `verifyOtp_alreadyVerified_returnsTokensIdempotently` | Already-active user → `otpService.verifyOtp()` skipped, tokens still issued |
| `verifyOtp_userNotFound_throwsOtpInvalid` | No user in DB → `OTP_INVALID` (no information leak about user existence) |
| `login_validCredentials_returnsTokens` | Active user + correct password → `LoginResponse` with both tokens |
| `login_userNotFound_throwsInvalidCredentials` | Missing user → `INVALID_CREDENTIALS` |
| `login_wrongPassword_throwsInvalidCredentials` | Wrong password → `INVALID_CREDENTIALS` |
| `login_unverifiedUser_throwsAccountNotVerified` | Unactivated user → `ACCOUNT_NOT_VERIFIED` |
| `login_suspendedUser_throwsAccountSuspended` | Status=SUSPENDED → `ACCOUNT_SUSPENDED` |
| `logout_revokesRefreshToken` | `refreshTokenService.revoke()` called with the raw token |
| `refresh_validToken_returnsNewTokenPair` | `refreshTokenService.rotate()` returns new token + userId → new access JWT issued |

---

### `PurchaseServiceTest` — 5 unit tests

Several non-obvious setup requirements were needed to make these tests pass:

**`anyLong()` matcher:** `FlashSaleSessionItemRepository.decrementSold(UUID, long)` takes a primitive `long`. Using `any()` returns `null`, which causes an NPE when Mockito tries to auto-unbox it. `anyLong()` is the correct matcher for primitive `long` parameters.

**`TransactionSynchronizationManager` initialization:** `doPurchase()` calls `TransactionSynchronizationManager.registerSynchronization(...)` to schedule post-commit Redis caching. Without an active synchronization context, this throws `IllegalStateException: Transaction synchronization is not active`. `@BeforeEach` initializes it via `initSynchronization()` and `@AfterEach` clears it.

**`lenient().when()` for product stubs:** `buildItem()` stubs `product.getId()` and `product.getName()`, but error-path tests throw before reaching `createFlashSaleOrder()` where those stubs would be consumed. Using `lenient()` prevents `UnnecessaryStubbingException`.

**Capturing orderId before `verify()`:** Calling `order.getId()` (a mock method) inside `eq(order.getId())` within a `verify()` block confuses Mockito's matcher stack. Extracting `var orderId = order.getId()` before the verify block resolves this.

**`FlashSaleSessionItem.version = 0L`:** The `@Version` field is initialized to `0L` (not null). `decrementSold(UUID, long)` takes a primitive `long`; a null `version` would cause NPE on auto-unboxing.

| Test | What it verifies |
|------|-----------------|
| `purchase_returnsCachedResponse_onIdempotencyCacheHit` | Redis cache hit on first lookup → return immediately, no DB access |
| `doPurchase_succeeds_whenAllConditionsMet` | Stock decremented (returns 1), balance deducted (returns 1), order created, `FLASH_SALE_PURCHASED` event published with correct orderId |
| `doPurchase_throwsOptimisticLock_whenStockDecrementFails` | `decrementSold` returns 0 → `OptimisticLockingFailureException` → balance deduction and order creation never called |
| `doPurchase_throwsInsufficientBalance_whenDeductFails` | Stock decremented successfully, `deductBalance` returns 0 → `INSUFFICIENT_BALANCE` → order never created |
| `doPurchase_throwsNotFound_whenSessionItemMissing` | `findByIdForPurchase` returns empty → `SESSION_ITEM_NOT_FOUND` immediately |

---

### `ArchitectureTest` — ArchUnit rules

ArchUnit statically analyzes compiled bytecode. No Spring context is loaded.

**Critical fix applied:** The original patterns used `..flashsale..` as a wildcard for the flash sale sub-package. Because the root package is `com.toanlv.flashsale`, `..flashsale..` matched every class in the project — causing 873 false violations. The fix uses the explicit path `com.toanlv.flashsale.flashsale..`. Similarly, the cycle check originally scanned `com.example.flashsale.(*)..` (wrong base package); fixed to `com.toanlv.flashsale.(*)..`.

**`ApplicationProperties` moved to `common.config`:** `common.security.IdempotencyFilter` and `RateLimitFilter` imported `config.ApplicationProperties`. `config.SecurityConfig` imported `common.security.*`. This created a `common → config → common` cycle. Moving `ApplicationProperties` into `common.config` breaks the cycle since `common` can depend on its own sub-packages.

**`InventoryAdminController` rewritten:** It originally injected `InventoryRepository` and `OutboxPublisher` directly — a violation of `controllers_do_not_access_repositories_directly`. All logic was moved to `InventorySyncService`.

| Rule | What it enforces |
|------|-----------------|
| `auth_does_not_depend_on_domain_packages` | `auth` cannot import from `flashsale`, `inventory`, or `order` |
| `product_does_not_depend_on_flashsale_or_order` | `product` is a pure catalog, independent of sale/order logic |
| `common_does_not_depend_on_domain_packages` | Shared infrastructure has no upward dependencies |
| `controllers_reside_in_controller_package` | `@RestController` beans must be in `*.controller.*` packages |
| `services_reside_in_service_or_worker_package` | `@Service` beans must be in `*.service.*` or `*.worker.*` |
| `repositories_reside_in_repository_package` | `@Repository` beans must be in `*.repository.*` |
| `controllers_do_not_access_repositories_directly` | Controllers must go through services |
| `no_cyclic_dependencies` | No dependency cycles between top-level bounded-context packages |

---

### Integration Tests

**`FlashSalePurchaseIntegrationTest`** (Testcontainers — PostgreSQL + Redis)

- Starts real containers via `@Testcontainers` + `@Container`
- Runs actual Flyway migrations, no mocks for persistence layer
- Tests: end-to-end purchase flow, idempotency (second call returns same `orderId`), concurrent purchase (multiple threads, only one succeeds when stock=1)

**`InventorySyncServiceTest`** (Testcontainers — PostgreSQL)

- Tests that `handlePurchase` decrements inventory and inserts an `InventoryAuditLog`
- Tests that a second call with the same `source_event_id` is a no-op (UNIQUE constraint idempotency)
- Tests that `handleRestock` increments available quantity

---

## Running the Project

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for PostgreSQL + Redis)

### Generate JWT Keystore

```bash
mkdir -p src/main/resources/keys
keytool -genkey -alias jwt-signing -keyalg RSA -keysize 2048 \
  -keystore src/main/resources/keys/jwt.p12 \
  -storepass changeit -keypass changeit \
  -dname "CN=localhost" -validity 365 -storetype PKCS12
```

### Start Infrastructure

```bash
docker-compose up -d   # starts PostgreSQL + Redis
```

### Run Application

```bash
export JWT_KEYSTORE_PASSWORD=changeit
export OTP_HASH_PEPPER=dev-only-pepper

mvn spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080
# → Swagger UI: http://localhost:8080/swagger-ui/index.html
# → Health: http://localhost:8080/actuator/health
```

### Run Tests

```bash
# Unit tests (no containers required)
mvn test

# Integration tests (Docker required for Testcontainers)
mvn verify

# Architecture tests only
mvn test -Dtest=ArchitectureTest

# Single test class
mvn test -Dtest=PurchaseServiceTest
```

### Build Docker Image

```bash
mvn clean package -DskipTests
docker build -t flash-sale-backend .
docker-compose -f docker-compose.yml up
```

---

## Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| Sealed `PurchaseEligibilityRule` interface | Makes the exhaustive rule set explicit; adding a rule requires a deliberate change |
| `PESSIMISTIC_READ` lock on item fetch | Prevents concurrent reads from observing partially-updated state during validation |
| Optimistic lock for stock decrement | Allows concurrent attempts; only one wins at the DB level; loser retries without a long lock hold |
| `UPDATE WHERE balance >= price` | Atomic balance guard at the DB level — no application-level TOCTOU race |
| Two-layer idempotency (Redis + DB UNIQUE) | Redis gives sub-millisecond cache hits; DB constraint is the safety net when Redis is cold |
| `Propagation.MANDATORY` on `OutboxPublisher` | Prevents silent failures where an event is published outside a transaction and gets lost on rollback |
| `FOR UPDATE SKIP LOCKED` on outbox poll | Multiple worker instances can run without blocking each other; each claims a non-overlapping batch |
| SHA-256 for OTP (not BCrypt) | OTPs are short-lived (5 min) and rate-limited; BCrypt's slowness adds latency without meaningful benefit |
| `constantTimeEquals` for OTP/token comparison | Prevents timing-based attacks that could reveal whether a prefix of the code is correct |
| Leader election via Redisson for workers | Ensures exactly one instance runs scheduled jobs (outbox dispatch, reconciliation) in a multi-replica deployment |
| Virtual threads (`AsyncConfig`, `SchedulingConfig`) | High request concurrency with minimal thread overhead under Java 21 |
| `ArchUnit` tests | Architecture violations are caught at compile time in CI, not at runtime in production |
