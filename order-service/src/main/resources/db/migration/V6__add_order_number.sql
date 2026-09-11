CREATE SEQUENCE order_number_seq START WITH 1000;

ALTER TABLE orders ADD COLUMN order_number BIGINT;

UPDATE orders SET order_number = nextval('order_number_seq') WHERE order_number IS NULL;

ALTER TABLE orders ALTER COLUMN order_number SET DEFAULT nextval('order_number_seq');
ALTER TABLE orders ALTER COLUMN order_number SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uq_orders_order_number UNIQUE (order_number);
