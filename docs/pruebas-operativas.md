# Pruebas operativas

## Automatizadas incluidas

- roles técnicos y matriz de permisos;
- identificador de login y política de correo corporativo;
- validación de imagen de perfil;
- límite de intentos de inicio de sesión;
- capacidad de cama;
- cantidad consistente de formalización;
- autenticación y protección básica de endpoints.

Ejecutar con Maven disponible:

```bash
npm run backend:test
npm run build
```

## Lista de pruebas manuales obligatorias

| Caso | Resultado esperado |
|---|---|
| Login por usuario y por correo | Solo una cuenta activa entra con contraseña válida. |
| Login inválido y bloqueo | Mensaje no revelador; bloqueo temporal tras máximo configurado. |
| CSRF | POST/PATCH/PUT/DELETE sin token CSRF falla. |
| Roles | Un rol de consulta no puede abrir APIs administrativas aunque altere la interfaz. |
| Último administrador | No se puede desactivar, cambiar de rol ni dejar inactivo al último administrador. |
| Usuario duplicado | Rechaza usuario o correo existente. |
| Contraseña temporal | La cuenta debe cambiar la contraseña antes de continuar. |
| Lote/cama | Rechaza códigos duplicados y cama con capacidad inferior a siembras activas. |
| Cadena operativa | No permite superar disponibilidad entre siembra, procesos y clasificación. |
| Despacho | Requiere clasificación validada del mismo lote trazable y saldo suficiente. |
| Merma | Requiere motivo, cantidad positiva, fecha válida, cronología y saldo de etapa. |
| Auditoría | Registra creación, cambio de rol/estado, normalización de legado, merma, anulación y eventos de sesión. |
| Reportes | Vista previa, PDF y XLSX muestran filtros, usuario, período, totales y “Sin información disponible” cuando aplica. |
| Perfil | Rechaza imagen no permitida, sobredimensionada o con firma inválida. |
| Normalización de legado | Solo administrador; exige evidencia y coincidencia de invernadero, cama y fecha. No vincula despachos automáticamente. |
| Bootstrap administrativo | Rechaza valores de ejemplo y no sobrescribe una cuenta existente. |

## Integración con MySQL

En una máquina con Docker:

```bash
npm run docker:config
docker compose --env-file .env up -d --build
curl -fsS http://localhost/api/v1/health
```

Las pruebas de integración con MySQL requieren un entorno aislado y datos controlados; no se ejecutan contra producción.

## Flujo físico de jabas, recuperación, empaque y pedido

La validación completa del flujo real se documenta en [`validaciones-flujo-real.md`](validaciones-flujo-real.md). Incluye pruebas de capacidad de jaba, traslado por uniformización, recuperación por riego, formalización de jabas completas, capacidad fija de empaque y despacho por variedad.
