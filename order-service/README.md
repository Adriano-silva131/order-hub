# order-service

Serviço responsável pela criação e persistência de pedidos, e pela orquestração inicial da saga de checkout do OrderHub.

## Responsabilidades

- Cria pedidos (`PENDING_PAYMENT`) e os persiste em PostgreSQL (migrações via Flyway).
- Consulta `catalog-service` via Feign para validar produtos, protegida por circuit breaker (Resilience4j, instância `catalog-service`).
- Publica `order.created.v1` no tópico Kafka `order-events`.
- Consome `payment.processed.v1` do tópico `payment-events` para atualizar o status do pedido (`PAID` ou `CANCELLED`).

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/orders` | Cria um novo pedido |

## Variáveis de ambiente principais

| Variável | Propósito |
|---|---|
| `SPRING_DATASOURCE_URL` | Conexão com o PostgreSQL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `INTEGRATION_CATALOG_URL` | Base URL do Feign client para o `catalog-service` |

## Rodando localmente

```bash
./gradlew bootRun
```

Requer PostgreSQL, Kafka e `catalog-service` acessíveis (ver `infra/docker-compose.yml`) — perfil `local` ativado via `spring.profiles.active=local`.

## Testes

```bash
./gradlew test
./gradlew test --tests "com.adriano.orderhub.controller.OrderControllerTest"
```
