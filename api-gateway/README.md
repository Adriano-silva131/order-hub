# api-gateway

Ponto único de entrada do OrderHub. Spring Cloud Gateway (WebFlux, reativo) sem lógica de negócio — apenas roteamento, autenticação e proteção de tráfego.

## Responsabilidades

- **Validação de JWT**: valida tokens emitidos pelo [`auth-service`](../../auth-service-go) (`spring-security-oauth2-resource-server`, via JWKS) antes de encaminhar qualquer requisição.
- **Rate limiting**: algoritmo Token Bucket via Redis, chaveado pelo claim `sub` do JWT (fallback para IP quando não autenticado).
- **Roteamento**: encaminha para `order-service` e `catalog-service` por nome de container (Docker) ou `localhost` (dev local).

## Rotas configuradas

| Predicate | Destino |
|---|---|
| `/api/v1/orders`, `/api/v1/orders/**` | `order-service` |
| `/api/v1/products`, `/api/v1/products/**` | `catalog-service` |
| `/api/v1/payments/**` | `payment-service` |

## Rate limiter (padrão)

`replenishRate=2`, `burstCapacity=5`, `requestedTokens=1` — configurável em `src/main/resources/application.yml`. Espere HTTP 429 ao estourar o burst.

## Variáveis de ambiente principais

| Variável | Propósito |
|---|---|
| `SPRING_DATA_REDIS_HOST` | Host do Redis usado pelo rate limiter |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | URL do endpoint JWKS do `auth-service` para validação do JWT |

## Rodando localmente

```bash
./gradlew bootRun
```

Requer Redis e o `auth-service` acessíveis (ver `infra/docker-compose.yml`).

## Testes

```bash
./gradlew test
```
