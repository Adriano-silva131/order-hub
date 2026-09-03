-- auth-service usa um banco separado (authdb) no mesmo container Postgres;
-- POSTGRES_DB só cria "orderdb" na inicialização, então criamos aqui.
CREATE DATABASE authdb;
