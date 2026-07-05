# Flujo operativo

## Antes de registrar operaciones

1. Crear invernadero/lote físico.
2. Crear cama activa con capacidad referencial.
3. Crear lote trazable con código único, variedad, procedencia, fecha de ingreso, invernadero, cama inicial y responsable.
4. Usar ese lote trazable en los formularios de operación.

## Operación diaria

1. **Siembra:** registra la cantidad en una cama vinculada al lote trazable.
2. **Uniformización:** registra criterio, cantidad inicial y cantidad uniformizada.
3. **Formalización:** registra bandejas, plantas formalizadas y detalle.
4. **Clasificación:** registra condición, tamaño, estado de planta y cantidad. Solo la clasificación `VALIDADA` puede ser despachada.
5. **Despacho:** selecciona una clasificación validada del mismo lote trazable, destino, guía y validación de calidad.
6. **Merma:** registra pérdidas o ajustes por etapa, con motivo y fecha. Si fue un error, anúlala con motivo; no la elimines.

## Estados operativos

- Lote físico: `ACTIVO`, `INACTIVO`, `MANTENIMIENTO`, `ARCHIVADO`.
- Cama: `ACTIVA`, `INACTIVA`, `MANTENIMIENTO`, `ARCHIVADA`.
- Lote trazable: `ACTIVO`, `CERRADO`, `ARCHIVADO`, `ANULADO`.
- Siembra / uniformización / formalización: `REGISTRADA`, `ANULADA`.
- Clasificación: `PENDIENTE`, `VALIDADA`, `OBSERVADA`, `ANULADA`.
- Despacho: `REGISTRADO`, `DESPACHADO`, `OBSERVADO`, `CANCELADO`.
- Merma: `REGISTRADA`, `ANULADA`.

Las transiciones deben ser justificadas y conservan auditoría.
