# Corrección de MySQL 8.4

## Motivo

BlueberryTrace usa MySQL 8.4. En esta versión, `caching_sha2_password` ya es el mecanismo de autenticación predeterminado y la opción de servidor `default-authentication-plugin` ya no existe.

Por esa razón, el servicio no debe iniciarse con:

```text
--default-authentication-plugin=caching_sha2_password
```

El archivo `docker-compose.yml` ya fue corregido para eliminar esa opción.

## Recuperación de una instalación nueva que falló

Si MySQL falló antes de estar operativo y todavía no existen datos reales, la inicialización puede quedar incompleta. Detén los servicios, elimina solo el volumen de MySQL y vuelve a iniciarlos:

```bash
docker compose --env-file .env down
docker volume rm blueberrytrace_mysql_data
npm run docker:up
```

No elimines el volumen si ya contiene información que necesitas conservar. En ese caso, realiza primero un backup y revisa el procedimiento de restauración antes de intervenir.

## Verificación

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 mysql backend frontend
curl -fsS http://127.0.0.1/nginx-health
```

El endpoint debe responder `UP` y los servicios `mysql`, `backend` y `frontend` deben quedar en estado `healthy` o `running`.
