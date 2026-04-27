-- ============================================================
-- Orders
-- Each order represents a completed purchase.
-- idempotency_key UNIQUE enforces exactly-once order creation.
-- ============================================================
CREATE TABLE orders
(
    id              UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID           NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    order_type      VARCHAR(20)    NOT NULL DEFAULT 'FLASH_SALE',
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_orders
        PRIMARY KEY (id),
    CONSTRAINT uq_orders_idempotency_key
        UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_orders_status
        CHECK (status IN (
                          'PENDING',
                          'COMPLETED',
                          'FAILED',
                          'REFUNDED'
            )),
    CONSTRAINT chk_orders_order_type
        CHECK (order_type IN ('FLASH_SALE', 'REGULAR')),
    CONSTRAINT chk_orders_total_amount
        CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_user_created
    ON orders (user_id, created_at DESC);

CREATE INDEX idx_orders_status
    ON orders (status);

-- ============================================================
-- Order items
-- Line items belonging to an order.
-- Snapshots product name at time of purchase — decoupled
-- from future product name changes.
-- ============================================================
CREATE TABLE order_items
(
    id            UUID           NOT NULL DEFAULT gen_random_uuid(),
    order_id      UUID           NOT NULL,
    product_id    UUID           NOT NULL,
    product_name  VARCHAR(255)   NOT NULL,
    quantity      INTEGER        NOT NULL DEFAULT 1,
    unit_price    NUMERIC(19, 2) NOT NULL,
    subtotal      NUMERIC(19, 2) NOT NULL,
    source_ref_id UUID,

    CONSTRAINT pk_order_items
        PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal
        CHECK (subtotal >= 0)
);

CREATE INDEX idx_order_items_order
    ON order_items (order_id);

CREATE INDEX idx_order_items_source_ref
    ON order_items (source_ref_id)
    WHERE source_ref_id IS NOT NULL;

-- ============================================================
-- Balance transactions
-- Immutable audit trail of every balance change.
-- order_id is nullable to support top-up / refund use cases.
-- ============================================================
CREATE TABLE balance_transactions
(
    id            UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id       UUID           NOT NULL,
    order_id      UUID,
    amount        NUMERIC(19, 2) NOT NULL,
    direction     VARCHAR(10)    NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    reason        VARCHAR(50)    NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_balance_transactions
        PRIMARY KEY (id),
    CONSTRAINT fk_balance_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_balance_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_balance_transactions_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_balance_transactions_amount
        CHECK (amount > 0),
    CONSTRAINT chk_balance_transactions_balance_after
        CHECK (balance_after >= 0),
    CONSTRAINT chk_balance_transactions_reason
        CHECK (reason IN (
                          'FLASH_SALE_PURCHASE',
                          'ORDER_REFUND',
                          'ADMIN_TOP_UP',
                          'ADMIN_DEDUCTION'
            ))
);

CREATE INDEX idx_balance_transactions_user
    ON balance_transactions (user_id, created_at DESC);

CREATE INDEX idx_balance_transactions_order
    ON balance_transactions (order_id)
    WHERE order_id IS NOT NULL;