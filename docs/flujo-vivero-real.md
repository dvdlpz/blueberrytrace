# Flujo operativo real del vivero

BlueberryTrace representa el recorrido físico de las plantas de arándano desde su ubicación inicial hasta la carga en un vehículo de despacho. El sistema diferencia la estructura física, el lote trazable y los movimientos que ocurren durante la operación.

## Estructura física

La estructura se organiza de la siguiente manera:

```text
Invernadero o lote físico
└── Cama
    └── Jaba de siembra
        └── Macetas plásticas con plantas
```

- **Lote físico o invernadero:** área productiva donde se agrupan las camas.
- **Cama:** fila longitudinal de trabajo dentro del invernadero.
- **Jaba de siembra:** contenedor ubicado en una cama que mantiene macetas plásticas. Cada jaba tiene capacidad de macetas, orden dentro de la cama y saldo físico disponible.
- **Maceta:** unidad física que contiene una planta. BlueberryTrace controla las macetas por cantidad; no inventa un identificador individual por cada planta.
- **Lote trazable:** grupo de plantas de la misma procedencia y variedad que avanza por el proceso sin mezclarse con otros grupos.

## 1. Siembra y crecimiento

La siembra registra la cantidad de macetas instaladas en una jaba específica. La jaba debe pertenecer a la cama seleccionada y no puede superar su capacidad física.

Después de la siembra, se pueden programar riegos para la cama o para una jaba concreta. Un riego queda como pendiente, realizado o cancelado; no se considera realizado hasta que se registra su ejecución.

## 2. Uniformización

La uniformización trabaja a nivel de **macetas individuales**. Permite trasladar macetas desde una jaba de origen hacia una jaba de destino para reunir plantas de tamaño semejante.

Durante la uniformización se puede registrar:

- criterio de tamaño o condición;
- cantidad revisada;
- macetas trasladadas;
- plantas enviadas a recuperación por riego;
- retiro de malezas;
- observaciones de trabajo.

Las cantidades trasladadas y enviadas a recuperación no pueden superar las macetas disponibles del mismo lote trazable en la jaba de origen. La jaba de destino tampoco puede exceder su capacidad.

## 3. Recuperación por riego

Las plantas secas u observadas durante uniformización o clasificación ingresan a una recuperación por riego. Mientras estén en recuperación, dejan de estar disponibles para clasificación o empaque.

Para cerrar una recuperación se debe registrar primero un riego realizado. Al cierre, las plantas se dividen en:

- **recuperadas:** vuelven a estar disponibles para continuar el flujo operativo;
- **descartadas:** generan una merma con motivo registrado.

## 4. Formalización

La formalización trabaja con **jabas completas**, no con macetas individuales. Permite seleccionar una o más jabas del mismo lote trazable, definir su secuencia final y registrar cómo fueron organizadas, por ejemplo:

- de mayor a menor tamaño;
- de menor a mayor tamaño.

La secuencia elegida se conserva en el historial de formalización y actualiza el orden físico de las jabas dentro de la cama, sin modificar las macetas que contiene cada jaba. La formalización no impide volver a uniformización. Esto refleja la operación real: una jaba formalizada puede requerir una nueva redistribución de macetas antes de llegar a clasificación.

## 5. Clasificación de calidad

La clasificación revisa plantas de una jaba concreta. Una clasificación válida representa plantas aptas para empaque.

- Las plantas húmedas y aptas pueden ser validadas.
- Las plantas secas o enviadas a recuperación quedan observadas y generan seguimiento de riego.
- Las plantas no recuperables se registran como merma.

Las plantas observadas no se consideran disponibles para empaques hasta una nueva clasificación válida.

## 6. Pedido por variedad

Un pedido representa el requerimiento comercial de un cliente. Cada pedido contiene una o más líneas, y cada línea define:

- variedad solicitada;
- cantidad solicitada;
- cantidad ya despachada;
- saldo pendiente.

No se puede usar un empaque ni confirmar un despacho para una variedad distinta a la solicitada en el detalle de pedido.

## 7. Empaque

El empaque agrupa plantas clasificadas y validadas antes de la salida.

Tipos soportados:

- **Jaba cosechera:** capacidad fija de **15 macetas**.
- **Bin de madera:** capacidad fija definida por la operación; debe ser mayor a 100 macetas y queda registrada en el empaque.

El sistema valida la clasificación fuente, el lote trazable, la variedad del pedido y el saldo disponible antes de preparar un empaque. Un empaque anulado devuelve su cantidad al saldo disponible.

## 8. Carga de tráiler y despacho

Cada línea de despacho se prepara desde un empaque y un detalle de pedido. La línea queda como **Registrada** y mantiene el lote trazable, clasificación, empaque, variedad, unidades y cantidad de plantas.

La salida física se confirma mediante una **carga de tráiler**. Una carga corresponde a un único pedido y puede reunir varias líneas de distintas variedades, siempre que correspondan a la misma fecha de carga. Su flujo es:

1. **Preparada:** se crean o agregan líneas de despacho del mismo pedido.
2. **Cargada:** el tráiler quedó armado y ya no permite agregar ni retirar líneas.
3. **Despachada:** se confirma la salida; todas las líneas se descuentan de su empaque y del saldo del pedido por variedad.
4. **Cancelada:** se libera la asociación de las líneas que aún no salieron.

La carga valida que coincidan:

- lote trazable y clasificación validada de cada línea;
- empaque y variedad solicitada;
- cantidades de plantas y unidades disponibles;
- pedido común, destino y fecha de carga;
- vehículo o tráiler asignado.

Un pedido pasa automáticamente a parcial o completado según las líneas confirmadas por variedad.

## Reglas principales de integridad

1. Una cama no puede contener más macetas que su capacidad referencial.
2. Una jaba no puede superar su capacidad de macetas.
3. No se pueden mover macetas de una jaba que no las tiene disponibles para el lote trazable seleccionado.
4. Una formalización solo usa jabas completas del mismo lote trazable.
5. Las plantas en recuperación no se empacan ni se despachan.
6. Una jaba cosechera siempre contiene 15 macetas.
7. Un bin debe tener capacidad registrada mayor a 100 macetas.
8. No se puede despachar más de lo solicitado por variedad ni más de lo empacado y validado.
9. Una línea preparada solo se confirma al despachar la carga de tráiler que la contiene.
10. Una carga solo mezcla líneas del mismo pedido y fecha; mantiene el detalle de cada variedad y empaque.
11. Los registros heredados sin lote trazable se conservan como históricos y no se mezclan con los saldos operativos actuales.
