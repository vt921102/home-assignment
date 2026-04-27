-- ============================================================
-- Development seed data
-- Only runs in dev/docker profile.
-- NOT for production — use flyway.locations to exclude.
-- ============================================================

-- ============================================================
-- Users
-- Passwords are BCrypt(cost=12) of "Password123"
-- ============================================================
INSERT INTO users (id, identifier, identifier_type, password_hash,
                   balance, status, is_verified)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     'user@example.com', 'EMAIL',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewohgfMpU.dGEbDG',
     5000000.00, 'ACTIVE', TRUE),

    ('00000000-0000-0000-0000-000000000002',
     '0912345678', 'PHONE',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewohgfMpU.dGEbDG',
     2000000.00, 'ACTIVE', TRUE),

    ('00000000-0000-0000-0000-000000000003',
     'admin@example.com', 'EMAIL',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewohgfMpU.dGEbDG',
     0.00, 'ACTIVE', TRUE);

-- ============================================================
-- Categories
-- ============================================================
INSERT INTO product_categories (id, name, parent_id)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Electronics', NULL),
    ('10000000-0000-0000-0000-000000000002', 'Smartphones',
     '10000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000003', 'Laptops',
     '10000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000004', 'Fashion', NULL),
    ('10000000-0000-0000-0000-000000000005', 'Shoes',
     '10000000-0000-0000-0000-000000000004');

-- ============================================================
-- Products
-- ============================================================
INSERT INTO products (id, sku, name, description,
                      base_price, category_id, status)
VALUES
    ('20000000-0000-0000-0000-000000000001',
     'IPHONE-15-128-BLACK',
     'iPhone 15 128GB Black',
     'Apple iPhone 15 128GB Black — latest model',
     24990000.00,
     '10000000-0000-0000-0000-000000000002',
     'ACTIVE'),

    ('20000000-0000-0000-0000-000000000002',
     'SAMSUNG-S24-256-WHITE',
     'Samsung Galaxy S24 256GB White',
     'Samsung Galaxy S24 256GB White — flagship 2024',
     21990000.00,
     '10000000-0000-0000-0000-000000000002',
     'ACTIVE'),

    ('20000000-0000-0000-0000-000000000003',
     'MACBOOK-AIR-M3-256-SILVER',
     'MacBook Air M3 256GB Silver',
     'Apple MacBook Air M3 chip 256GB SSD Silver',
     32990000.00,
     '10000000-0000-0000-0000-000000000003',
     'ACTIVE'),

    ('20000000-0000-0000-0000-000000000004',
     'NIKE-AIR-MAX-270-42',
     'Nike Air Max 270 Size 42',
     'Nike Air Max 270 — running shoes size 42',
     3490000.00,
     '10000000-0000-0000-0000-000000000005',
     'ACTIVE');

-- ============================================================
-- Inventories
-- ============================================================
INSERT INTO inventories (id, product_id,
                         total_quantity,
                         reserved_quantity,
                         available_quantity)
VALUES
    ('30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001',
     100, 0, 100),

    ('30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002',
     150, 0, 150),

    ('30000000-0000-0000-0000-000000000003',
     '20000000-0000-0000-0000-000000000003',
     50, 0, 50),

    ('30000000-0000-0000-0000-000000000004',
     '20000000-0000-0000-0000-000000000004',
     200, 0, 200);

-- ============================================================
-- Flash sale sessions
-- Two sessions today: morning and evening.
-- Uses CURRENT_DATE so seed data works on any day.
-- ============================================================
INSERT INTO flash_sale_sessions (id, name, sale_date,
                                 start_time, end_time, is_active)
VALUES
    ('40000000-0000-0000-0000-000000000001',
     'Morning Flash Sale',
     CURRENT_DATE, '08:00:00', '10:00:00', TRUE),

    ('40000000-0000-0000-0000-000000000002',
     'Evening Flash Sale',
     CURRENT_DATE, '20:00:00', '22:00:00', TRUE);

-- ============================================================
-- Flash sale session items
-- ============================================================
INSERT INTO flash_sale_session_items (id, session_id, product_id,
                                      sale_price,
                                      total_quantity,
                                      sold_quantity,
                                      per_user_limit)
VALUES
    -- Morning: iPhone and Samsung
    ('50000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001',
     19990000.00, 10, 0, 1),

    ('50000000-0000-0000-0000-000000000002',
     '40000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000002',
     17990000.00, 15, 0, 1),

    -- Evening: MacBook and Nike
    ('50000000-0000-0000-0000-000000000003',
     '40000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000003',
     27990000.00, 5, 0, 1),

    ('50000000-0000-0000-0000-000000000004',
     '40000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000004',
     2490000.00, 20, 0, 1);