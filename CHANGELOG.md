# Changelog

Este projeto ainda não segue um esquema formal de versionamento/releases; este arquivo registra marcos relevantes da evolução da plataforma.

## [Não lançado]

### Segurança
- Removida credencial real (Gmail App Password) que estava commitada em `k8s/secrets.yaml`, substituída por placeholder; e-mail e senha do notification-service agora são totalmente parametrizados via variáveis de ambiente.

### Adicionado
- Licença MIT.
- README em inglês (`README.en.md`) e READMEs individuais por serviço.
- Documentação OpenAPI/Swagger em `order-service` e `catalog-service`.
- Testes de segurança e rate limiting para o `api-gateway`.
- Healthchecks para todos os serviços de infraestrutura e aplicação no `infra/docker-compose.yml`.
- `.github/dependabot.yml` e execução real dos testes no pipeline de CI.

## Histórico anterior

Ver `git log` para o histórico completo de commits antes da criação deste changelog.
