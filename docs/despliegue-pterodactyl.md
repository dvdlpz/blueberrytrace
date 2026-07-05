# Despliegue de BlueberryTrace en Pterodactyl con Java 21

Esta guía usa la arquitectura integrada preparada para el servidor **Web Keraunen**: React/Vite se compila dentro del JAR y Spring Boot sirve tanto la interfaz como la API. En Pterodactyl se sube un único artefacto Java y la base de datos se crea desde la pestaña **Databases**.

```text
Navegador
   │  IP:PUERTO o dominio HTTPS
   ▼
Pterodactyl · Java 21 · blueberrytrace.jar
   ├── React estático integrado
   ├── Spring Boot /api/v1
   └── Sesión, seguridad y Flyway
             │
             ▼
    MySQL/MariaDB administrado por el panel
```

## 1. Requisitos que debe confirmar el administrador del VPS

No despliegues en el Egg actual de Node.js. El servidor debe cambiarse a un Egg o imagen Docker con **Java 21**, permitiendo iniciar un JAR. También se necesita una base MySQL/MariaDB creada para este servidor.

Solicita estos valores, sin compartir contraseñas en capturas:

- Imagen/egg Java 21.
- Puerto de asignación del servidor; Pterodactyl lo expone como `{{SERVER_PORT}}` en el comando de inicio.
- Host, puerto, base, usuario y contraseña de la base de datos creada desde el panel.
- Memoria mínima práctica: 2 GB. La interfaz y el backend comparten un solo proceso Java.
- Para producción real: un subdominio y un proxy HTTPS administrado por el dueño del VPS.

## 2. Construir el paquete en tu equipo

Desde la raíz del proyecto, con Node.js, Java 21 y acceso a Maven Central:

```bash
npm run package:pterodactyl
```

El proceso instala dependencias limpias, compila React, ejecuta las pruebas Maven y genera:

```text
artifacts/pterodactyl/blueberrytrace.jar
artifacts/pterodactyl/config/application-pterodactyl.properties.example
```

No subas `node_modules`, `frontend/dist`, el código fuente completo, `.env`, bases de datos ni el ZIP original al panel.

## 3. Preparar la configuración privada

En **Files** crea la carpeta `config`. Sube el archivo de ejemplo y renómbralo a:

```text
/home/container/config/application-pterodactyl.properties
```

Edita únicamente esa copia privada. Completa las credenciales que muestra la base creada desde Pterodactyl:

```properties
DB_URL=jdbc:mysql://HOST:PUERTO/BASE?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
DB_USERNAME=USUARIO
DB_PASSWORD=CONTRASENA
ALLOWED_EMAIL_DOMAINS=vlv.agro.pe
```

La interfaz y la API comparten dirección, por lo que `CORS_ALLOWED_ORIGINS` puede permanecer vacío. El ejemplo JDBC usa `useSSL=false` porque normalmente la conexión se realiza dentro de la red privada administrada del panel; cámbialo solamente si el administrador de la base confirma TLS y entrega la configuración correspondiente. No reemplaces `spring.jpa.hibernate.ddl-auto=validate` ni desactives Flyway.

Para la primera cuenta administrativa, establece temporalmente:

```properties
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_ENABLED=true
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_USERNAME=administrador
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_EMAIL=tu.correo@vlv.agro.pe
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_PASSWORD=UNA_CONTRASENA_DE_12_O_MAS_CARACTERES
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_FULL_NAME=Nombre completo
BLUEBERRYTRACE_BOOTSTRAP_ADMIN_POSITION=Administrador del sistema
```

Después del primer inicio correcto, cambia `BLUEBERRYTRACE_BOOTSTRAP_ADMIN_ENABLED=false` y elimina la contraseña de bootstrap del archivo.

## 4. Subir el JAR

En **Files** sube únicamente:

```text
/home/container/blueberrytrace.jar
```

La carpeta final debe verse así:

```text
/home/container/
├── blueberrytrace.jar
└── config/
    └── application-pterodactyl.properties
```

## 5. Comando de inicio

El Egg Java debe iniciar el artefacto con un comando equivalente a este:

```bash
java -XX:MaxRAMPercentage=70.0 -Dspring.profiles.active=pterodactyl -Dserver.port={{SERVER_PORT}} -jar blueberrytrace.jar
```

`{{SERVER_PORT}}` debe ser reemplazado por Pterodactyl según la asignación del servidor. No uses `index.js`, `npm start`, ni el Egg Node.js.

Si el panel no permite editar el comando, envíale el comando al administrador para que lo configure en el Egg Java 21.

## 6. Primer inicio y verificación

Inicia el servidor desde **Console** y espera los mensajes de Flyway y Spring Boot. Debe quedar escuchando en la asignación mostrada por el panel.

Pruebas mínimas:

1. Abre `http://IP:PUERTO/` y confirma que carga el login.
2. Abre `http://IP:PUERTO/api/v1/health` y confirma una respuesta con estado `UP`.
3. Inicia sesión con la cuenta bootstrap.
4. Registra una operación de prueba autorizada y confirma que persiste tras reiniciar el servidor.
5. Desactiva bootstrap y reinicia.

Una asignación directa por IP normalmente usa HTTP. Para esa prueba inicial mantiene `COOKIE_SECURE=false`. Antes de usar datos reales por Internet, solicita un dominio HTTPS y cambia:

```properties
COOKIE_SECURE=true
COOKIE_SAME_SITE=strict
```

## 7. Actualización posterior

1. Genera un nuevo JAR con `npm run package:pterodactyl`.
2. Detén el servidor en el panel.
3. Conserva una copia del JAR anterior fuera del panel.
4. Reemplaza `/home/container/blueberrytrace.jar`.
5. No elimines `config/application-pterodactyl.properties`.
6. Inicia y revisa Console; Flyway ejecutará solo migraciones nuevas.
7. Verifica `/api/v1/health`, login y una operación crítica.

Antes de una actualización con datos reales, solicita o realiza un backup probado de MySQL. El panel Pterodactyl no reemplaza una estrategia de backup de base de datos.

## Límites de esta modalidad

La modalidad de JAR único es adecuada para el panel que fue asignado y reduce la complejidad de mantener Node/Nginx separados. No sustituye un proxy HTTPS, monitoreo del host, backups ni control del acceso administrativo. Para un dominio público estable, el dueño del VPS debe publicar el puerto mediante proxy inverso y certificado TLS.
