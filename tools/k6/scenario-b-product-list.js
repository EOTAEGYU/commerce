import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  thresholds: {
    http_req_duration: ['p(95)<200'], // SLO: 95%의 요청이 200ms 이내
    http_req_failed: ['rate<0.001'],  // 에러율 0.1% 미만
  },
  stages: [
    { duration: '1m', target: 50 }, // 1분간 50 VU까지 점진적 증가
    { duration: '2m', target: 50 }, // 2분간 유지
    { duration: '1m', target: 0 },  // 1분간 종료
  ],
};

export default function () {
  // 인증 불필요 — 상품 목록 조회
  const res = http.get(`${BASE_URL}/api/products?page=0&size=20`);

  check(res, {
    'status 200': (r) => r.status === 200,
    // ApiResponse<Page<ProductResponse>> 구조: data.content
    'content 존재': (r) => {
      const body = r.json();
      return body?.data?.content?.length > 0;
    },
  });
}
