import { useEffect, useMemo, useState } from 'react';
import type { CSSProperties } from 'react';
import { Check, CheckCircle2, CircleAlert, LockKeyhole, Paintbrush, Pencil, ShieldCheck, UsersRound, XCircle } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DetailDrawer } from '../components/DetailDrawer';
import { EmptyState } from '../components/EmptyState';
import { FormSection } from '../components/FormLayout';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { ModuleHeader } from '../components/ModuleHeader';
import { RoleBadge } from '../components/RoleBadge';
import { StatusBadge } from '../components/StatusBadge';
import { blueberryApi } from '../lib/api';
import { emitToast } from '../lib/uiEvents';
import type { RoleDetailResponse, RolePermissionSelectionPayload } from '../types/api';

interface RolesPageProps {
  onRolesChange?: (roles: RoleDetailResponse[]) => void;
}

const COLOR_OPTIONS = ['#6D28D9', '#0F766E', '#C26A08', '#0E7490', '#2563EB', '#B91C1C', '#475569'];

const ACTION_LABELS: Record<string, string> = {
  ver: 'Consultar',
  crear: 'Registrar',
  editar: 'Editar',
  activar: 'Activar',
  cambiar_estado: 'Cambiar estado',
  archivar: 'Archivar',
  anular: 'Anular',
  validar: 'Validar',
  observar: 'Observar',
  exportar: 'Exportar',
  normalizar_legado: 'Regularizar históricos',
  editar_descripcion: 'Editar información',
  editar_permisos: 'Administrar permisos',
  restablecer_contrasena: 'Restablecer contraseña'
};

function permissionKey(module: string, action: string) {
  return `${module}.${action}`;
}

function actionLabel(action: string) {
  return ACTION_LABELS[action] || action.replace(/_/g, ' ');
}

function selectedPermissions(role: RoleDetailResponse) {
  return new Set(role.permisos.flatMap((permission) => permission.actions.map((action) => permissionKey(permission.module, action))));
}

function actionCount(role: RoleDetailResponse) {
  return role.permisos.reduce((total, permission) => total + permission.actions.length, 0);
}

