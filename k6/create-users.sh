#!/bin/bash
# Cria 20 usuários de teste no Keycloak e gera tokens de dentro do cluster.
# Pré-requisito: kubectl port-forward svc/keycloak 9090:8080 -n orderhub rodando.
# Os tokens são salvos em k6/tokens.json e usados pelo load-test.js.
# Tokens expiram em 5 minutos — rode este script imediatamente antes do k6.

set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:9090}"
REALM="orderhub"
CLIENT_ID="orderhub_client"
NUM_USERS="${NUM_USERS:-20}"
OUTPUT="$(dirname "$0")/tokens.json"

echo "==> Obtendo token de admin..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli&grant_type=password&username=admin&password=admin" \
  | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "ERRO: falha ao obter token de admin. Verifique o port-forward."
  exit 1
fi

echo "==> Criando $NUM_USERS usuários no realm '$REALM'..."
for i in $(seq 1 $NUM_USERS); do
  USERNAME="k6-user-$i"

  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"firstName\":\"k6\",\"lastName\":\"user$i\",\"enabled\":true,\"emailVerified\":true,\"email\":\"$USERNAME@k6.test\",\"requiredActions\":[]}")

  if [ "$STATUS" = "201" ]; then
    echo "  + $USERNAME criado"
  elif [ "$STATUS" = "409" ]; then
    echo "  ~ $USERNAME já existe"
  else
    echo "  ERRO ao criar $USERNAME (status: $STATUS)"
    continue
  fi

  USER_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$USERNAME" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

  curl -s -o /dev/null \
    -X PUT "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/reset-password" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"type":"password","value":"k6pass","temporary":false}'
done

echo "==> Gerando tokens de dentro do cluster (issuer correto)..."
kubectl run token-generator --restart=Never --image=python:3.12-alpine -n orderhub \
  -- python3 -c "
import urllib.request, urllib.parse, json
tokens = []
for i in range(1, ${NUM_USERS} + 1):
    data = urllib.parse.urlencode({
        'client_id': '${CLIENT_ID}',
        'grant_type': 'password',
        'username': 'k6-user-' + str(i),
        'password': 'k6pass'
    }).encode()
    try:
        req = urllib.request.urlopen('http://keycloak:8080/realms/${REALM}/protocol/openid-connect/token', data)
        tokens.append(json.loads(req.read())['access_token'])
    except Exception as e:
        print('ERRO user', i, ':', e, flush=True)
print(json.dumps(tokens))
" 2>/dev/null

kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/token-generator -n orderhub --timeout=120s
kubectl logs token-generator -n orderhub > "$OUTPUT"
kubectl delete pod token-generator -n orderhub --grace-period=0 --wait=false

TOTAL=$(jq 'length' "$OUTPUT")
echo "==> $TOTAL tokens salvos em $OUTPUT"
echo "==> Execute agora: k6 run -e BASE_URL=http://192.168.49.2:31614 k6/load-test.js"
