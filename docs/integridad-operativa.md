# Integridad operativa

## Alcance de los saldos actuales

BlueberryTrace calcula los indicadores, reportes y consultas operativas únicamente con movimientos vinculados a un **lote trazable**. Esta separación evita que registros históricos sin vínculo confirmado alteren el saldo de una operación actual.

Los movimientos anteriores que no tengan lote trazable continúan conservados como historial. No se eliminan ni se vinculan automáticamente.

## Cadena controlada

La secuencia válida es:

1. Lote trazable activo.
2. Siembra registrada.
3. Uniformización registrada.
4. Formalización registrada.
5. Clasificación validada.
6. Despacho confirmado.

Una merma registrada reduce únicamente el saldo de su etapa de origen. Las mermas de clasificación reducen el saldo disponible para despacho.

## Reglas principales

- Una siembra no puede superar la capacidad de la cama.
- Una uniformización no puede superar la siembra disponible.
- Una formalización no puede superar la uniformización disponible.
- Una clasificación no puede superar la formalización disponible.
- Un despacho debe provenir de una clasificación validada del mismo lote trazable.
- Un despacho solo descuenta saldo cuando se confirma como `DESPACHADO`.
- Los movimientos anulados, cancelados y observados no se incluyen como saldo disponible.
- Las fechas deben respetar el orden de las etapas.
- Un movimiento con etapas posteriores no puede anularse sin revisar primero los registros dependientes.

## Registros históricos

Los registros sin lote trazable se muestran como información histórica o pendiente de normalización. Para vincularlos, un administrador debe confirmar lote físico, cama y evidencia operativa. El sistema rechaza vínculos que rompan fechas o cantidades.

## Verificación operativa antes de despacho

1. Abra **Lotes trazables** y confirme que el lote esté activo.
2. Abra **Clasificaciones** y valide el registro correspondiente.
3. Registre el despacho; inicialmente quedará como registrado.
4. Revise los datos y confirme el despacho desde el módulo de despachos.
5. Consulte **Trazabilidad** para verificar la línea de tiempo y el saldo disponible.

## Revisión de incidencias

Si el panel muestra una alerta de saldo, revise primero los despachos confirmados, las mermas de clasificación y las clasificaciones validadas del lote trazable. No corrija el saldo creando movimientos adicionales; use la anulación o el ajuste documentado según corresponda.
