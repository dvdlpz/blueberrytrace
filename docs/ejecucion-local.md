# Ejecución local

## Requisitos

- Java 21.
- Maven 3.9+ o Maven Wrapper.
- Node.js LTS y npm.
- MySQL local en ejecución.

## Variables locales

```bash
cp backend/.env.example backend/.env
```

Edita únicamente `backend/.env` con tu contraseña local y datos de bootstrap. No lo subas al repositorio.

> No uses `source backend/.env`. El comando `npm run backend:run` carga ese archivo sin evaluarlo como código Bash; así conserva URLs JDBC con `&` y nombres con espacios.

## Iniciar

En una terminal:

```bash
npm run backend:run
```

En una segunda terminal:

```bash
npm run frontend:dev
```

La interfaz queda disponible en `http://localhost:5173` y el backend en `http://localhost:8080`.

## Verificaciones

```bash
npm run frontend:build
npm run backend:test
```

## Bootstrap de administrador

Activa `BLUEBERRYTRACE_BOOTSTRAP_ADMIN_ENABLED=true` solo en una base vacía. Después del primer inicio de sesión, cámbialo a `false` y reinicia el backend.
