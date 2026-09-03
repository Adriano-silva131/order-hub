import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL  = __ENV.BASE_URL;
const JWT_TOKEN = __ENV.JWT_TOKEN;


const multiTokens = new SharedArray('tokens', function () {
  try {
    return JSON.parse(open('./tokens.json'));
  } catch (_) {
    return [];
  }
});

export const options = {
  stages: [
    { duration: '30s', target: 5  },
    { duration: '1m',  target: 5  },
    { duration: '30s', target: 20 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // 95% das requisições < 1s
    http_req_failed:   ['rate<0.15'],   // menos de 15% de erros (rate limit conta)
    order_created:     ['rate>0'],      // pelo menos um pedido criado com sucesso
  },
};

const orderCreated   = new Rate('order_created');
const rateLimited    = new Counter('rate_limited_429');
const orderDuration  = new Trend('order_create_duration', true);
const catalogDuration = new Trend('catalog_list_duration', true);


let productIds = [];

export function setup() {
  const token = multiTokens.length > 0 ? multiTokens[0] : JWT_TOKEN;

  if (!token) {
    console.error('Nenhum token disponível. Execute k6/create-users.sh ou passe JWT_TOKEN.');
    return { productIds: [] };
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const products = [
    { name: 'Notebook Pro',    description: 'Notebook para desenvolvimento', price: 4999.99, active: true, attributes: { ram: '16GB', storage: '512GB' } },
    { name: 'Mouse Wireless',  description: 'Mouse sem fio ergonômico',      price: 149.90,  active: true, attributes: { dpi: 1600 } },
    { name: 'Teclado Mecânico', description: 'Teclado mecânico RGB',         price: 349.90,  active: true, attributes: { switches: 'red' } },
  ];

  const ids = [];
  for (const product of products) {
    const res = http.post(`${BASE_URL}/api/v1/products`, JSON.stringify(product), { headers });
    if (res.status === 201) {
      const body = JSON.parse(res.body);
      ids.push(body.id);
      console.log(`Produto criado: ${product.name} → id=${body.id}`);
    } else {
      console.warn(`Falha ao criar produto ${product.name}: status=${res.status} body=${res.body}`);
    }
  }

  return { productIds: ids };
}

export default function (data) {
  const token = multiTokens.length > 0
    ? multiTokens[(__VU - 1) % multiTokens.length]
    : JWT_TOKEN;

  if (!token) return;

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const roll = Math.random();

  if (roll < 0.7) {
    const res = http.get(`${BASE_URL}/api/v1/products`, { headers });

    catalogDuration.add(res.timings.duration);

    check(res, {
      'catalog: status 200':     (r) => r.status === 200,
      'catalog: body é array':   (r) => Array.isArray(JSON.parse(r.body || '[]')),
      'catalog: resposta < 500ms': (r) => r.timings.duration < 500,
    });

    if (res.status === 429) rateLimited.add(1);

  } else {
    const productIds = data.productIds || [];
    if (productIds.length === 0) return;

    const productId = productIds[Math.floor(Math.random() * productIds.length)];
    const payload = {
      items: [
        { productId, quantity: Math.floor(Math.random() * 3) + 1 },
      ],
    };

    const res = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify(payload), { headers });

    orderDuration.add(res.timings.duration);
    orderCreated.add(res.status === 201);

    check(res, {
      'order: status 201':        (r) => r.status === 201,
      'order: tem id':            (r) => JSON.parse(r.body || '{}').id !== undefined,
      'order: resposta < 1000ms': (r) => r.timings.duration < 1000,
    });

    if (res.status === 429) rateLimited.add(1);
    if (res.status !== 201) {
      console.warn(`Order falhou: status=${res.status} body=${res.body}`);
    }
  }

  sleep(0.5);
}

export function teardown(data) {
  console.log(`Produtos criados no setup: ${(data.productIds || []).length}`);
  console.log('Teste concluído. Verifique o HPA com: kubectl get hpa -n orderhub');
}
