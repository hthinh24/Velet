import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

export const options = {
  scenarios: {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '20s', target: 100 },
        { duration: '20s', target: 300 },
        { duration: '20s', target: 500 },
        { duration: '20s', target: 800 },
        { duration: '20s', target: 1200 },
        { duration: '20s', target: 1800 },
        { duration: '20s', target: 2500 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const userWalletId = 1011 + (__VU % 50);
  const merchantWalletId = 1061 + (__VU % 50);

  const payload = JSON.stringify({
    userWalletId,
    merchantWalletId,
    originalPrice: 10000,
  });

  const res = http.post('http://localhost:8083/api/v1/payments', payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-Idempotency-Key': uuidv4(),
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch {
        return false;
      }
    },
    'has paymentId': (r) => {
      try {
        return r.json('data.paymentId') !== undefined;
      } catch {
        return false;
      }
    },
  });
}

export function handleSummary(data) {
  return {
    "payment_summary.html": htmlReport(data),
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}