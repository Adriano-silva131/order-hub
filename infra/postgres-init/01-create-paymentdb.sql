-- payment-service usa um banco separado (paymentdb) no mesmo container
-- Postgres; POSTGRES_DB só cria o banco "orderdb" (usado por order-service)
-- na inicialização, então criamos o segundo aqui.
CREATE DATABASE paymentdb;
