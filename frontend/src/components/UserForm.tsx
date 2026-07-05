import { useMemo, useState } from 'react';
import { Loader2, Save, ShieldCheck, UserRound } from 'lucide-react';
import { FormMessage, FormPrerequisite, FormSection } from './FormLayout';
import type { RoleDetailResponse, UserFormPayload } from '../types/api';

const defaultPayload: UserFormPayload = { username: '', nombreCompleto: '', email: '', cargo: '', telefono: '', avatarColor: 'emerald', rol: '', password: '', activo: true };

interface UserFormProps {
  roles: RoleDetailResponse[];
  initialData?: UserFormPayload;
  editing?: boolean;
  submitLabel?: string;
  onSubmit: (payload: UserFormPayload) => Promise<void>;
  onCancel: () => void;
}

const normalizeCorporateEmail = (value: string) => value.trim().toLowerCase();

export function UserForm({ roles, initialData, editing = false, submitLabel = 'Guardar usuario', onSubmit, onCancel }: UserFormProps) {
  const activeRoles = useMemo(() => roles.filter((role) => role.activo), [roles]);
  const firstRole = activeRoles[0]?.codigo || '';
  const [payload, setPayload] = useState<UserFormPayload>({ ...defaultPayload, ...initialData, rol: initialData?.rol || firstRole });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const selected = roles.find((role) => role.codigo === payload.rol);
  const canSubmit = activeRoles.length > 0 && Boolean(payload.rol);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) {
      setError('No hay perfiles activos disponibles para asignar a la cuenta.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await onSubmit({
        ...payload,
        username: payload.username.trim().toLowerCase(),
        nombreCompleto: payload.nombreCompleto.trim(),
        email: normalizeCorporateEmail(payload.email),
        cargo: payload.cargo?.trim() || undefined,
        telefono: payload.telefono?.trim() || undefined,
        avatarColor: payload.avatarColor || 'emerald',
        password: payload.password?.trim() || undefined
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No fue posible guardar la cuenta.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-shell" onSubmit={submit}>
      {error ? <FormMessage>{error}</FormMessage> : null}
      {!canSubmit ? <FormPrerequisite title="Se requiere un perfil activo" description="Activa al menos un perfil de acceso antes de crear o actualizar una cuenta." /> : null}
      <FormSection title="Datos de la persona" description="Esta información se mostrará en las operaciones y en el historial de cambios." icon={<UserRound size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Nombre completo</span>
            <input value={payload.nombreCompleto} onChange={(event) => setPayload({ ...payload, nombreCompleto: event.target.value })} required maxLength={150} autoComplete="name" />
          </label>
          <label>
            <span>Usuario</span>
            <input value={payload.username} onChange={(event) => setPayload({ ...payload, username: event.target.value })} required minLength={3} maxLength={50} autoComplete="username" placeholder="Ej.: rjimenez" />
            <small className="field-hint">Se usa para iniciar sesión junto con el correo empresarial.</small>
          </label>
          <label>
            <span>Correo empresarial</span>
            <input type="email" value={payload.email} onBlur={() => setPayload((current) => ({ ...current, email: normalizeCorporateEmail(current.email) }))} onChange={(event) => setPayload({ ...payload, email: event.target.value })} required maxLength={120} autoComplete="email" placeholder="nombre@vlv.agro.pe" />
          </label>
          <label>
            <span>Cargo</span>
            <input value={payload.cargo || ''} onChange={(event) => setPayload({ ...payload, cargo: event.target.value })} maxLength={90} placeholder="Ej.: Supervisor de producción" />
          </label>
          <label>
            <span>Teléfono</span>
            <input value={payload.telefono || ''} onChange={(event) => setPayload({ ...payload, telefono: event.target.value })} maxLength={30} autoComplete="tel" placeholder="Ej.: +51 956 000 100" />
          </label>
          <label>
            <span>Color de identificación</span>
            <select value={payload.avatarColor || 'emerald'} onChange={(event) => setPayload({ ...payload, avatarColor: event.target.value })}>
              <option value="emerald">Verde corporativo</option>
              <option value="blue">Azul operativo</option>
              <option value="purple">Arándano</option>
              <option value="orange">Despacho</option>
              <option value="slate">Administrativo</option>
            </select>
          </label>
        </div>
      </FormSection>

      <FormSection title="Acceso y permisos" description="Selecciona el perfil que define las acciones disponibles dentro de BlueberryTrace." icon={<ShieldCheck size={18} />}>
        <div className="form-grid form-grid--two">
          <label>
            <span>Perfil de acceso</span>
            <select value={payload.rol} onChange={(event) => setPayload({ ...payload, rol: event.target.value })} required disabled={activeRoles.length === 0}>
              <option value="" disabled>Selecciona un perfil</option>
              {activeRoles.map((role) => <option key={role.id} value={role.codigo}>{role.nombreVisible}</option>)}
            </select>
            <small className="field-hint">{selected?.descripcion || 'Elige un perfil activo para conocer el alcance del acceso.'}</small>
          </label>
          {!editing ? <label>
            <span>Contraseña inicial</span>
            <input type="password" value={payload.password || ''} onChange={(event) => setPayload({ ...payload, password: event.target.value })} required minLength={12} maxLength={128} autoComplete="new-password" />
            <small className="field-hint">La persona deberá actualizarla al ingresar por primera vez.</small>
          </label> : null}
          <label className="toggle-field form-grid__full">
            <input type="checkbox" checked={payload.activo} onChange={(event) => setPayload({ ...payload, activo: event.target.checked })} />
            <span><strong>Cuenta activa</strong><small>Permite iniciar sesión y realizar acciones según el perfil asignado.</small></span>
          </label>
        </div>
      </FormSection>

      <footer className="form-actions form-actions--sticky">
        <button type="button" className="ghost-button" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="action-button" disabled={saving || !canSubmit}>
          {saving ? <Loader2 className="spin" size={16} /> : <Save size={16} />} {saving ? 'Guardando...' : submitLabel}
        </button>
      </footer>
    </form>
  );
}
