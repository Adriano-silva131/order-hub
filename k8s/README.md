# Deploy no Kubernetes

Manifestos para rodar a stack completa do OrderHub em um cluster (testado com Minikube).

## Pré-requisitos

- Cluster Kubernetes acessível (`kubectl` configurado) — Minikube funciona bem para testes locais.
- Imagens dos 5 microsserviços já buildadas e disponíveis para o cluster (`docker build` + `minikube image load`, ou publicadas em um registry).

## Ordem de apply

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml      # ver aviso abaixo antes de aplicar
kubectl apply -f k8s/infra/
kubectl apply -f k8s/apps/
kubectl apply -f k8s/monitoring/
```

## ⚠️ Sobre `k8s/secrets.yaml`

Os valores em `k8s/secrets.yaml` são **placeholders de exemplo**, não segredos reais. Antes de aplicar em qualquer ambiente que não seja um teste local descartável:

1. Substitua os valores base64 por credenciais reais (nunca reutilize os placeholders do repositório).
2. Para produção, não edite o arquivo diretamente — use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets), [External Secrets Operator](https://external-secrets.io/) ou o gerenciador de secrets do seu provedor de nuvem, e injete os valores via CI/CD.
3. Nunca commite um `k8s/secrets.yaml` com valores reais.

## Estrutura

| Diretório | Conteúdo |
|---|---|
| `k8s/infra/` | Kafka, Keycloak, MongoDB, PostgreSQL, Redis |
| `k8s/apps/` | Um `Deployment` + `Service` por microsserviço |
| `k8s/monitoring/` | Prometheus e Grafana |

## Serviços expostos (ClusterIP) e portas

| Serviço | Porta |
|---|---|
| `api-gateway` | 8000 |
| `order-service` | 8080 |
| `catalog-service` | 8081 |
| `payment-service` | 8082 |
| `notification-service` | 8083 |
| `grafana` | 3000 |
| `prometheus` | 9090 |

Para acessar de fora do cluster (Minikube), use `minikube service <nome> -n orderhub` ou `kubectl port-forward`.
