# Auditoría operativa y administrativa

La tabla `auditoria_operaciones` conserva evidencia de acciones relevantes. El módulo **Auditoría** es de solo lectura y está disponible para `ADMINISTRADOR` y `SUPERVISOR`.

## Eventos registrados

- inicio y cierre de sesión;
- creación, edición, activación y desactivación de usuarios;
- cambio de rol y restablecimiento de contraseña;
- actualización de perfil y cambio de contraseña;
- creación, edición, anulación y cambio de estado de operaciones;
- cambios de roles;
- creación, actualización y archivado de lotes trazables;
- mermas y anulaciones.

## Campos

Cada evento puede incluir usuario, rol, fecha/hora, módulo, acción, entidad, identificador, referencia legible, descripción, motivo, valores antes/después cuando no contienen secretos, IP de origen y agente de usuario.

No se guardan contraseñas, hashes, tokens, cookies ni el contenido binario de imágenes en los valores auditados.

## Uso administrativo

Filtra por periodo, módulo, acción o usuario. Antes de corregir una operación consolidada, revisa primero su línea de auditoría y si existen movimientos posteriores. Las correcciones se realizan por anulación o movimiento compensatorio, no por borrado físico.
