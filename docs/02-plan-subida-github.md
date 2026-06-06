# Plan paso a paso para subir BlueberryTrace a GitHub con evidencia real

La clave es no subir todo en un único commit. El objetivo es construir un historial ordenado que demuestre uso de Git, GitHub, ramas, commits, Pull Requests y CI.

## 1. Crear el repositorio en GitHub

Nombre sugerido:

```text
blueberrytrace-apf2
```

Configuración recomendada:

- Público o privado, según indique el docente.
- No marcar README inicial si se subirá desde el proyecto local.
- No agregar `.gitignore` desde GitHub si ya existe en el proyecto.

## 2. Inicializar Git localmente

Dentro de la carpeta del proyecto:

```bash
git init
git branch -M main
```

Configurar usuario si es necesario:

```bash
git config user.name "Angel David Lopez Flores"
git config user.email "angellopezcoes@gmail.com"
```

## 3. Primer commit: base mínima

Agregar solo archivos base:

```bash
git add .gitattributes .gitignore README.md pom.xml mvnw mvnw.cmd .mvn script_bd_blueberrytrace.sql MYSQL_CONEXION_NOTA.txt
git add src/main/java/com/keraune/vlvblueberrysystem/VlvBlueberrySystemApplication.java
git add src/main/resources/application.properties
git commit -m "feat: agregar estructura base del proyecto Spring Boot"
```

## 4. Conectar con GitHub

Reemplazar la URL por la del repositorio creado:

```bash
git remote add origin https://github.com/TU_USUARIO/blueberrytrace-apf2.git
git push -u origin main
```

## 5. Rama de seguridad y usuarios

```bash
git checkout -b feature/security-users
git add src/main/java/com/keraune/vlvblueberrysystem/config/SecurityConfig.java
git add src/main/java/com/keraune/vlvblueberrysystem/config/DataInitializer.java
git add src/main/java/com/keraune/vlvblueberrysystem/security
git add src/main/java/com/keraune/vlvblueberrysystem/entity/User.java src/main/java/com/keraune/vlvblueberrysystem/entity/Role.java
git add src/main/java/com/keraune/vlvblueberrysystem/repository/UserRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/RoleRepository.java
git add src/main/java/com/keraune/vlvblueberrysystem/controller/AuthController.java src/main/java/com/keraune/vlvblueberrysystem/controller/AccountController.java src/main/java/com/keraune/vlvblueberrysystem/controller/UsuarioController.java
git add src/main/resources/templates/auth src/main/resources/templates/cuenta src/main/resources/templates/usuarios
git commit -m "feat: implementar autenticacion usuarios y roles"
git push -u origin feature/security-users
```

En GitHub, abrir Pull Request hacia `main`, revisar y hacer merge.

## 6. Rama de lotes y camas

```bash
git checkout main
git pull origin main
git checkout -b feature/lotes-camas
git add src/main/java/com/keraune/vlvblueberrysystem/entity/Cama.java src/main/java/com/keraune/vlvblueberrysystem/entity/Lote.java
git add src/main/java/com/keraune/vlvblueberrysystem/dto/CamaForm.java src/main/java/com/keraune/vlvblueberrysystem/dto/LoteForm.java
git add src/main/java/com/keraune/vlvblueberrysystem/repository/CamaRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/LoteRepository.java
git add src/main/java/com/keraune/vlvblueberrysystem/service/CamaService.java src/main/java/com/keraune/vlvblueberrysystem/service/LoteService.java
git add src/main/java/com/keraune/vlvblueberrysystem/controller/CamaController.java src/main/java/com/keraune/vlvblueberrysystem/controller/LoteController.java
git add src/main/resources/templates/camas src/main/resources/templates/lotes
git commit -m "feat: agregar gestion de camas y lotes"
git push -u origin feature/lotes-camas
```

Abrir Pull Request y fusionar.

## 7. Rama de procesos productivos

