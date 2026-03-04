CREATE UNIQUE INDEX idx_dlt_pending_unique
    ON dlt_messages (original_topic, message_key)
    WHERE status = 'PENDING';

ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
