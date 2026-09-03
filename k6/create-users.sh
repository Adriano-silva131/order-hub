#!/bin/bash
# Cria usuários de teste no auth-service e gera tokens de login.
# Os tokens são salvos em k6/tokens.json e usados pelo load-test.js.
# Tokens expiram em 5 minutos (ACCESS_TOKEN_TTL_SECONDS) — rode este script
# imediatamente antes do k6.
#
# Docker Compose: AUTH_BASE_URL padrão já aponta pra localhost:8090.
# Minikube: kubectl port-forward svc/auth-service 8090:8090 -n orderhub &

set -e

AUTH_BASE_URL="${AUTH_BASE_URL:-http://localhost:8090}"
ADMIN_API_KEY="${AUTH_ADMIN_API_KEY:?defina AUTH_ADMIN_API_KEY (veja k6/.env.example)}"
NUM_USERS="${NUM_USERS:-20}"
PASSWORD="k6pass123"
OUTPUT="$(dirname "$0")/tokens.json"

echo "==> Montando payload de $NUM_USERS usuários..."
USERS_JSON=$(jq -n --argjson n "$NUM_USERS" --arg pw "$PASSWORD" '
  [range(1; $n+1) | {email: ("k6-user-" + (.|tostring) + "@k6.test"), password: $pw, name: ("k6 user " + (.|tostring))}]
  | {users: .}')

echo "==> Criando usuários via /admin/users/bulk..."
curl -sf -X POST "$AUTH_BASE_URL/admin/users/bulk" \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY" -H "Content-Type: application/json" \
  -d "$USERS_JSON" > /dev/null

echo "==> Login para coletar tokens..."
TOKENS="[]"
for i in $(seq 1 "$NUM_USERS"); do
  EMAIL="k6-user-$i@k6.test"
  TOKEN=$(curl -sf -X POST "$AUTH_BASE_URL/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r '.access_token')
  TOKENS=$(echo "$TOKENS" | jq --arg t "$TOKEN" '. + [$t]')
done

echo "$TOKENS" > "$OUTPUT"
TOTAL=$(echo "$TOKENS" | jq 'length')
echo "==> $TOTAL tokens salvos em $OUTPUT"
echo "==> Execute agora: k6 run -e BASE_URL=http://localhost:8000 k6/load-test.js"
