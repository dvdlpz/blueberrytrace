# Roles y permisos

BlueberryTrace usa cinco perfiles fijos. Sus códigos técnicos son parte de la política de seguridad y no se crean ni se eliminan desde la interfaz.

| Rol | Alcance principal |
|---|---|
| `ADMINISTRADOR` | Usuarios, roles, configuración operativa, todos los módulos, auditoría y reportes. |
| `SUPERVISOR` | Control y gestión de operación, trazabilidad, mermas, auditoría y reportes. No gestiona cuentas ni roles. |
| `OPERARIO` | Registra siembra, uniformización, formalización y mermas permitidas. Consulta trazabilidad y reportes. |
| `CONTROL_CALIDAD` | Registra y gestiona clasificación, despacho y mermas autorizadas. Consulta trazabilidad y reportes. |
| `CONSULTA` | Solo consulta panel, lotes trazables, trazabilidad y reportes. |

## Módulo Roles

Solo `ADMINISTRADOR` puede abrir **Roles y permisos**. El módulo permite consultar:

- nombre visible, descripción y estado;
- usuarios activos y total asignado;
- módulos accesibles;
- acciones permitidas;
- matriz de permisos;
- usuarios asignados.

Se puede editar la descripción y activar/desactivar un rol bajo estas reglas:

1. Los códigos técnicos son inmutables.
2. No se crean roles arbitrarios.
3. No se eliminan roles del sistema.
4. Un rol con usuarios activos no puede desactivarse hasta reasignarlos.
5. Siempre debe existir al menos una cuenta administrativa activa.

El backend aplica autorización real con Spring Security. Ocultar un botón en React nunca concede un permiso.

## Usuarios y sesiones

- Solo administradores crean, editan, activan, desactivan y restablecen contraseñas de usuarios.
- Las cuentas nuevas y las contraseñas restablecidas requieren un cambio de contraseña en el siguiente acceso.
- Cambiar rol, estado o contraseña incrementa la versión de sesión. La siguiente solicitud invalida las sesiones antiguas.

## Configuración visual y permisos individuales

En la pantalla **Roles y permisos**, cada perfil muestra un distintivo de color. Administración puede elegir un color corporativo y marcar o retirar acciones una por una, por área de trabajo.

- La acción **Consultar** habilita el acceso a un área; al retirarla se deshabilitan sus acciones relacionadas.
- Activar una acción adicional habilita automáticamente la consulta de esa misma área.
- Los accesos esenciales del perfil administrativo se muestran bloqueados para evitar dejar la plataforma sin una cuenta capaz de administrarla.
- Los cambios se guardan en la base de datos y se aplican en cada solicitud. Las sesiones de las personas con ese perfil se actualizan para que los nuevos permisos tengan efecto al volver a operar.
