# notification-service

Serviço de notificações por e-mail do OrderHub. Consome ambos os tópicos de eventos e não possui infraestrutura de retry/DLT (diferente de `order-service`/`payment-service`).

## Responsabilidades

- Consome `order-events` e `payment-events` via `NotificationConsumer` (dois `@KafkaListener` explícitos, um por tópico, com container factories separadas).
- Envia e-mails transacionais em HTML via SMTP do Gmail (pedido recebido, pagamento aprovado/recusado).

> Os eventos de `order-events`/`payment-events` ainda não carregam o e-mail do cliente — por isso as notificações vão para um destinatário de demonstração configurável (`NOTIFICATION_DEMO_RECIPIENT_EMAIL` / `notification.demo-recipient-email`), até essa propagação existir de ponta a ponta.

## Variáveis de ambiente principais

| Variável | Propósito |
|---|---|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `MAIL_USERNAME` | Usuário SMTP do Gmail |
| `MAIL_PASSWORD` | Gmail App Password (nunca a senha da conta) |
| `NOTIFICATION_DEMO_RECIPIENT_EMAIL` | Destinatário de demonstração para os e-mails enviados |

## Rodando localmente

```bash
./gradlew bootRun
```

Requer Kafka acessível (ver `infra/docker-compose.yml`) — perfil `local` ativado via `spring.profiles.active=local`.

## Testes

```bash
./gradlew test
```
