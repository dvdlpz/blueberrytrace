# BlueberryTrace

Plataforma interna de **Vivero Los Viñedos** para control, clasificación y trazabilidad de plantas de arándano. Integra lotes físicos, camas, lotes trazables, siembra, uniformización, formalización, clasificación, despacho, mermas, auditoría, reportes técnicos y administración de usuarios/roles.

## Arquitectura

- **Frontend:** React + Vite + TypeScript, distribuido como archivos estáticos por Nginx.
- **Backend:** Java 21 + Spring Boot + Spring Security + JPA/Hibernate.
- **Datos:** MySQL 8.4 con volumen persistente.
- **Producción principal:** Docker Compose en VPS Linux con Nginx como único punto de exposición pública.
- **Alternativa Pterodactyl:** un JAR Java 21 con React integrado, pensado para un servidor del panel que no dispone de Docker Compose propio.

La arquitectura productiva usa un único origen:

```text
https://app.dominio.com/        frontend
https://app.dominio.com/api/    API Spring Boot por proxy inverso
```

MySQL y Spring Boot no exponen puertos al exterior. El backend se comunica con `mysql` y Nginx se comunica con `backend` usando nombres de servicio Docker, nunca `localhost` dentro de contenedores.

## Seguridad implementada

- Inicio de sesión por usuario o correo, con BCrypt, sesión y CSRF.
- Sin registro público: solamente `ADMINISTRADOR` crea, edita o activa usuarios.
- Roles fijos: `ADMINISTRADOR`, `SUPERVISOR`, `OPERARIO`, `CONTROL_CALIDAD` y `CONSULTA`, con matriz de permisos consultable y aplicable desde backend.
- CORS restringido por variable de entorno.
- Cookies seguras para HTTPS en perfil de producción.
- Errores API amistosos sin stack traces, hashes ni secretos.
- Validación de imágenes de perfil PNG/JPG/WEBP por MIME, Base64, tamaño y firma binaria.
- Unidad de lote trazable independiente de la cama; validación de cadena, mermas y saldo para evitar cantidades inconsistentes entre siembra, procesos, clasificación y despacho.
- Sin credenciales reales, tokens o llaves privadas versionadas.

## Inicio rápido local

### Frontend

```bash
npm ci
cp frontend/.env.example frontend/.env
npm run build
npm run frontend:dev
```

El build se ejecuta con `npm run build` desde la raíz o `npm run build` dentro de `frontend/`.

### Backend

Configura variables locales a partir de `backend/.env.example`. El backend exige una base MySQL local y se ejecuta con:

```bash
cp backend/.env.example backend/.env
npm run backend:run
```

`npm run backend:run` lee `backend/.env` de forma segura y prioriza el archivo local sobre valores obsoletos exportados en la terminal. No uses `source backend/.env`: una URL JDBC contiene `&` y algunos valores pueden contener espacios, por lo que Bash podría interpretarlos como comandos.

Para pruebas y empaquetado se usa Maven Wrapper:

```bash
./mvnw -pl backend test
./mvnw -pl backend clean package
```

No existe una cuenta por defecto. En una base vacía, habilita temporalmente `BLUEBERRYTRACE_BOOTSTRAP_ADMIN_ENABLED=true` y completa las variables `BLUEBERRYTRACE_BOOTSTRAP_ADMIN_*`; después de crear la cuenta, vuelve a `false`.

## Producción con Docker

1. Copia `.env.example` a `.env` únicamente en el VPS y configura contraseñas seguras.
2. Revisa la composición:

```bash
docker compose --env-file .env config
```

3. Inicia con plantilla HTTP para obtener certificado:

```bash
docker compose --env-file .env up -d --build
```

4. Emite certificado, cambia `NGINX_CONFIG` por la plantilla HTTPS y recrea `frontend`.

Las instrucciones exactas están en [docs/despliegue-vps.md](docs/despliegue-vps.md).

### Pterodactyl con un único JAR Java 21

Para un servidor Pterodactyl con Egg Java 21 y MySQL asignado, React se integra dentro de Spring Boot y se genera un solo artefacto:

```bash
npm run package:pterodactyl
```

