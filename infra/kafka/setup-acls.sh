#!/bin/sh
# Grants the minimum Kafka ACLs each service needs, and explicitly creates the two base
# event topics — run on every `docker compose up` (kafka-acl-init service), idempotent:
# re-adding an ACL or re-creating an existing topic (--if-not-exists) is a no-op, not an
# error.
#
# The topic-create step exists because relying on producer-side auto-creation is
# inconsistent across clients: order-service's Spring Kafka producer auto-creates
# order-events on first publish, but payment-service's Go client (franz-go) does not —
# without an explicit create here, the very first real payment.processed.v1 publish fails
# with UNKNOWN_TOPIC_OR_PARTITION, silently stranding the payment as resolved in
# payment-service's own DB with no order-service ever finding out.
#
# Topic map (see order-hub-application/CLAUDE.md "Event-Driven Flow"):
#   order-service      -> produces order-events; consumes payment-events
#                          (Spring RetryableTopic: payment-events-retry-N,
#                          payment-events-dlt, all attempt-indexed/DLT topics
#                          it creates and consumes itself)
#   payment-service     -> consumes order-events (single retry topic
#     (Go)                 order-events-retry, per its own README); produces
#                          payment-events
#   notification-service -> consumes order-events and payment-events only,
#                          no retry/DLT topics of its own
set -e

cat > /tmp/admin.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="${KAFKA_ADMIN_PASSWORD}";
EOF

TOPICS="/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --command-config /tmp/admin.properties"
ACLS="/opt/kafka/bin/kafka-acls.sh --bootstrap-server kafka:9092 --command-config /tmp/admin.properties"

# Base topics — single broker/no replication in this local setup, same as every topic a
# service's own client has auto-created here so far.
$TOPICS --create --if-not-exists --topic order-events --partitions 1 --replication-factor 1 --config min.insync.replicas=1
$TOPICS --create --if-not-exists --topic payment-events --partitions 1 --replication-factor 1 --config min.insync.replicas=1

# order-service
$ACLS --add --allow-principal User:order-service --operation Create --operation Write --topic order-events
$ACLS --add --allow-principal User:order-service --operation Read --topic payment-events --group order-group
$ACLS --add --allow-principal User:order-service --operation All --topic payment-events-retry --resource-pattern-type prefixed
$ACLS --add --allow-principal User:order-service --operation All --topic payment-events-dlt
$ACLS --add --allow-principal User:order-service --operation Read --group order-group --resource-pattern-type prefixed

# payment-service (Go)
$ACLS --add --allow-principal User:payment-service --operation Create --operation Write --topic payment-events
$ACLS --add --allow-principal User:payment-service --operation Read --topic order-events --group payment-group
$ACLS --add --allow-principal User:payment-service --operation Read --group payment-group --resource-pattern-type prefixed
$ACLS --add --allow-principal User:payment-service --operation All --topic order-events-retry

# notification-service
$ACLS --add --allow-principal User:notification-service --operation Read --topic order-events --group notification-group
$ACLS --add --allow-principal User:notification-service --operation Read --topic payment-events --group notification-group

echo "kafka ACLs applied"
