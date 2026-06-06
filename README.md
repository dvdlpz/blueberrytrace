# BlueberryTrace

**BlueberryTrace** es un sistema web interno para el control, clasificación y trazabilidad de plantas de arándano destinadas a exportación en el área de frutales de **Vivero Los Viñedos**.

Este repositorio fue organizado para el **Avance 2 del curso Herramientas de Desarrollo**. Por eso, además del código fuente, incluye evidencias de uso de Git, GitHub, ramas, issues, pull requests, integración continua y documentación técnica.

## Módulos principales

| Módulo | Estado | Descripción |
|---|---|---|
| Autenticación | Implementado | Login, logout y usuarios internos. |
| Usuarios y roles | Implementado | Gestión base de cuentas y perfiles. |
| Camas / invernaderos | Implementado | Registro de estructura física del vivero. |
| Lotes | Implementado | Registro y seguimiento de lotes de arándano. |
| Procesos productivos | Implementado | Siembra, uniformización, formalización, clasificación y despacho. |
| Reportes | Implementado | Consulta de trazabilidad por lote, etapa y estado. |
| Auditoría | Base implementada | Registro y consulta de acciones relevantes del sistema. |
| CI/CD | Documentado | Workflow de GitHub Actions para compilar el proyecto. |
| Docker | Documentado | Archivos base para ejecución reproducible. |

## Flujo de trazabilidad

```text
Invernadero → Cama → Lote → Siembra → Uniformización → Formalización → Clasificación → Despacho → Reportes
```

Cada registro operativo se relaciona con:

- lote,
- cama o invernadero,
- usuario responsable,
- fecha del proceso,
- estado operativo,
- observaciones.

## Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal. |
| Spring Boot | Framework web del backend. |
| Spring Security | Autenticación y control de acceso. |
| Spring Data JPA | Persistencia y repositorios. |
| Thymeleaf | Vistas HTML del lado servidor. |
| MySQL | Base de datos relacional. |
| Maven | Gestión de dependencias y empaquetado. |
| Git | Control de versiones. |
| GitHub | Repositorio remoto, issues, ramas y PRs. |
| GitHub Actions | Integración continua. |
| Docker | Entorno reproducible para despliegue piloto. |

## Estructura del proyecto

```text
blueberrytrace/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE/
│   └── workflows/
├── docs/
├── src/main/java/com/keraune/vlvblueberrysystem/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── security/
│   └── service/
├── src/main/resources/
│   ├── static/
│   ├── templates/
│   └── application.properties
├── script_bd_blueberrytrace.sql
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Configuración local

Crear la base de datos:

```sql
CREATE DATABASE vlv_blueberry_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Configurar credenciales mediante variables de entorno o editar `src/main/resources/application.properties`.

Variables recomendadas:

```env
DB_URL=jdbc:mysql://localhost:3306/vlv_blueberry_system?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=12345678
```

## Ejecución

Con Maven Wrapper:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

Abrir en el navegador:

```text
http://localhost:8080
```

## Credenciales iniciales

El inicializador crea un usuario administrador si no existe:

```text
Usuario: admin
Contraseña: admin123
```

## Flujo Git propuesto

El trabajo se organiza con ramas cortas desde `main`:

```text
feature/base-project
feature/security-users
feature/lotes-camas
feature/procesos-productivos
feature/reportes-dashboard
feature/auditoria-ci-docs
```

Cada rama debe cerrarse con un Pull Request hacia `main`, revisión de cambios y ejecución del workflow de CI.

Ver detalle en:

```text
docs/01-flujo-git-blueberrytrace.md
docs/02-plan-subida-github.md
docs/03-evidencias-apf2.md
```

## Evidencias recomendadas para el informe

- Captura del repositorio en GitHub.
- Captura de ramas creadas.
- Captura de commits descriptivos.
- Captura de Issues.
- Captura de Pull Request.
- Captura de GitHub Actions ejecutado.
- Captura del sistema funcionando localmente.
- Captura del README y documentación del repositorio.

## Estado académico del proyecto

Este proyecto corresponde a una propuesta académica. La estructura funcional permite demostrar trazabilidad productiva y aplicación de herramientas de desarrollo, pero una versión productiva real requeriría validación completa con usuarios finales, pruebas de seguridad, despliegue formal y revisión de infraestructura.
