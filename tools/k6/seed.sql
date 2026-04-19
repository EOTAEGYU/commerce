-- ============================================================
-- 성능 테스트 시드 데이터 (test/performance 브랜치 전용)
-- 실행: cat tools/k6/seed.sql | docker exec -i commerce-db psql -U commerce -d commerce
-- 정리: cat tools/k6/seed-cleanup.sql | docker exec -i commerce-db psql -U commerce -d commerce
-- ============================================================

BEGIN;

-- 1. 테스트 상품 (기존 카테고리 1번 재사용, 재고 50짜리 옵션 1개)
INSERT INTO products (name, description, price, category_id, image_url, created_at, updated_at)
VALUES ('k6테스트상품', '성능 테스트용 상품', 10000, 1, NULL, NOW(), NOW());

INSERT INTO product_options (product_id, size, color, stock, created_at, updated_at)
VALUES (
    (SELECT id FROM products WHERE name = 'k6테스트상품' LIMIT 1),
    'FREE', 'BLACK', 50, NOW(), NOW()
);

-- 2. 테스트 유저 100명 (비밀번호: Test1234! → BCrypt 해시)
DO $$
DECLARE
    v_i      INT;
    v_uid    BIGINT;
    v_cid    BIGINT;
    v_prod   BIGINT;
    v_opt    BIGINT;
BEGIN
    SELECT id INTO v_prod FROM products WHERE name = 'k6테스트상품' LIMIT 1;
    SELECT id INTO v_opt  FROM product_options WHERE product_id = v_prod LIMIT 1;

    FOR v_i IN 1..100 LOOP
        INSERT INTO users (email, password, name, role, created_at, updated_at)
        VALUES (
            'k6test' || v_i || '@test.com',
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'k6테스터' || v_i,
            'USER',
            NOW(), NOW()
        )
        ON CONFLICT (email) DO NOTHING;

        SELECT id INTO v_uid FROM users WHERE email = 'k6test' || v_i || '@test.com';

        INSERT INTO carts (user_id, created_at, updated_at)
        VALUES (v_uid, NOW(), NOW())
        ON CONFLICT (user_id) DO NOTHING;

        SELECT id INTO v_cid FROM carts WHERE user_id = v_uid;

        DELETE FROM cart_items WHERE cart_id = v_cid AND product_option_id = v_opt;

        INSERT INTO cart_items (cart_id, product_id, product_option_id, quantity, price, created_at, updated_at)
        VALUES (v_cid, v_prod, v_opt, 1, 10000, NOW(), NOW());
    END LOOP;
END $$;

COMMIT;

-- 결과 확인
SELECT 'users'      AS tbl, COUNT(*)       AS cnt FROM users WHERE email LIKE 'k6test%@test.com'
UNION ALL
SELECT 'carts',            COUNT(*)               FROM carts WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'k6test%@test.com')
UNION ALL
SELECT 'cart_items',       COUNT(*)               FROM cart_items WHERE product_id = (SELECT id FROM products WHERE name = 'k6테스트상품')
UNION ALL
SELECT 'stock(남은재고)',   stock::BIGINT          FROM product_options WHERE product_id = (SELECT id FROM products WHERE name = 'k6테스트상품');
