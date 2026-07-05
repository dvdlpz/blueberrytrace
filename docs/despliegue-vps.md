# Despliegue seguro de BlueberryTrace en un VPS Linux

Esta guía aplica a Ubuntu 22.04 LTS o 24.04 LTS. La arquitectura elegida usa **un solo dominio**:

- `https://app.dominio.com/` para React/Vite estático.
- `https://app.dominio.com/api/` como proxy inverso hacia Spring Boot.

Esta alternativa evita CORS entre subdominios, simplifica las cookies de sesión y mantiene el backend y MySQL fuera de Internet. MySQL no publica puertos y el backend solo está en la red Docker privada.

## 1. Requisitos del VPS

Recomendado para una instalación inicial:

- Ubuntu 22.04/24.04 actualizado.
- 2 vCPU, 4 GB RAM y 30 GB SSD como mínimo práctico.
- IP pública estática.
- Dominio o subdominio administrable por DNS.
- Acceso SSH con llave para el administrador.

Nunca subas al repositorio claves SSH, contraseñas, archivos `.env` reales ni certificados.

## 2. Crear usuario de despliegue y endurecer acceso

Ingresa inicialmente como root o con un usuario con sudo. Reemplaza `blueberry` por el usuario que usará la organización:

```bash
sudo adduser blueberry
sudo usermod -aG sudo blueberry
sudo install -d -m 700 -o blueberry -g blueberry /home/blueberry/.ssh
sudo nano /home/blueberry/.ssh/authorized_keys
sudo chown blueberry:blueberry /home/blueberry/.ssh/authorized_keys
sudo chmod 600 /home/blueberry/.ssh/authorized_keys
```

Verifica el acceso con llave en otra terminal antes de cerrar la sesión actual. Después, en `/etc/ssh/sshd_config`, deshabilita el acceso root por contraseña según la política de TI y reinicia SSH:

```bash
sudo systemctl reload ssh
```

## 3. Instalar Docker Engine y Compose

Instala desde el repositorio oficial de Docker:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker blueberry
```

Cierra y abre sesión con el usuario `blueberry`, luego valida:

```bash
docker --version
docker compose version
```

## 4. Firewall UFW

Solo expón SSH, HTTP y HTTPS. No abras 3306 ni 8080:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status verbose
```

Si SSH usa un puerto diferente, permite ese puerto antes de habilitar UFW.

## 5. DNS y dominio

En el proveedor DNS crea un registro `A`:

```text
app.dominio.com -> IP_PUBLICA_DEL_VPS
```

Espera a que resuelva desde Internet antes de solicitar el certificado. Confirma en el VPS:

```bash
getent hosts app.dominio.com
```

## 6. Cargar el proyecto y crear variables privadas

Con el usuario de despliegue:

```bash
sudo mkdir -p /srv/blueberrytrace
sudo chown blueberry:blueberry /srv/blueberrytrace
cd /srv/blueberrytrace
# Clona desde tu repositorio privado o sube el proyecto por un canal seguro.
git clone URL_PRIVADA_DEL_REPOSITORIO .
cp .env.example .env
chmod 600 .env
nano .env
```

Antes de iniciar, valida que las variables no tengan valores de ejemplo y que Docker Compose pueda resolverlas:

```bash
npm run deploy:preflight
```

Completa exclusivamente en el VPS los siguientes valores privados:

- `APP_DOMAIN`: por ejemplo `app.dominio.com`.
- `MYSQL_PASSWORD` y `DB_PASSWORD`: la misma contraseña larga para la cuenta restringida de la aplicación; `MYSQL_ROOT_PASSWORD`: una contraseña distinta, solo administrativa.
- `CORS_ALLOWED_ORIGINS`: `https://app.dominio.com`.
- `ALLOWED_EMAIL_DOMAINS`: dominio corporativo permitido, por ejemplo `vlv.agro.pe`.
- `BOOTSTRAP_ADMIN_*`: la cuenta inicial de administración; usa una contraseña de 12 o más caracteres.

Para crear la primera cuenta, establece `BOOTSTRAP_ADMIN_ENABLED=true`. No reutilices usuarios de prueba ni contraseñas conocidas.

## 7. Primera ejecución HTTP y cuenta administrativa

El primer inicio usa la plantilla HTTP para que Let's Encrypt pueda validar el dominio:

> Compatibilidad MySQL 8.4: el `docker-compose.yml` no incluye
> `--default-authentication-plugin=caching_sha2_password`, ya que MySQL 8.4
> ya usa `caching_sha2_password` como valor predeterminado. Consulta
> `docs/solucion-mysql-8.4.md` si una inicialización nueva falló.

```bash
cd /srv/blueberrytrace
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 backend frontend mysql
```

Cuando la cuenta administrativa se cree correctamente, edita `.env`, cambia `BOOTSTRAP_ADMIN_ENABLED=false` y elimina del archivo los valores de contraseña de bootstrap que ya no sean necesarios. Luego recrea solo el backend:

```bash
docker compose --env-file .env up -d --force-recreate backend
```

## 8. Certificado TLS con Certbot

Con DNS resuelto y el puerto 80 accesible:

```bash
cd /srv/blueberrytrace
read -r -p "Correo para avisos de renovación: " CERTBOT_EMAIL
docker compose --env-file .env --profile certbot run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --email "$CERTBOT_EMAIL" --agree-tos --no-eff-email \
  -d "$(grep '^APP_DOMAIN=' .env | cut -d= -f2-)"
```

Cambia la variable de plantilla en `.env`:

```dotenv
NGINX_CONFIG=./deploy/nginx/templates/blueberrytrace-https.conf.template
```

Recrea el proxy:

```bash
docker compose --env-file .env up -d --force-recreate frontend
docker compose --env-file .env exec frontend nginx -t
```

Verifica desde un navegador y desde el VPS:

```bash
curl -I https://app.dominio.com/
curl -fsS https://app.dominio.com/api/v1/health
```

## 9. Renovación automática del certificado

Prueba primero una renovación simulada:

```bash
docker compose --env-file .env --profile certbot run --rm certbot renew --dry-run
```

Programa cron del usuario `blueberry` con `crontab -e`:

```cron
0 3 * * * cd /srv/blueberrytrace && /usr/bin/docker compose --env-file .env --profile certbot run --rm certbot renew --webroot -w /var/www/certbot && /usr/bin/docker compose --env-file .env exec -T frontend nginx -s reload >> /home/blueberry/blueberrytrace-certbot.log 2>&1
```

## 10. Verificación final

```bash
cd /srv/blueberrytrace
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=200 backend
docker compose --env-file .env logs --tail=200 frontend
curl -fsS https://app.dominio.com/api/v1/health
```

Prueba manualmente el login por usuario y correo, carga de imagen de perfil válida, permisos por rol, una operación de cada módulo y la descarga de un reporte PDF/XLSX. Para una base que ya existía antes de esta versión, realiza un backup probado y ejecuta `MIGRATION_CONFIRM=YES bash scripts/migrate-db.sh` antes de arrancar con el perfil `prod`.

## Operación diaria

```bash
# Estado y logs
docker compose --env-file .env ps
docker compose --env-file .env logs -f --tail=200

# Reinicio controlado
docker compose --env-file .env restart

# Actualización con compilación
 git pull --ff-only
 docker compose --env-file .env up -d --build

# Backup y restauración
bash scripts/backup-db.sh
RESTORE_CONFIRM=YES bash scripts/restore-db.sh /ruta/segura/backup.sql.gz
```

Consulta `backup-restore.md`, `monitoreo.md`, `mantenimiento.md` y `rollback.md` antes de intervenir producción.
