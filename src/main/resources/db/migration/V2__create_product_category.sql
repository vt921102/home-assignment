-- ============================================================
-- Product categories (self-referencing hierarchy)
-- ============================================================
CREATE TABLE product_categories
(
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    parent_id  UUID,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_categories
        PRIMARY KEY (id),
    CONSTRAINT uq_product_category_name
        UNIQUE (name),
    CONSTRAINT fk_product_category_parent
        FOREIGN KEY (parent_id) REFERENCES product_categories (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_product_categories_parent
    ON product_categories (parent_id);

-- ============================================================
-- Products
-- ============================================================
CREATE TABLE products
(
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    sku         VARCHAR(50)    NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    base_price  NUMERIC(19, 2) NOT NULL,
    image_url   VARCHAR(500),
    category_id UUID,
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products
        PRIMARY KEY (id),
    CONSTRAINT uq_products_sku
        UNIQUE (sku),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_products_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    CONSTRAINT chk_products_base_price
        CHECK (base_price >= 0)
);

CREATE INDEX idx_products_status
    ON products (status)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_products_category
    ON products (category_id);

CREATE INDEX idx_products_sku
    ON products (sku);