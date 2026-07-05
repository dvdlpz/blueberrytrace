# Implementación del flujo real del vivero

Esta fase adapta BlueberryTrace a la operación descrita por Vivero Los Viñedos. Se agregan módulos y reglas que reflejan el uso real de camas, jabas, recuperación por riego, pedidos, empaque y despacho.

## Módulos incorporados

| Módulo | Propósito operativo |
| --- | --- |
| Jabas | Registrar jabas de siembra por cama, capacidad de macetas y orden físico. |
| Riegos programados | Programar y confirmar riegos de cama o jaba. |
| Recuperación por riego | Controlar plantas secas u observadas que requieren riego antes de regresar al flujo. |
| Pedidos | Registrar requerimientos de clientes por variedad y cantidad. |
| Empaques | Preparar jabas cosecheras de 15 macetas o bins de madera con capacidad definida. |
| Cargas de tráiler | Consolidar líneas por variedad del mismo pedido antes de confirmar la salida física. |

## Cambios de operación

- La siembra ahora se realiza en una jaba específica.
- La uniformización registra jaba de origen, jaba de destino, macetas recuperables y retiro de maleza.
- La formalización registra las jabas completas, conserva su secuencia final y actualiza su posición física dentro de la cama.
- La clasificación identifica la jaba revisada y puede derivar automáticamente plantas a recuperación por riego.
- El despacho prepara líneas por pedido, variedad, clasificación validada y empaque correspondiente.
- La salida solo se confirma desde una carga de tráiler, que consolida líneas del mismo pedido y fecha.

## Migraciones incluidas

- `V4__real_nursery_operations.sql`: estructura física de jabas, recuperación, pedidos, empaques y relaciones operativas.
- `V5__scheduled_irrigation.sql`: riegos programados y ejecución real.
- `V6__formalization_crates.sql`: vínculo de formalización con jabas completas.
- `V7__uniformization_recovery_link.sql`: vínculo entre uniformización y recuperación por riego.
- `V8__classification_recovery_link.sql`: vínculo entre clasificación y recuperación por riego.
- `V9__formalization_physical_order.sql`: secuencia exacta de jabas formalizadas.
- `V10__trailer_loads.sql`: manifiestos de carga de tráiler y relación con las líneas de despacho.

Antes de aplicar migraciones sobre una base con información real, realizar un respaldo y ejecutar el procedimiento de restauración documentado en `docs/backup-restore.md`.

## Datos históricos

No se asignan automáticamente jabas, pedidos, empaques ni lotes trazables a registros antiguos. Los movimientos heredados se mantienen como referencia hasta que un administrador cuente con evidencia suficiente para normalizarlos.

## Prueba operativa sugerida

1. Crear invernadero, cama y una jaba con capacidad definida.
2. Registrar un lote trazable y una siembra en la jaba.
3. Programar y marcar como realizado un riego.
4. Registrar una uniformización entre dos jabas.
5. Enviar una parte de las plantas secas a recuperación y cerrar la recuperación después de registrar riego.
6. Formalizar una o más jabas completas y verificar que su orden físico en la cama coincida con la secuencia registrada.
7. Registrar y validar una clasificación.
8. Crear un pedido con una o más variedades y cantidad por variedad.
9. Preparar una jaba cosechera con exactamente 15 macetas o un bin de madera con capacidad superior a 100.
10. Preparar una línea de despacho por cada variedad requerida.
11. Crear una carga de tráiler, agregar sus líneas, marcarla como cargada y confirmar la salida.
