# Modelo de trazabilidad

## Separación de conceptos

BlueberryTrace diferencia la **ubicación física** de la **unidad trazable**:

- **Lote físico / invernadero:** área de producción donde se organizan camas.
- **Cama:** ubicación física con capacidad referencial.
- **Lote trazable:** grupo concreto de plantas con código único, variedad, procedencia, fecha de ingreso, ubicación inicial y responsable.

La cama no es la identidad de las plantas. Dos siembras en la misma cama pueden corresponder a lotes trazables distintos; por ello las nuevas operaciones exigen el identificador del lote trazable.

## Cadena operativa

```text
Lote trazable
  └─ Siembra registrada
      └─ Uniformización registrada
          └─ Formalización registrada
              └─ Clasificación pendiente / validada / observada
                  └─ Despacho de una clasificación validada
```

Las mermas se registran por lote trazable y por etapa de origen. No se eliminan físicamente: pueden anularse con motivo y quedan auditadas.

## Balance de plantas

La consulta de un lote trazable calcula, con registros persistidos:

- sembradas;
- uniformizadas;
- formalizadas;
- clasificación pendiente, validada y observada;
- despachadas;
- anuladas;
- mermas registradas;
- saldo disponible: clasificación validada menos despachos y mermas de clasificación.

Los registros anulados, cancelados, archivados u observados no se usan como stock disponible.

## Datos históricos

Las operaciones existentes antes de la migración no se vinculan automáticamente. La migración conserva esos registros y deja la relación `lote_trazable_id` nula. Deben normalizarse solo cuando el administrador cuente con evidencia verificable de procedencia, cama y fechas. No se deben inventar relaciones históricas.

## Reglas de consistencia

- No se siembra por encima de la capacidad activa de la cama.
- Uniformización no puede superar siembra disponible.
- Formalización no puede superar uniformización disponible.
- Clasificación no puede superar formalización disponible.
- Un despacho requiere una clasificación validada del mismo lote trazable.
- Una merma requiere cantidad positiva, motivo, fecha válida y saldo de la etapa.
- Operaciones con movimientos posteriores no se eliminan físicamente; se anulan o corrigen con auditoría.
