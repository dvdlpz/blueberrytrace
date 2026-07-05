# Validaciones de la fase de flujo real

## Validaciones estáticas realizadas

- Se verificó que las migraciones `V1` a `V10` estén presentes y ordenadas.
- Se verificó que los módulos físicos y operativos estén registrados en la metadata de módulos:
  - Jabas
  - Riegos programados
  - Recuperación por riego
  - Pedidos
  - Empaques
  - Cargas de tráiler (dentro de Despacho)
- Se verificó que las respuestas consumidas por la interfaz incluyan jaba, recuperación, pedido, empaque y despacho.
- Se revisó que los despachos trabajen con pedido, detalle de variedad, clasificación validada y empaque.
- Se revisó que la formalización conserve el orden explícito de jabas y actualice la posición física de las seleccionadas.
- Se revisó que una carga de tráiler solo consolide líneas registradas del mismo pedido y fecha, y que la salida se confirme desde dicha carga.
- Se validó la sintaxis de scripts Bash de mantenimiento.
- Se revisó que el paquete de entrega no incluya archivos `.env`, dependencias instaladas, compilados, respaldos ni archivos del IDE.

## Validaciones de ejecución

- `npm ci --ignore-scripts` finalizó correctamente: 53 paquetes auditados y sin vulnerabilidades reportadas por npm.
- `npm run frontend:build` finalizó correctamente con TypeScript y Vite.
- El build de frontend mantiene una advertencia de Vite sobre un bundle principal superior a 500 kB; no bloquea la compilación, pero queda como mejora futura mediante carga diferida de reportes.
- No se pudo ejecutar la suite Maven del backend en este entorno porque no hay Maven instalado y la descarga del Maven Wrapper desde Maven Central no tiene acceso de red. Se restauró `.mvn/wrapper/maven-wrapper.properties` para que el proyecto pueda descargar Maven en el equipo de desarrollo o VPS con red.

En el equipo de desarrollo, ejecutar:

```bash
npm ci
npm run frontend:build
npm run backend:test
npm run backend:package
```

Luego, con MySQL configurado y respaldo previo de la base de datos:

```bash
npm run backend:run
npm run frontend:dev
```

## Prueba funcional recomendada

1. Crear una cama y dos jabas activas.
2. Registrar una siembra en la primera jaba.
3. Registrar una uniformización hacia la segunda jaba y enviar una parte a recuperación por riego.
4. Programar y marcar como realizado un riego de recuperación.
5. Cerrar la recuperación con plantas recuperadas y, si corresponde, descartadas.
6. Formalizar jabas completas, definir la secuencia y verificar su nuevo orden en la cama.
7. Registrar una clasificación válida de las plantas aptas.
8. Crear un pedido confirmado por al menos dos variedades.
9. Preparar una jaba cosechera de exactamente 15 macetas o un bin de madera con capacidad superior a 100 para cada variedad requerida.
10. Registrar una línea de despacho por variedad.
11. Crear una carga de tráiler para el pedido, agregar las líneas de la misma fecha, marcarla como cargada y confirmar la salida.

El sistema debe rechazar movimientos que excedan la capacidad de una jaba, el saldo de clasificación, las unidades preparadas o la cantidad pendiente del pedido por variedad. También debe rechazar una carga vacía, líneas de otro pedido o fecha, y una confirmación de salida fuera de una carga cargada.
