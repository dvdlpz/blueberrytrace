# Plan de validación funcional y de seguridad

Las siguientes pruebas deben ejecutarse en una base de datos de ensayo antes de producción y repetirse después de cada actualización crítica. No se marcan como aprobadas por estar documentadas: el responsable debe registrar fecha, usuario, ambiente y resultado.

| ID | Caso | Resultado esperado |
|---|---|---|
| AUTH-01 | Login válido por usuario | Se crea sesión y se carga el panel autorizado. |
| AUTH-02 | Login válido por correo | Se crea sesión con la misma cuenta. |
| AUTH-03 | Login inválido | Respuesta 401 amistosa sin indicar qué credencial falló. |
| AUTH-04 | Ruta sin sesión | Respuesta 401 JSON; no se filtran datos. |
| AUTH-05 | Solicitud mutante sin token CSRF | Rechazo 403. |
| AUTH-06 | Usuario `CONSULTA` intenta crear/modificar | Rechazo 403 desde backend. |
| AUTH-07 | `OPERARIO` intenta gestionar usuarios o despacho | Rechazo 403 desde backend. |
| AUTH-08 | `CONTROL_CALIDAD` registra clasificación/despacho | Permitido según su rol. |
| AUTH-09 | Último administrador activo se desactiva | Rechazo con mensaje claro. |
| DATA-01 | Código de lote duplicado | Rechazo controlado, sin error SQL expuesto. |
| DATA-02 | Código de cama duplicado | Rechazo controlado, sin error SQL expuesto. |
| DATA-03 | Siembra mayor que capacidad de cama | Rechazo controlado. |
| DATA-04 | Uniformización mayor que inicial o siembra | Rechazo controlado. |
| DATA-05 | Formalización mayor que uniformización | Rechazo controlado. |
| DATA-06 | Clasificación mayor que formalización | Rechazo controlado. |
| DATA-07 | Despacho `DESPACHADO` mayor que clasificación `VALIDADA` | Rechazo controlado. |
| DATA-08 | Fecha operacional anterior al lote | Rechazo controlado. |
| DATA-09 | Eliminar etapa con etapas posteriores activas | Rechazo controlado para conservar consistencia. |
| TRACE-01 | Trazabilidad de lote | Conteos reflejan los registros persistidos. |
| REPORT-01 | Reporte con datos | Vista previa muestra usuario, período, filtros, métricas, detalle, totales y observaciones derivadas. |
| REPORT-02 | Reporte sin datos | Estado y detalle muestran “Sin información disponible”; no se inventan hallazgos. |
| REPORT-03 | XLSX | Archivo abre como `.xlsx` real, no HTML renombrado. |
| REPORT-04 | PDF | Archivo contiene encabezado institucional, tabla, totales y confidencialidad. |
| PROFILE-01 | Imagen PNG/JPG/WEBP válida menor a 1 MB | Se acepta y se muestra al actualizar perfil. |
| PROFILE-02 | Extensión/MIME falso, datos no imagen o mayor a 1 MB | Rechazo desde backend. |
| DEPLOY-01 | `/api/v1/health` | Respuesta exitosa a través de HTTPS. |
| DEPLOY-02 | MySQL y backend | No existen puertos 3306/8080 publicados en el VPS. |
| DEPLOY-03 | Renovación Certbot | `renew --dry-run` finaliza correctamente. |
| BACKUP-01 | Backup | Se genera `.sql.gz` fuera de la carpeta pública y pasa `gzip -t`. |
| BACKUP-02 | Restauración de ensayo | La base aislada restaura y supera pruebas básicas de lectura. |

## Evidencia mínima

Conserva capturas o logs de:

```bash
npm run build
./mvnw -pl backend test
./mvnw -pl backend clean package
docker compose --env-file .env config
docker compose --env-file .env ps
curl -fsS https://app.dominio.com/api/v1/health
```

No incluyas cookies, token CSRF, archivos `.env` ni contraseñas en la evidencia.
