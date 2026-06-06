# Flujo Git propuesto para BlueberryTrace

Este documento define el flujo de trabajo que debe evidenciarse en GitHub para el Avance 2 de Herramientas de Desarrollo.

## Estrategia elegida

Se propone usar **GitHub Flow**, porque el proyecto académico requiere un flujo simple, con ramas cortas, Pull Requests y revisión antes de integrar a `main`.

## Rama principal

```text
main
```

Debe representar la versión estable del proyecto.

## Ramas de trabajo

| Rama | Objetivo | Evidencia esperada |
|---|---|---|
| feature/base-project | Subida inicial del proyecto Spring Boot | Commit base, estructura del proyecto, README inicial |
| feature/security-users | Login, usuarios, roles y seguridad | Commits de SecurityConfig, User, Role, vistas auth |
| feature/lotes-camas | Gestión de lotes, camas e invernaderos | Commits de controller/service/repository/templates |
| feature/procesos-productivos | Siembra, uniformización, formalización, clasificación y despacho | Commits por etapa productiva |
| feature/reportes-dashboard | Dashboard y reportes de trazabilidad | Commits de reportes, métricas y vistas |
| feature/auditoria-ci-docs | Auditoría, GitHub Actions, Docker y documentación | PR final con evidencias del curso |

## Reglas de commits

Usar mensajes claros y pequeños:

```text
feat: agregar estructura base del proyecto
feat: implementar login y usuarios
feat: registrar camas por invernadero
feat: agregar gestión de lotes
feat: implementar procesos productivos
feat: agregar reportes de trazabilidad
feat: agregar auditoria de acciones
ci: agregar workflow de compilacion con Maven
docs: documentar flujo Git del proyecto
```

## Pull Requests

Cada rama debe abrir un Pull Request hacia `main`. El PR debe indicar:

- módulo trabajado,
- archivos principales modificados,
- relación con el informe APF2,
- capturas o pruebas realizadas,
- resultado de GitHub Actions.

## Resolución de conflictos

Si GitHub detecta conflicto, se debe actualizar la rama con `main`, resolver el archivo afectado, confirmar el cambio y volver a subir la rama.

```bash
git checkout feature/nombre-rama
git pull origin main
# resolver conflictos
git add .
git commit -m "fix: resolver conflicto de integracion"
git push origin feature/nombre-rama
```

## Release académico

Cuando el proyecto esté integrado en `main`, se recomienda crear una versión:

```text
v1.0-apf2
```

Notas sugeridas:

```text
Primera versión académica de BlueberryTrace para el Avance 2 de Herramientas de Desarrollo. Incluye módulos de trazabilidad, documentación, GitHub Flow, CI y soporte Docker.
```
