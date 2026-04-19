import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const TOTAL_USERS = 100;
const TEST_PRODUCT_ID = 15;        // seed.sql로 생성된 k6테스트상품 ID
const TEST_PRODUCT_OPTION_ID = 77; // seed.sql로 생성된 k6테스트상품 옵션 ID

export const options = {
  vus: TOTAL_USERS,
  duration: '30s',
  thresholds: {
    // 5xx 서버 에러만 체크 — 4xx(재고 부족, 빈 장바구니)는 정상 처리
    'checks{check:5xx 에러 없음}': ['rate>0.99'],
    http_req_duration: ['p(95)<3000'],
  },
};

// setup(): 100명 가입 → 장바구니 담기 → 로그인 → 토큰 배열 반환
export function setup() {
  const tokens = [];

  for (let i = 1; i <= TOTAL_USERS; i++) {
    const email = `k6test${i}@test.com`;
    const password = 'Test1234!';

    // 1. 회원가입 (이미 있으면 무시)
    http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `k6테스터${i}` }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    // 2. 로그인
    const signinRes = http.post(
      `${BASE_URL}/api/users/signin`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (signinRes.status !== 200) {
      console.error(`[setup] 로그인 실패 user${i}: ${signinRes.status}`);
      tokens.push(null);
      continue;
    }

    const token = signinRes.json().data?.accessToken ?? null;

    // 3. 장바구니에 테스트 상품 담기
    if (token) {
      const cartRes = http.post(
        `${BASE_URL}/api/cart/items`,
        JSON.stringify({ productId: TEST_PRODUCT_ID, productOptionId: TEST_PRODUCT_OPTION_ID, quantity: 1 }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
      );
      if (cartRes.status !== 200 && cartRes.status !== 201) {
        console.warn(`[setup] cart add 실패 user${i}: ${cartRes.status} ${cartRes.body}`);
      }
    }

    tokens.push(token);
  }

  const validCount = tokens.filter(t => t !== null).length;
  console.log(`[setup] 준비 완료: ${validCount}/${TOTAL_USERS}명`);
  return { tokens };
}

// default(): 각 VU가 자신의 토큰으로 주문 1회 시도
export default function (data) {
  const idx = (__VU - 1) % TOTAL_USERS;
  const token = data.tokens[idx];

  if (!token) {
    console.warn(`[VU ${__VU}] 토큰 없음, 스킵`);
    return;
  }

  const res = http.post(
    `${BASE_URL}/api/orders`,
    null, // 요청 바디 없음 — 장바구니 기반 주문
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    }
  );

  check(res, {
    '정상 처리 (200 성공 또는 4xx 재고부족)': (r) =>
      r.status === 200 || (r.status >= 400 && r.status < 500),
    '5xx 에러 없음': (r) => r.status < 500,
  });

  sleep(0.1);
}

export function teardown() {
  console.log('=== 시나리오 A 완료 ===');
  console.log('초과 판매 확인: SELECT stock FROM product_options WHERE id = ' + TEST_PRODUCT_OPTION_ID);
  console.log('성공 주문 수 확인: SELECT COUNT(*) FROM orders WHERE user_id IN (SELECT id FROM users WHERE email LIKE \'k6test%@test.com\')');
}
