#!/bin/bash
set -e

# Cria um banco por bounded context (sem banco compartilhado entre serviços).
# Lista vem de POSTGRES_MULTIPLE_DATABASES, ex: "inscricoes,partidas,ranking"

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
  echo "Criando múltiplos bancos: $POSTGRES_MULTIPLE_DATABASES"
  IFS=',' read -ra DBS <<< "$POSTGRES_MULTIPLE_DATABASES"
  for db in "${DBS[@]}"; do
    echo "  -> $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
      SELECT 'CREATE DATABASE ${db}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${db}')\gexec
EOSQL
  done
fi