El resultado es `artifacts/pterodactyl/blueberrytrace.jar`. La guía específica, incluida la configuración privada de la base y el comando de inicio, está en [docs/despliegue-pterodactyl.md](docs/despliegue-pterodactyl.md).

## Reportes técnicos

El módulo de reportes genera vista previa y exportación real a PDF/XLSX. Cada informe incorpora identidad institucional, usuario generador, fecha/hora, período, filtros, estado de información, métricas calculadas, tabla detallada, totales, observaciones basadas en datos disponibles y pie de confidencialidad. Cuando no hay datos, el reporte declara explícitamente “Sin información disponible”.

## Estructura relevante

```text
backend/
  db/init/01-schema.sql          esquema para volumen nuevo
  db/init/02-app-user-privileges.sh  reducción de permisos de la cuenta de aplicación
  src/main/resources/db/migration/ migraciones Flyway versionadas
  Dockerfile
  src/main/resources/application-local.properties
  src/main/resources/application-prod.properties
frontend/
  Dockerfile
  .env.example
  .env.production
deploy/nginx/templates/          plantillas HTTP y HTTPS
scripts/backup-db.sh
scripts/restore-db.sh
scripts/migrate-db.sh
docs/despliegue-vps.md
docs/backup-restore.md
docs/monitoreo.md
docs/mantenimiento.md
docs/rollback.md
docs/validaciones-realizadas.md
docs/modelo-trazabilidad.md
docs/roles-permisos.md
docs/flujo-operativo.md
docs/auditoria.md
docs/migracion-datos.md
docs/pruebas-operativas.md
docker-compose.yml
.env.example
```

## Operación

```bash
# Estado y logs
docker compose --env-file .env ps
docker compose --env-file .env logs -f --tail=200

# Backup
bash scripts/backup-db.sh

# Restauración explícita
RESTORE_CONFIRM=YES bash scripts/restore-db.sh /ruta/segura/backup.sql.gz

# Migración de una base existente, solo tras backup verificado
MIGRATION_CONFIRM=YES bash scripts/migrate-db.sh
```

Consulta también:

- [Guía de despliegue VPS](docs/despliegue-vps.md)
- [Corrección MySQL 8.4](docs/solucion-mysql-8.4.md)
- [Backup y restauración](docs/backup-restore.md)
- [Monitoreo](docs/monitoreo.md)
- [Mantenimiento](docs/mantenimiento.md)
- [Rollback](docs/rollback.md)
- [Plan de pruebas](docs/pruebas-produccion.md)
- [Entornos local, Docker y VPS](docs/entornos.md)
- [Validaciones realizadas](docs/validaciones-realizadas.md)
- [Interfaz corporativa y permisos](docs/interfaz-corporativa.md)

## Experiencia operativa

Los formularios, ventanas y estados vacíos están diseñados para orientar al equipo operativo con acciones claras, sin mensajes de implementación. El panel principal propone el siguiente paso de la cadena de trazabilidad según los registros disponibles y los permisos del perfil activo.

Consulta [docs/experiencia-usuario.md](docs/experiencia-usuario.md) para conocer los criterios de interacción y la guía de avance operativo.

## Integridad operativa

La cadena de cantidades, los registros históricos y el procedimiento de normalización se documentan en [`docs/integridad-operativa.md`](docs/integridad-operativa.md).


## Flujo operativo real
El modelo operativo incorpora **jabas de siembra**, riegos programados, recuperación por riego, formalización de jabas completas con orden físico, pedidos por variedad, empaques y cargas de tráiler antes del despacho.


BlueberryTrace controla la estructura física **invernadero → cama → jaba → macetas** y el recorrido operativo **siembra → riego → uniformización ↔ formalización → clasificación → empaque → carga de tráiler → despacho**. Las plantas secas ingresan a recuperación por riego; las cargas reúnen líneas de un pedido por variedad, y la salida descuenta el saldo disponible al confirmar el tráiler.

Consulta:

- [`docs/flujo-vivero-real.md`](docs/flujo-vivero-real.md)
- [`docs/implementacion-flujo-real.md`](docs/implementacion-flujo-real.md)

