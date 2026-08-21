# catalog-service

Catálogo de produtos do OrderHub, com schema flexível em MongoDB. Não participa da malha de eventos Kafka.

## Responsabilidades

- CRUD de produtos com atributos variáveis por categoria (schema flexível, sem migrações rígidas).
- Consumido internamente pelo `order-service` via Feign para validação de produtos em pedidos.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/products` | Lista produtos |
| `GET` | `/api/v1/products/{id}` | Busca um produto por id |
| `POST` | `/api/v1/products` | Cria um produto |

## Variáveis de ambiente principais

| Variável | Propósito |
|---|---|
| `SPRING_MONGODB_URI` | Connection string do MongoDB |

## Rodando localmente

```bash
./gradlew bootRun
```

Requer MongoDB acessível (ver `infra/docker-compose.yml`) — perfil `local` ativado via `spring.profiles.active=local`.

## Testes

```bash
./gradlew test
```
