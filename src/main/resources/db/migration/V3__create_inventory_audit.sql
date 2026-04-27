-- ============================================================
-- Inventories (1-to-1 with products)
-- ============================================================
CREATE TABLE inventories
(
    id                 UUID      NOT NULL DEFAULT gen_random_uuid(),
    product_id         UUID      NOT NULL,
    total_quantity     INTEGER   NOT NULL DEFAULT 0,
    reserved_quantity  INTEGER   NOT NULL DEFAULT 0,
    available_quantity INTEGER   NOT NULL DEFAULT 0,
    version            BIGINT    NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_inventories
        PRIMARY KEY (id),
    CONSTRAINT uq_inventories_product
        UNIQUE (product_id),
    CONSTRAINT fk_inventories_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_inventories_invariant
        CHECK (
            total_quantity = available_quantity + reserved_quantity
                AND available_quantity >= 0
                AND reserved_quantity >= 0
                AND total_quantity >= 0
            )
);

-- ============================================================
-- Inventory audit logs
-- Tracks every change to inventory with source event reference.
-- UNIQUE on source_event_id enforces idempotency in sync worker.
-- ============================================================
CREATE TABLE inventory_audit_logs
(
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_id      UUID        NOT NULL,
    source_event_id UUID        NOT NULL,
    delta           INTEGER     NOT NULL,
    reason          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_inventory_audit_logs
        PRIMARY KEY (id),
    CONSTRAINT uq_inventory_audit_source_event
        UNIQUE (source_event_id),
    CONSTRAINT fk_inventory_audit_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_inventory_audit_reason
        CHECK (reason IN (
                          'FLASH_SALE_PURCHASE',
                          'PRODUCT_RESTOCKED',
                          'MANUAL_ADJUSTMENT',
                          'ORDER_REFUND'
            ))
);

CREATE INDEX idx_inventory_audit_product
    ON inventory_audit_logs (product_id, created_at DESC);