export function RolesPage({ onRolesChange }: RolesPageProps) {
  const [roles, setRoles] = useState<RoleDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<RoleDetailResponse | null>(null);
  const [editing, setEditing] = useState<RoleDetailResponse | null>(null);
  const [description, setDescription] = useState('');
  const [color, setColor] = useState('#2563EB');
  const [permissionSelections, setPermissionSelections] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [pendingState, setPendingState] = useState<RoleDetailResponse | null>(null);

  async function reload() {
    try {
      setLoading(true);
      setError(null);
      const response = await blueberryApi.roles();
      setRoles(response);
      onRolesChange?.(response);
      setSelected((current) => current ? response.find((item) => item.id === current.id) || null : null);
      setEditing((current) => current ? response.find((item) => item.id === current.id) || null : null);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudieron cargar los roles.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void reload(); }, []);

  const summary = useMemo(() => ({
    active: roles.filter((role) => role.activo).length,
    users: roles.reduce((total, role) => total + role.usuariosActivos, 0),
    permissions: roles.reduce((total, role) => total + actionCount(role), 0)
  }), [roles]);

  const mandatory = useMemo(() => new Set(editing?.permisosObligatorios || []), [editing]);
  const enabledActionCount = permissionSelections.size;

  function openEditor(role: RoleDetailResponse) {
    setEditing(role);
    setDescription(role.descripcion || '');
    setColor(role.color || '#2563EB');
    setPermissionSelections(selectedPermissions(role));
  }

  function togglePermission(module: string, action: string) {
    const key = permissionKey(module, action);
    if (mandatory.has(key)) return;

    setPermissionSelections((current) => {
      const next = new Set(current);
      if (action === 'ver') {
        if (next.has(key)) {
          next.delete(key);
          editing?.permisos.find((permission) => permission.module === module)?.accionesDisponibles
            .filter((availableAction) => availableAction !== 'ver')
            .forEach((availableAction) => next.delete(permissionKey(module, availableAction)));
        } else {
          next.add(key);
        }
      } else if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
        next.add(permissionKey(module, 'ver'));
      }
      return next;
    });
  }

  function selectAllAvailable() {
    if (!editing) return;
    setPermissionSelections(new Set(editing.permisos.flatMap((permission) => permission.accionesDisponibles.map((action) => permissionKey(permission.module, action)))));
  }

  function keepConsultationOnly() {
    if (!editing) return;
    const selected = new Set(editing.permisos.map((permission) => permissionKey(permission.module, 'ver')));
    mandatory.forEach((key) => selected.add(key));
    setPermissionSelections(selected);
  }

  async function saveRole() {
    if (!editing) return;
    const cleanDescription = description.trim();
    if (!cleanDescription) {
      emitToast('warning', 'Completa la descripción', 'Describe brevemente la responsabilidad de este perfil.');
      return;
    }
    try {
      setSaving(true);
      const permisos: RolePermissionSelectionPayload[] = Array.from(permissionSelections)
        .map((key) => {
          const [module, accion] = key.split('.', 2);
          return module && accion ? { module, accion } : null;
        })
        .filter((item): item is RolePermissionSelectionPayload => item !== null);
      await blueberryApi.updateRole(editing.id, { descripcion: cleanDescription, color, permisos });
      const roleName = editing.nombreVisible;
      setEditing(null);
      await reload();
      emitToast('success', 'Perfil actualizado', `Los permisos y la presentación de ${roleName} fueron actualizados.`);
    } catch (exception) {
      emitToast('error', 'No se pudo actualizar el perfil', exception instanceof Error ? exception.message : 'Revisa la información e inténtalo nuevamente.');
    } finally {
      setSaving(false);
    }
  }

  async function changeState() {
    if (!pendingState) return;
    try {
      setSaving(true);
      await blueberryApi.changeRoleState(pendingState.id, !pendingState.activo);
      const roleName = pendingState.nombreVisible;
      const wasActive = pendingState.activo;
      setPendingState(null);
      await reload();
      emitToast('success', 'Estado actualizado', `El rol ${roleName} fue ${wasActive ? 'desactivado' : 'activado'}.`);
    } catch (exception) {
      emitToast('error', 'No se pudo cambiar el estado', exception instanceof Error ? exception.message : 'Revisa los usuarios asignados antes de continuar.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Seguridad y gobierno"
        title="Roles y permisos"
        description="Define qué puede consultar o registrar cada perfil corporativo, sin crear roles fuera de la estructura aprobada."
        icon={<ShieldCheck size={21} />}
        tone="purple"
      />

      <section className="summary-strip summary-strip--three">
        <article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><ShieldCheck size={18} /></span><strong>{roles.length}</strong><span>Roles definidos</span><small>perfiles corporativos</small></article>
        <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><CheckCircle2 size={18} /></span><strong>{summary.active}</strong><span>Roles activos</span><small>disponibles para asignación</small></article>
        <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><UsersRound size={18} /></span><strong>{summary.users}</strong><span>Usuarios activos</span><small>{summary.permissions} acciones habilitadas</small></article>
      </section>

      {error ? <section className="panel-card"><EmptyState icon={<XCircle size={27} />} title="No se pudieron cargar los roles" description={error} action={<button type="button" className="action-button" onClick={() => void reload()}>Reintentar</button>} /></section> : null}
      {!error && !loading && roles.length === 0 ? <section className="panel-card"><EmptyState icon={<ShieldCheck size={27} />} title="No hay roles disponibles" description="Los perfiles corporativos aparecerán aquí cuando la administración los habilite." /></section> : null}

      {!error && roles.length > 0 ? <section className="panel-card panel-card--interactive roles-matrix-card">
        <div className="panel-card__header">
          <div><h2>Perfiles corporativos</h2><p>Personaliza la descripción, el color y las acciones autorizadas de cada perfil corporativo.</p></div>
        </div>
        <div className="data-table-wrap"><table className="data-table roles-data-table"><thead><tr><th>Rol</th><th>Estado</th><th>Usuarios</th><th>Acciones habilitadas</th><th aria-label="Acciones" /></tr></thead><tbody>{roles.map((role) => <tr key={role.id}>
          <td><div className="role-cell"><RoleBadge label={role.nombreVisible} color={role.color} /><small className="table-subtext">{role.descripcion || 'Perfil corporativo sin descripción registrada.'}</small></div></td>
          <td><StatusBadge value={role.activo ? 'Activo' : 'Inactivo'} /></td>
          <td><strong>{role.usuariosActivos}</strong><small className="table-subtext">de {role.usuariosTotales} asignados</small></td>
          <td><span className="role-action-count"><Check size={14} /> {actionCount(role)} acciones</span></td>
          <td><div className="icon-actions"><button type="button" className="icon-action" title="Ver resumen" onClick={() => setSelected(role)}><ShieldCheck size={15} /></button><button type="button" className="icon-action" title="Editar rol" onClick={() => openEditor(role)}><Pencil size={15} /></button><button type="button" className={role.activo ? 'icon-action icon-action--danger' : 'icon-action'} title={role.activo ? 'Desactivar rol' : 'Activar rol'} onClick={() => setPendingState(role)}>{role.activo ? <XCircle size={15} /> : <CheckCircle2 size={15} />}</button></div></td>
        </tr>)}</tbody></table></div>
      </section> : null}

      <DetailDrawer
        open={Boolean(selected)}
        title={selected?.nombreVisible || 'Rol'}
        subtitle={selected ? `Perfil ${selected.nombreVisible}` : ''}
        onClose={() => setSelected(null)}
        actions={selected ? <button type="button" className="action-button" onClick={() => openEditor(selected)}><Pencil size={15} /> Editar rol</button> : null}
      >
        {selected ? <>
          <InfoGrid items={[
            { label: 'Perfil', value: <RoleBadge label={selected.nombreVisible} color={selected.color} />, tone: 'purple' },
            { label: 'Estado', value: <StatusBadge value={selected.activo ? 'Activo' : 'Inactivo'} />, tone: selected.activo ? 'green' : 'neutral' },
            { label: 'Usuarios activos', value: String(selected.usuariosActivos), tone: 'blue' },
            { label: 'Acciones habilitadas', value: String(actionCount(selected)), tone: 'purple' }
          ]} />
          <section className="drawer-section"><h3>Responsabilidad del perfil</h3><p>{selected.descripcion || 'Sin descripción registrada.'}</p></section>
          <section className="drawer-section"><h3>Acciones autorizadas</h3><div className="role-permission-summary">{selected.permisos.filter((permission) => permission.actions.length > 0).map((permission) => <article key={permission.module}><strong>{permission.label}</strong><div>{permission.actions.map((action) => <span className="tag" key={action}>{actionLabel(action)}</span>)}</div></article>)}</div></section>
          <section className="drawer-section"><h3>Personas asignadas</h3>{selected.usuariosAsignados.length === 0 ? <p>No hay personas asignadas a este perfil.</p> : <div className="tag-list">{selected.usuariosAsignados.map((user) => <span className="tag" key={user.id}>{user.nombreCompleto} · {user.activo ? 'Activo' : 'Inactivo'}</span>)}</div>}</section>
        </> : null}
      </DetailDrawer>

      <Modal
        open={Boolean(editing)}
        title={editing ? `Configurar ${editing.nombreVisible}` : 'Configurar rol'}
        description="Personaliza la presentación y marca las acciones que este perfil puede realizar dentro del sistema."
        eyebrow="Administración de perfiles"
        icon={<Paintbrush size={19} />}
        size="xl"
        onClose={() => setEditing(null)}
      >
        {editing ? <form className="form-shell role-editor" onSubmit={(event) => { event.preventDefault(); void saveRole(); }}>
          <FormSection title="Identidad del perfil" description="La identificación del perfil se mantiene. La descripción y el color ayudan a reconocerlo en las pantallas operativas.">
            <div className="form-grid">
              <label><span>Nombre visible</span><input value={editing.nombreVisible} readOnly /></label>
              <label><span>Identificador del perfil</span><input value={editing.codigo.replace(/_/g, ' ')} readOnly /></label>
              <label className="form-grid__full"><span>Descripción</span><textarea value={description} onChange={(event) => setDescription(event.target.value)} maxLength={255} required placeholder="Describe la responsabilidad principal de este perfil" /></label>
              <div className="form-grid__full role-color-field">
                <div><span>Color identificador</span><p>Se usa para reconocer el rol en listados y fichas de usuario.</p></div>
                <div className="role-color-controls">
                  <input aria-label="Elegir color del rol" type="color" value={color} onChange={(event) => setColor(event.target.value.toUpperCase())} />
                  <input aria-label="Código de color" value={color} onChange={(event) => setColor(event.target.value.toUpperCase())} pattern="^#[0-9A-Fa-f]{6}$" maxLength={7} required />
                  <div className="role-color-swatches" aria-label="Colores sugeridos">{COLOR_OPTIONS.map((suggested) => <button key={suggested} type="button" className={color === suggested ? 'role-color-swatch role-color-swatch--selected' : 'role-color-swatch'} style={{ '--swatch-color': suggested } as CSSProperties} onClick={() => setColor(suggested)} aria-label={`Usar color ${suggested}`} />)}</div>
                </div>
              </div>
            </div>
          </FormSection>

          <FormSection title="Permisos del perfil" description="Marca cada acción de forma individual. Quitar “Consultar” desactiva las acciones asociadas a esa área.">
            <div className="role-permission-toolbar">
              <div><strong>{enabledActionCount} acciones habilitadas</strong><span>Los cambios se aplican a las personas asignadas al perfil.</span></div>
              <div className="button-group"><button type="button" className="ghost-button" onClick={keepConsultationOnly}>Solo consulta</button><button type="button" className="ghost-button" onClick={selectAllAvailable}>Habilitar todas</button></div>
            </div>
            <div className="role-permission-grid">
              {editing.permisos.map((permission) => {
                const activeActions = permission.accionesDisponibles.filter((action) => permissionSelections.has(permissionKey(permission.module, action)));
                return <article className="role-permission-card" key={permission.module}>
                  <header><div><strong>{permission.label}</strong><span>{activeActions.length} de {permission.accionesDisponibles.length} acciones habilitadas</span></div><span className={activeActions.length > 0 ? 'role-permission-card__state role-permission-card__state--active' : 'role-permission-card__state'}>{activeActions.length > 0 ? 'Habilitado' : 'Sin acceso'}</span></header>
                  <div className="role-permission-options">{permission.accionesDisponibles.map((action) => {
                    const key = permissionKey(permission.module, action);
                    const isLocked = mandatory.has(key);
                    const isChecked = permissionSelections.has(key);
                    return <label className={isLocked ? 'permission-option permission-option--locked' : isChecked ? 'permission-option permission-option--checked' : 'permission-option'} key={key}>
                      <input type="checkbox" checked={isChecked} disabled={isLocked} onChange={() => togglePermission(permission.module, action)} />
                      <span className="permission-option__check">{isChecked ? <Check size={14} /> : null}</span>
                      <span>{actionLabel(action)}</span>
                      {isLocked ? <LockKeyhole size={13} aria-label="Acceso esencial" /> : null}
                    </label>;
                  })}</div>
                </article>;
              })}
            </div>
            <div className="role-editor__note"><CircleAlert size={16} /><span>Los accesos esenciales de administración se mantienen protegidos para evitar que el sistema quede sin gestión autorizada.</span></div>
          </FormSection>
          <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={() => setEditing(null)} disabled={saving}>Cancelar</button><button type="submit" className="action-button" disabled={saving}>{saving ? 'Guardando cambios...' : 'Guardar permisos y presentación'}</button></footer>
        </form> : null}
      </Modal>

      <ConfirmDialog open={Boolean(pendingState)} title={pendingState?.activo ? 'Desactivar rol' : 'Activar rol'} description={pendingState?.activo ? `El rol ${pendingState.nombreVisible} solo puede desactivarse cuando no tenga personas activas asignadas.` : `El rol ${pendingState?.nombreVisible || ''} volverá a estar disponible para asignación.`} confirmLabel={pendingState?.activo ? 'Desactivar rol' : 'Activar rol'} tone={pendingState?.activo ? 'danger' : 'success'} loading={saving} onCancel={() => setPendingState(null)} onConfirm={changeState} />
    </main>
  );
}
