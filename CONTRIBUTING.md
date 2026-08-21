# Contribuindo

Este é um projeto pessoal de portfólio, mas contribuições, sugestões e relatos de bugs via issues/PRs são bem-vindos.

## Como rodar localmente

Veja as instruções de setup no [README.md](README.md) (ou [README.en.md](README.en.md) em inglês) e em `CLAUDE.md`.

## Padrão de commits

Mensagens de commit seguem o estilo [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`), em inglês, com o escopo do serviço quando aplicável (ex.: `feat(order-service): ...`).

## Antes de abrir um PR

- Rode os testes do(s) serviço(s) alterado(s): `./gradlew test` dentro do diretório do serviço.
- Garanta que `./gradlew build` passa sem erros.
- Não commite segredos, credenciais ou dados pessoais reais — use variáveis de ambiente e os arquivos `.env.example` como referência.
