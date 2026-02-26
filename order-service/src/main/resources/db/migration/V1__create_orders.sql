CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);


