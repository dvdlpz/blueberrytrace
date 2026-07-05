# Seguridad y sesiones

BlueberryTrace conserva autenticación por sesión con cookie y CSRF, no JWT.

- Contraseñas con BCrypt.
- Cookies `HttpOnly`; `Secure` y `SameSite=Strict` en producción HTTPS.
- CORS limitado al dominio de la aplicación.
- CSRF obligatorio para operaciones que cambian estado.
- Límite temporal de intentos fallidos de login en memoria.
- Invalidación de sesión cuando la cuenta se desactiva, cambia de rol o cambia/restablece su contraseña.
- Restricción de rutas por rol desde Spring Security.
- El backend no expone hashes, tokens ni stack traces al usuario.

El límite de intentos es local al proceso. Para una futura arquitectura con varias réplicas del backend, reemplázalo por Redis u otro almacén compartido.
