-- 성능 테스트 데이터 정리
-- 실행: psql -U postgres -d commerce -f tools/k6/seed-cleanup.sql

BEGIN;

DELETE FROM cart_items
WHERE product_id = (SELECT id FROM products WHERE name = 'k6테스트상품');

DELETE FROM carts
WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'k6test%@test.com');

DELETE FROM orders
WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'k6test%@test.com');

DELETE FROM users
WHERE email LIKE 'k6test%@test.com';

DELETE FROM product_options
WHERE product_id = (SELECT id FROM products WHERE name = 'k6테스트상품');

DELETE FROM products
WHERE name = 'k6테스트상품';

COMMIT;

SELECT '정리 완료' AS result;