```bash
git checkout main
git pull origin main
git checkout -b feature/procesos-productivos
git add src/main/java/com/keraune/vlvblueberrysystem/entity/Siembra.java src/main/java/com/keraune/vlvblueberrysystem/entity/Uniformizacion.java src/main/java/com/keraune/vlvblueberrysystem/entity/Formalizacion.java src/main/java/com/keraune/vlvblueberrysystem/entity/Clasificacion.java src/main/java/com/keraune/vlvblueberrysystem/entity/Despacho.java
git add src/main/java/com/keraune/vlvblueberrysystem/dto/SiembraForm.java src/main/java/com/keraune/vlvblueberrysystem/dto/UniformizacionForm.java src/main/java/com/keraune/vlvblueberrysystem/dto/FormalizacionForm.java src/main/java/com/keraune/vlvblueberrysystem/dto/ClasificacionForm.java src/main/java/com/keraune/vlvblueberrysystem/dto/DespachoForm.java
git add src/main/java/com/keraune/vlvblueberrysystem/repository/SiembraRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/UniformizacionRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/FormalizacionRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/ClasificacionRepository.java src/main/java/com/keraune/vlvblueberrysystem/repository/DespachoRepository.java
git add src/main/java/com/keraune/vlvblueberrysystem/service/SiembraService.java src/main/java/com/keraune/vlvblueberrysystem/service/ProcesoOperativoService.java src/main/java/com/keraune/vlvblueberrysystem/service/ClasificacionService.java src/main/java/com/keraune/vlvblueberrysystem/service/DespachoService.java
git add src/main/java/com/keraune/vlvblueberrysystem/controller/SiembraController.java src/main/java/com/keraune/vlvblueberrysystem/controller/ProcesoController.java src/main/java/com/keraune/vlvblueberrysystem/controller/ClasificacionController.java src/main/java/com/keraune/vlvblueberrysystem/controller/DespachoController.java
git add src/main/resources/templates/siembra src/main/resources/templates/procesos src/main/resources/templates/clasificacion src/main/resources/templates/despacho
git commit -m "feat: implementar procesos productivos del lote"
git push -u origin feature/procesos-productivos
```

Abrir Pull Request y fusionar.

## 8. Rama de reportes y dashboard

```bash
git checkout main
git pull origin main
git checkout -b feature/reportes-dashboard
git add src/main/java/com/keraune/vlvblueberrysystem/dto/DashboardSummary.java src/main/java/com/keraune/vlvblueberrysystem/dto/TrazabilidadRow.java
git add src/main/java/com/keraune/vlvblueberrysystem/service/DashboardMetricsService.java src/main/java/com/keraune/vlvblueberrysystem/service/TrazabilidadQueryService.java
git add src/main/java/com/keraune/vlvblueberrysystem/controller/DashboardController.java src/main/java/com/keraune/vlvblueberrysystem/controller/ReporteController.java src/main/java/com/keraune/vlvblueberrysystem/controller/HomeController.java
git add src/main/resources/templates/dashboard src/main/resources/templates/reportes src/main/resources/templates/fragments
git add src/main/resources/static/css src/main/resources/static/js src/main/resources/static/img
git commit -m "feat: agregar dashboard y reportes de trazabilidad"
git push -u origin feature/reportes-dashboard
```

Abrir Pull Request y fusionar.

## 9. Rama de auditoría, CI y documentación

```bash
git checkout main
git pull origin main
git checkout -b feature/auditoria-ci-docs
git add src/main/java/com/keraune/vlvblueberrysystem/entity/Auditoria.java
git add src/main/java/com/keraune/vlvblueberrysystem/repository/AuditoriaRepository.java
git add src/main/java/com/keraune/vlvblueberrysystem/service/AuditoriaService.java
git add src/main/java/com/keraune/vlvblueberrysystem/controller/AdminController.java
git add src/main/resources/templates/admin
git add .github docs Dockerfile docker-compose.yml .env.example
git commit -m "feat: agregar auditoria documentacion ci y docker"
git push -u origin feature/auditoria-ci-docs
```

Abrir Pull Request y fusionar.

## 10. Crear Issues para evidenciar gestión

Crear estos issues en GitHub:

1. Implementar login y control de acceso.
2. Registrar lotes e invernaderos.
3. Registrar procesos productivos por lote.
4. Generar reportes de trazabilidad.
5. Agregar auditoría de acciones.
6. Configurar GitHub Actions.
7. Documentar flujo Git del proyecto.

Cerrar cada issue desde el Pull Request correspondiente o manualmente después del merge.

## 11. Crear release final

En GitHub:

```text
Releases → Draft a new release → Tag: v1.0-apf2
```

Título:

```text
BlueberryTrace APF2
```

Descripción:

```text
Versión académica para el Avance 2 de Herramientas de Desarrollo. Incluye módulos de trazabilidad de arándanos, flujo GitHub Flow, documentación, CI y soporte Docker.
```
