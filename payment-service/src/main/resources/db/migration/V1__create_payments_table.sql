CREATE TABLE payments (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);
