# Backup y restauración de MySQL

## Política mínima

- Backup lógico diario, retenido al menos 14 días.
- Una copia adicional cifrada fuera del VPS o en almacenamiento institucional.
- Probar restauración al menos una vez al mes en un entorno aislado.
- No guardar backups dentro de `/var/www`, la carpeta del frontend ni rutas públicas.

Los scripts usan el servicio Docker `mysql`; MySQL no se expone al exterior.

## Crear un backup manual

Desde la raíz del proyecto, con `.env` privado configurado:

```bash
bash scripts/backup-db.sh
```

Por defecto el archivo se guarda en:

```text
$HOME/blueberrytrace-backups/blueberrytrace_<base>_<fecha>.sql.gz
```

Puedes elegir una ruta segura y retención distinta:

```bash
BACKUP_DIR=/srv/backups/blueberrytrace BACKUP_RETENTION_DAYS=30 bash scripts/backup-db.sh
```

El script asigna permisos restrictivos al directorio y a cada archivo. Comprueba integridad básica:

```bash
gzip -t /ruta/segura/archivo.sql.gz
```

## Programar backup diario con cron

Crea la ruta privada y permite acceso solo al usuario de despliegue:

```bash
sudo install -d -m 700 -o blueberry -g blueberry /srv/backups/blueberrytrace
crontab -e
```

Agrega una tarea diaria, por ejemplo a las 02:15:

```cron
15 2 * * * cd /srv/blueberrytrace && BACKUP_DIR=/srv/backups/blueberrytrace BACKUP_RETENTION_DAYS=14 /bin/bash scripts/backup-db.sh >> /home/blueberry/blueberrytrace-backup.log 2>&1
```

Revisa periódicamente el log y el espacio disponible:

```bash
tail -n 100 /home/blueberry/blueberrytrace-backup.log
df -h /srv/backups
```

## Restaurar un backup

> **Advertencia:** una restauración sobrescribe o mezcla datos con la base actual según el contenido del SQL. Siempre crea un backup nuevo antes y detén el acceso de usuarios si la recuperación es de producción.

1. Comunica la ventana de mantenimiento y realiza un backup adicional.
2. Verifica el archivo con `gzip -t`.
3. Detén frontend y backend para evitar escrituras durante la recuperación:

```bash
cd /srv/blueberrytrace
docker compose --env-file .env stop frontend backend
```

4. Ejecuta la restauración con confirmación explícita:

```bash
RESTORE_CONFIRM=YES bash scripts/restore-db.sh /srv/backups/blueberrytrace/archivo.sql.gz
```

5. Levanta los servicios y revisa salud:

```bash
docker compose --env-file .env up -d backend frontend
docker compose --env-file .env logs --tail=150 backend
curl -fsS https://app.dominio.com/api/v1/health
```

## Prueba mensual de restauración

No pruebes la restauración sobre la base productiva. En un VPS de prueba o equipo aislado:

1. Copia un backup cifrado o por canal seguro.
2. Usa un nombre de base y un `.env` de prueba distinto.
3. Inicia MySQL aislado y ejecuta `restore-db.sh` apuntando a ese `.env`.
4. Valida conteos de lotes, camas, siembras, procesos, clasificación y despachos.
5. Registra fecha, responsable, duración y resultado en la bitácora de mantenimiento.

## Recuperación ante pérdida del VPS

Un backup local no protege contra pérdida total del servidor. Copia periódicamente los `.sql.gz` a almacenamiento externo cifrado y conserva también el archivo de versión/release desplegado. Para recuperar:

1. Provisiona un VPS nuevo.
2. Despliega la misma versión etiquetada de BlueberryTrace.
3. Configura `.env` nuevo con credenciales seguras.
4. Levanta MySQL, restaura el backup y valida salud.
5. Actualiza DNS si cambió la IP.
