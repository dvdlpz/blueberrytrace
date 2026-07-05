# Validaciones realizadas

Fecha de preparación: 1 de julio de 2026.

## Ejecutadas en el entorno de preparación

| Validación | Resultado |
|---|---|
| `npm ci --foreground-scripts --registry=https://registry.npmjs.org/` | Correcto. Instalación limpia desde `package-lock.json`; se instalaron 53 paquetes. |
| `npm run build` | Correcto. TypeScript y Vite completaron el build de producción con 1,993 módulos transformados. |
| `bash -n scripts/*.sh` | Correcto para los scripts de ejecución, carga segura de variables, backup, restore y migración. |
| Parseo YAML | Correcto para `docker-compose.yml` y `docker-compose.prod.yml`. |
| Revisión de textos operativos | Correcto. Los estados vacíos, formularios, roles, auditoría y reportes usan lenguaje dirigido al personal operativo y administrativo. |
| Navegación y vista previa | Correcto mediante revisión estática. El panel lateral queda fijo en escritorio y la vista previa limita el desplazamiento horizontal al contenedor de tabla. |
| Configuración de permisos | Correcto mediante revisión estática. Los permisos se guardan por rol, se validan en el servidor y la interfaz los presenta como selección individual. |
| Revisión de entrega | Correcto. No se detectaron `.env`, archivos IDE, `target`, `dist`, `node_modules`, respaldos ni archivos `.tsbuildinfo` antes del empaquetado. |

## No ejecutadas en este entorno

| Validación | Motivo |
|---|---|
| `npm audit --omit=dev` | No se pudo completar por fallo DNS temporal hacia `registry.npmjs.org` (`EAI_AGAIN`). No se afirma un resultado de vulnerabilidades. |
| `./mvnw -pl backend test` | Maven Wrapper quedó configurado, pero el entorno no pudo descargar Apache Maven 3.9.16 desde Maven Central. |
| `./mvnw -pl backend clean package` | Misma limitación de Maven/red. |
| `docker compose config`, `docker build`, `docker compose up` | Docker no está instalado en el entorno de preparación. |
| Pruebas integradas contra MySQL | No se dispone de MySQL/Docker aislado en el entorno de preparación. |

## Validación obligatoria antes de producción

Ejecutar en el VPS o en un equipo con Internet, Java 21, Docker, Docker Compose y MySQL compatible:

```bash
npm ci --foreground-scripts --registry=https://registry.npmjs.org/
npm run build
npm audit --omit=dev
./mvnw -pl backend test
./mvnw -pl backend clean package
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
curl -fsS https://TU_DOMINIO/api/v1/health
```

## Fase de integridad operativa — 2 de julio de 2026

| Validación | Resultado |
|---|---|
| `bash -n scripts/*.sh` | Correcto. Los scripts de operación no presentan errores de sintaxis Bash. |
| Revisión estática de saldos actuales | Correcta. El panel, reportes y trazabilidad operativa usan movimientos vinculados a lote trazable. |
| Revisión estática de despacho | Correcta. El código exige clasificación del mismo lote trazable y descuenta saldo únicamente al confirmar el despacho. |
| Revisión de entrega | Correcta. Se excluyeron `.env`, compilados, IDE, `node_modules`, `target` y archivos generados. |
| `npm run build` | No ejecutado con éxito en esta fase: las dependencias del entorno quedaron incompletas y TypeScript no pudo resolver paquetes de tipos. Debe repetirse con `npm ci` en un equipo con acceso al registro npm. |
| `./mvnw -pl backend -DskipTests compile` | No ejecutado con éxito: Maven Wrapper no pudo descargar Apache Maven desde Maven Central en este entorno. |

Antes de integrar esta fase, ejecutar en el equipo local:

```bash
npm ci --foreground-scripts --registry=https://registry.npmjs.org/
npm run build
./mvnw -pl backend test
./mvnw -pl backend clean package
```

## Fase de formularios y controles operativos — 3 de julio de 2026

