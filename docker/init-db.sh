#!/bin/bash
set -e

# O container Postgres já cria o banco definido em POSTGRES_DB (hospital_scheduling).
# Este script cria os outros dois bancos, um por serviço, na mesma instância do Postgres —
# uma concessão de custo/operação para o desafio: bancos logicamente isolados (cada serviço
# só acessa o seu), mas compartilhando um único container em vez de três.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE hospital_notification;
    CREATE DATABASE hospital_history;
EOSQL
