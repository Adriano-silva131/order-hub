# payment-service

Processamento assíncrono de pagamentos do OrderHub. Não expõe REST — orientado inteiramente a eventos Kafka.

## Responsabilidades

- Consome `order.created.v1` do tópico `order-events` e processa o pagamento.
- Verifica idempotência via `existsByOrderId` antes de reprocessar um pedido.
- Publica `payment.processed.v1` no tópico `payment-events` (consumido por `order-service` e `notification-service`).
- Usa o `GenericEventConsumer` compartilhado com `order-service`: `@RetryableTopic` (4 tentativas, backoff exponencial 2s→10s) e `@DltHandler` persistindo mensagens exauridas em `dlt_messages`, reprocessadas por `DltReprocessingService`.

## Variáveis de ambiente principais

| Variável | Propósito |
|---|---|
| `SPRING_DATASOURCE_URL` | Conexão com o PostgreSQL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `DLT_REPROCESS_INTERVAL` | Intervalo (ms) entre tentativas de reprocessamento da DLT (padrão: 60000) |

## Rodando localmente

```bash
./gradlew bootRun
```

Requer PostgreSQL e Kafka acessíveis (ver `infra/docker-compose.yml`) — perfil `local` ativado via `spring.profiles.active=local`.

## Testes

```bash
./gradlew test
```
