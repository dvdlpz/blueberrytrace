# Monitoreo de BlueberryTrace

## Señales mínimas

| Componente | Señal | Comando / URL |
|---|---|---|
| Backend Spring Boot | salud disponible | `https://app.dominio.com/api/v1/health` |
| MySQL | healthcheck Docker | `docker compose --env-file .env ps` |
| Nginx/frontend | healthcheck interno | `docker compose --env-file .env ps` |
| Aplicación | login, rutas protegidas y reportes | prueba funcional por rol |
| Recursos VPS | CPU, RAM, disco | `htop`, `free -h`, `df -h` |

El Actuator del backend expone solamente `health`; no publica métricas de negocio ni detalles sensibles.

## Comandos de revisión

```bash
cd /srv/blueberrytrace

docker compose --env-file .env ps
docker compose --env-file .env logs --tail=200 backend
docker compose --env-file .env logs --tail=200 frontend
docker compose --env-file .env logs --tail=200 mysql
curl -fsS https://app.dominio.com/api/v1/health
```

Para observar logs en vivo:

```bash
docker compose --env-file .env logs -f --tail=200 backend frontend mysql
```

## Interpretación y acción inicial

- **Backend unhealthy:** revisa primero `backend`; confirma variables DB, conectividad interna `mysql`, migración y credenciales. No expongas 8080 para diagnosticar.
- **MySQL unhealthy:** revisa espacio de disco, logs y volumen `mysql_data`; no elimines el volumen sin un backup validado.
- **Frontend unhealthy o 502 en `/api`:** revisa que `backend` esté healthy y que `NGINX_CONFIG` corresponda a HTTP/HTTPS según el certificado.
- **Errores 401:** verificar sesión, cookie HTTPS y token CSRF; no desactives CSRF para resolverlo.
- **Errores 403:** revisar la autoridad del usuario en backend; no resolverlos agregando permisos globales.
- **Aumento de errores 5xx:** toma una copia de logs, registra hora UTC/Perú, ruta y usuario afectado sin incluir contraseñas o cookies.

## Revisión periódica

Diaria: healthchecks, backup y espacio libre.  
Semanal: logs de errores, actualizaciones de seguridad Ubuntu/Docker y prueba de login.  
Mensual: restauración de prueba, revisión de roles activos y rotación de credenciales si hay una incidencia.

## Registro de incidentes

Para cada incidente conserva: fecha/hora, servicio, síntoma, impacto, acciones, backup disponible, versión/commit, resolución y causa raíz. No adjuntes `.env`, hashes de contraseña, tokens CSRF ni cookies a tickets públicos.
