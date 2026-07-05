const technicalPattern = /\b(backend|frontend|api|controller|service|repository|database|base de datos|sql|hibernate|jdbc|exception|stack|constraint|foreign key|json|mysql|mariadb|server)\b/i;
const transportPattern = /\b(failed to fetch|networkerror|network request|load failed|error http)\b/i;

function fallback(status?: number) {
  if (status === 401) return 'Tu sesión no está activa. Inicia sesión para continuar.';
  if (status === 403) return 'No cuentas con permisos para realizar esta acción.';
  if (status === 404) return 'No se encontró la información solicitada. Actualiza la pantalla e inténtalo nuevamente.';
  if (status === 409) return 'No se pudo guardar porque existe información relacionada o duplicada.';
  return 'No se pudo completar la operación. Intenta nuevamente o comunícate con el administrador si el problema persiste.';
}

export function messageForUser(value: unknown, status?: number) {
  const source = String(value || '').trim();
  if (!source || transportPattern.test(source) || technicalPattern.test(source)) return fallback(status);
  if (/^error\s*(http)?\s*\d+/i.test(source)) return fallback(status);
  return source.length > 300 ? fallback(status) : source;
}
