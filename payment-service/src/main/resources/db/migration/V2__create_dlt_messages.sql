CREATE TABLE dlt_messages (
    id             UUID PRIMARY KEY,
    original_topic VARCHAR(255) NOT NULL,
    message_key    VARCHAR(255),
    event_type     VARCHAR(255),
    payload        TEXT         NOT NULL,
    error_message  TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL,
    reprocessed_at TIMESTAMP
);
