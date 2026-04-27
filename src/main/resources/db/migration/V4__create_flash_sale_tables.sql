-- ============================================================
-- Flash sale sessions
-- Each session defines a time window for a flash sale event.
-- Multiple sessions can exist per day with non-overlapping windows.
-- ============================================================
CREATE TABLE flash_sale_sessions
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    sale_date  DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_flash_sale_sessions
        PRIMARY KEY (id),
    CONSTRAINT chk_flash_sale_session_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_flash_sale_sessions_active_date
    ON flash_sale_sessions (sale_date, is_active)
    WHERE is_active = TRUE;

-- ============================================================
-- Flash sale session items
-- Each item defines a product available in a session
-- with its own price, quantity, and per-user limit.
-- ============================================================
CREATE TABLE flash_sale_session_items
(
    id              UUID           NOT NULL DEFAULT gen_random_uuid(),
    session_id      UUID           NOT NULL,
    product_id      UUID           NOT NULL,
    sale_price      NUMERIC(19, 2) NOT NULL,
    total_quantity  INTEGER        NOT NULL,
    sold_quantity   INTEGER        NOT NULL DEFAULT 0,
    per_user_limit  INTEGER        NOT NULL DEFAULT 1,
    version         BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_flash_sale_session_items
        PRIMARY KEY (id),
    CONSTRAINT uq_flash_sale_session_product
        UNIQUE (session_id, product_id),
    CONSTRAINT fk_flash_sale_session_items_session
        FOREIGN KEY (session_id) REFERENCES flash_sale_sessions (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_flash_sale_session_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_flash_sale_item_sale_price
        CHECK (sale_price >= 0),
    CONSTRAINT chk_flash_sale_item_total_qty
        CHECK (total_quantity > 0),
    CONSTRAINT chk_flash_sale_item_sold_qty
        CHECK (sold_quantity >= 0
            AND sold_quantity <= total_quantity),
    CONSTRAINT chk_flash_sale_item_per_user_limit
        CHECK (per_user_limit >= 1)
);

CREATE INDEX idx_flash_sale_session_items_session
    ON flash_sale_session_items (session_id);

CREATE INDEX idx_flash_sale_session_items_product
    ON flash_sale_session_items (product_id);

-- ============================================================
-- User daily purchase limits
-- Tracks how many flash sale items a user has purchased today.
-- UNIQUE on (user_id, purchase_date) enables atomic INSERT ON CONFLICT.
-- ============================================================
CREATE TABLE user_daily_purchase_limits
(
    id             UUID    NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID    NOT NULL,
    purchase_date  DATE    NOT NULL,
    purchase_count INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT pk_user_daily_purchase_limits
        PRIMARY KEY (id),
    CONSTRAINT uq_user_daily_purchase_limits
        UNIQUE (user_id, purchase_date),
    CONSTRAINT fk_user_daily_purchase_limits_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_user_daily_purchase_count
        CHECK (purchase_count >= 1)
);