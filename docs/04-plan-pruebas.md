# Plan de pruebas básico

## Objetivo

Verificar que los módulos principales de BlueberryTrace funcionen antes de integrar cambios a `main`.

## Casos de prueba

| Código | Módulo | Acción | Resultado esperado |
|---|---|---|---|
| CP-01 | Login | Ingresar con admin/admin123 | El sistema redirige al dashboard. |
| CP-02 | Login | Ingresar credenciales incorrectas | El sistema muestra error. |
| CP-03 | Lotes | Registrar lote con código nuevo | El lote aparece en la lista. |
| CP-04 | Lotes | Registrar lote con código duplicado | El sistema evita duplicidad. |
| CP-05 | Camas | Registrar cama asociada a invernadero | La cama se guarda correctamente. |
| CP-06 | Siembra | Registrar cantidad inicial | El proceso queda asociado al lote. |
| CP-07 | Clasificación | Registrar aprobadas y descartadas | El reporte refleja el cambio. |
| CP-08 | Despacho | Registrar cantidad despachada | El lote queda actualizado. |
| CP-09 | Reportes | Filtrar por lote | Se muestra historial completo. |
| CP-10 | Auditoría | Consultar acciones registradas | Se visualiza usuario, módulo, acción y fecha. |

## Validación de Pull Request

Antes de fusionar una rama:

- Compilar el proyecto.
- Ejecutar prueba manual del módulo tocado.
- Revisar que no se afecten rutas existentes.
- Confirmar que el PR tenga descripción y relación con el informe.
