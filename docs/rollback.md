# Rollback de BlueberryTrace

## Cuándo aplicar rollback

Aplica rollback si el healthcheck falla, hay errores funcionales críticos, migración incompatible o impacto de seguridad. No uses rollback para ocultar logs; conserva evidencia del incidente.

## Rollback de aplicación sin cambios de esquema

1. Identifica el commit/release anterior en `.deployed-commit` o Git.
2. Revisa que exista un backup reciente.
3. Vuelve al release aprobado y reconstruye:

```bash
cd /srv/blueberrytrace
git checkout COMMIT_O_TAG_ANTERIOR
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=150 backend frontend
```

4. Comprueba `https://app.dominio.com/api/v1/health`, login y rutas críticas.

## Rollback con cambios de esquema

Una migración de base de datos puede no tener reversión automática. Antes de restaurar:

1. Detén frontend y backend para impedir nuevas escrituras.
2. Recupera el backup hecho antes de la migración.
3. Despliega el commit compatible con ese esquema.
4. Ejecuta validación funcional completa.

```bash
cd /srv/blueberrytrace
docker compose --env-file .env stop frontend backend
RESTORE_CONFIRM=YES bash scripts/restore-db.sh /ruta/segura/backup-pre-migracion.sql.gz
git checkout COMMIT_COMPATIBLE
docker compose --env-file .env up -d --build
```

## Certificado o Nginx

Si falla HTTPS después de una actualización, vuelve temporalmente a la plantilla HTTP solo para diagnóstico interno si el dominio sigue controlado, corrige el certificado y restaura la plantilla HTTPS. No dejes la producción en HTTP.

## Cierre del incidente

Registra versión afectada, versión restaurada, backup usado, validaciones realizadas, causa y acción preventiva. Actualiza el procedimiento antes de reintentar el despliegue.
