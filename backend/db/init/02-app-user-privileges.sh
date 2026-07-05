#!/usr/bin/env bash

# Este archivo es ejecutado mediante "source" por MySQL.
# El subshell evita modificar las opciones internas del entrypoint oficial.
(
  set -eo pipefail

  : "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
  : "${MYSQL_USER:?MYSQL_USER is required}"
  : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"
  : "${MYSQL_MIGRATION_USER:?MYSQL_MIGRATION_USER is required}"
  : "${MYSQL_MIGRATION_PASSWORD:?MYSQL_MIGRATION_PASSWORD is required}"

  mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER IF NOT EXISTS '${MYSQL_MIGRATION_USER}'@'%' IDENTIFIED BY '${MYSQL_MIGRATION_PASSWORD}';
ALTER USER '${MYSQL_MIGRATION_USER}'@'%' IDENTIFIED BY '${MYSQL_MIGRATION_PASSWORD}';

GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_MIGRATION_USER}'@'%';

REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${MYSQL_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';

FLUSH PRIVILEGES;
SQL
)
