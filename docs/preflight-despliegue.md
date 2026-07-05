# Preflight antes del despliegue en VPS

Este checklist se ejecuta antes de arrancar BlueberryTrace en el VPS. No reemplaza las pruebas operativas, el backup ni la revisión de seguridad.

## 1. Preparar variables privadas

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

No uses `CHANGE_ME`, `example.com` ni contraseñas temporales. Para la cuenta de aplicación, `DB_USERNAME` debe coincidir con `MYSQL_USER` y `DB_PASSWORD` con `MYSQL_PASSWORD`.

## 2. Validar configuración

```bash
npm run deploy:preflight
```

El comando verifica archivos requeridos, valores críticos, coincidencia de credenciales de aplicación, CORS con el dominio configurado y la sintaxis resuelta por Docker Compose. No imprime secretos.

## 3. Primera ejecución

Con la plantilla HTTP activa:

```bash
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
curl -fsS http://127.0.0.1/api/v1/health
```

Luego solicita el certificado y cambia `NGINX_CONFIG` a la plantilla HTTPS según `docs/despliegue-vps.md`.

## 4. Después de crear la cuenta inicial

Si se usó el bootstrap administrativo, cambia `BOOTSTRAP_ADMIN_ENABLED=false`, elimina sus secretos de `.env` y recrea el backend. La cuenta inicial no debe quedar habilitada para ejecuciones posteriores.

## 5. Validación operativa previa a producción

Prueba en el navegador: inicio de sesión, permisos por rol, alta de una operación permitida, trazabilidad, pedido, empaque, carga de tráiler, despacho y exportación de reporte. Realiza también un backup probado antes de migrar una base existente.