| Validación | Resultado |
|---|---|
| `npm ci --foreground-scripts --registry=https://registry.npmjs.org/` | Correcto. Instalación limpia del workspace a partir de los archivos de bloqueo. |
| `npm run frontend:build` | Correcto. TypeScript y Vite generaron el build de producción sin errores. Vite reporta únicamente la advertencia conocida de un bundle mayor a 500 kB. |
| `bash -n scripts/*.sh` | Correcto. Los scripts operativos no presentan errores de sintaxis Bash. |
| `bash scripts/doctor.sh` | Correcto respecto de estructura, Java 21, Node, permisos y Maven Wrapper raíz. El puerto 8080 ya estaba ocupado por un proceso externo al proyecto, por lo que no se usó para esta validación. |
| Revisión estática del sistema de formularios | Correcta. Los controles reutilizables de detalle de pedido, checkbox de uniformización/formalización y botones compactos cuentan con estilos definidos en `frontend/src/styles/forms.css`. |
| Revisión estática de duplicados de variedad | Correcta. El frontend compara variedad normalizada sin espacios laterales y sin distinción de mayúsculas/minúsculas; el backend conserva la misma regla en `PedidoService`. |

### Pendiente por ejecutar fuera de este entorno

| Validación | Motivo |
|---|---|
| `npm run backend:test` | No completada. Se restauró `.mvn/wrapper/maven-wrapper.properties`, pero Maven Wrapper no pudo descargar Apache Maven 3.9.16 desde Maven Central porque el entorno bloqueó la descarga. |
| Pruebas visuales automatizadas del navegador | El frontend no tiene una suite configurada. La revisión se realizó mediante build, inspección de componentes y reglas responsive para escritorio, tablet y móvil. Debe comprobarse con datos reales en el navegador del equipo o VPS antes del despliegue. |

## Fase de estabilización visual y preflight VPS — 2 de julio de 2026

| Validación | Resultado |
|---|---|
| `npm ci` | Correcto. Instalación limpia del workspace desde `package-lock.json`; no se reportaron vulnerabilidades por npm durante la instalación. |
| `npm run frontend:build` | Correcto. TypeScript y Vite generaron la compilación de producción sin errores. Se mantiene la advertencia de Vite sobre un bundle mayor a 500 kB; no bloquea el despliegue. |
| `bash -n scripts/*.sh` | Correcto. Incluye el nuevo `scripts/preflight-vps.sh`. |
| Preflight VPS con configuración simulada | Correcto. El script valida variables obligatorias, consistencia de las credenciales de aplicación, dominio/CORS y el paso de Docker Compose sin exponer secretos. |
| Revisión estática de interfaz | Correcta. El asistente de siembra, checkbox operativos, selector de trazabilidad, resumen de lote y búsqueda de auditoría cuentan con estilos encapsulados en `frontend/src/styles/operations.css`. |
| Revisión de archivos de despliegue | Correcta. Se incorporaron `.env.example`, ejemplos locales, `.gitignore` y la configuración del Maven Wrapper que requieren README, scripts y Docker Compose. |
| Limpieza antes de empaquetar | Correcta. Se retiraron `node_modules`, `dist`, archivos `.tsbuildinfo`, archivos compilados de Vite e IDE. |

### Pendiente antes de producción

| Validación | Motivo |
|---|---|
| `./mvnw -pl backend test` | No completada en este entorno. Maven Wrapper intentó descargar Apache Maven 3.9.16 desde Maven Central, pero la red del entorno bloqueó la descarga. |
| `npm run deploy:preflight` con Docker real | Docker no está instalado en el entorno de preparación. Debe ejecutarse dentro del VPS después de crear `.env` a partir de `.env.example`. |
| Prueba visual con datos reales | No existe una suite E2E de navegador. Se debe comprobar manualmente en navegador antes del despliegue, con especial atención a 1366px, 1024px, 768px y 390px. |

## Adaptación Pterodactyl Java 21

- Se agregó el perfil `pterodactyl` para ejecutar Spring Boot con React integrado en el mismo JAR.
- El empaquetado usa el perfil Maven `pterodactyl-bundle`, que copia `frontend/dist` a `BOOT-INF/classes/static` únicamente para el artefacto de Pterodactyl.
- Se añadió fallback de rutas SPA para las rutas operativas de React y se mantuvo la protección de `/api/v1/**`.
- Se incorporaron scripts `package:pterodactyl` y `preflight:pterodactyl`.
- `npm ci` y `npm run frontend:build` se ejecutaron correctamente.
- Se validaron los scripts con `bash -n`, el `pom.xml` con un parser XML y la cobertura de las 22 rutas SPA entre React, seguridad y el controlador de fallback.
- `preflight-pterodactyl.sh` se validó contra un JAR de prueba que contenía React integrado y el perfil Pterodactyl.
- El empaquetado Maven real quedó pendiente: el Maven Wrapper no pudo descargar Maven 3.9.16 porque este entorno bloqueó el acceso a Maven Central. Debe ejecutarse `npm run package:pterodactyl` en un equipo con acceso a Maven Central antes de subir el JAR al panel.
