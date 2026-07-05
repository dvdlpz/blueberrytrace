import { useMemo, useState } from 'react';
import { Eye, KeyRound, Pencil, Plus, Search, ShieldCheck, UserCheck, UsersRound, UserX } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DetailDrawer } from '../components/DetailDrawer';
import { InfoGrid } from '../components/InfoGrid';
import { Modal } from '../components/Modal';
import { FormSection } from '../components/FormLayout';
import { ModuleHeader } from '../components/ModuleHeader';
import { StatusBadge } from '../components/StatusBadge';
import { RoleBadge } from '../components/RoleBadge';
import { UserForm } from '../components/UserForm';
import { blueberryApi } from '../lib/api';
import { dateShort, initials } from '../lib/format';
import { emitToast } from '../lib/uiEvents';
import type { RoleDetailResponse, UserFormPayload, UserReferenceResponse } from '../types/api';

interface UsuariosPageProps {
  usuarios: UserReferenceResponse[];
  roles: RoleDetailResponse[];
  onUsuariosChange: (items: UserReferenceResponse[]) => void;
}

function toUserPayload(usuario: UserReferenceResponse): UserFormPayload {
  return {
    username: usuario.username,
    nombreCompleto: usuario.nombreCompleto,
    email: usuario.email,
    cargo: usuario.cargo || '',
    telefono: usuario.telefono || '',
    avatarColor: usuario.avatarColor || 'emerald',
    rol: usuario.rol || 'OPERARIO',
    password: '',
    activo: usuario.activo
  };
}

export function UsuariosPage({ usuarios, roles, onUsuariosChange }: UsuariosPageProps) {
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('TODOS');
  const [statusFilter, setStatusFilter] = useState('TODOS');
  const [selectedUser, setSelectedUser] = useState<UserReferenceResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [editingUser, setEditingUser] = useState<UserReferenceResponse | null>(null);
  const [confirmUser, setConfirmUser] = useState<UserReferenceResponse | null>(null);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [resettingUser, setResettingUser] = useState<UserReferenceResponse | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState('');

  const availableRoles = useMemo(() => roles.filter((role) => role.activo), [roles]);
  const roleLabels = useMemo(() => availableRoles.map((role) => role.codigo), [availableRoles]);
  const rolesByCode = useMemo(() => new Map(roles.map((role) => [role.codigo, role])), [roles]);

  function roleBadge(roleCode?: string | null) {
    const role = rolesByCode.get(roleCode || '');
    return <RoleBadge label={role?.nombreVisible || roleCode || 'Sin rol'} color={role?.color} />;
  }

  const activos = usuarios.filter((usuario) => usuario.activo).length;
  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return usuarios.filter((usuario) => {
      const matchesQuery = !term || [usuario.nombreCompleto, usuario.email, usuario.username, usuario.rol]
        .some((value) => String(value || '').toLowerCase().includes(term));
      const matchesRole = roleFilter === 'TODOS' || usuario.rol === roleFilter;
      const matchesStatus = statusFilter === 'TODOS' || (statusFilter === 'ACTIVO' ? usuario.activo : !usuario.activo);
      return matchesQuery && matchesRole && matchesStatus;
    });
  }, [usuarios, query, roleFilter, statusFilter]);

  async function createUser(payload: UserFormPayload) {
    const response = await blueberryApi.createUsuario(payload);
    onUsuariosChange(response.items);
    setCreating(false);
    emitToast('success', 'Usuario creado', `${payload.nombreCompleto} ya puede operar con su cuenta corporativa.`);
  }

  async function updateUser(payload: UserFormPayload) {
    if (!editingUser) {
      return;
    }
    const response = await blueberryApi.updateUsuario(editingUser.id, payload);
    onUsuariosChange(response.items);
    setEditingUser(null);
    setSelectedUser(response.items.find((item) => item.id === editingUser.id) || null);
    emitToast('success', 'Usuario actualizado', `${payload.nombreCompleto} fue actualizado correctamente.`);
  }

  async function toggleUserStatus() {
    if (!confirmUser) {
      return;
    }
    try {
      setConfirmLoading(true);
      const response = await blueberryApi.toggleUsuarioStatus(confirmUser.id);
      onUsuariosChange(response.items);
      const updated = response.items.find((item) => item.id === confirmUser.id);
      setSelectedUser(updated || null);
      emitToast('info', 'Estado actualizado', `${confirmUser.nombreCompleto} quedó ${updated?.activo ? 'activo' : 'inactivo'}.`);
      setConfirmUser(null);
    } finally {
      setConfirmLoading(false);
    }
  }

  async function resetPassword() {
    if (!resettingUser) return;
    try {
      setConfirmLoading(true);
      await blueberryApi.resetUsuarioPassword(resettingUser.id, temporaryPassword.trim());
      setResettingUser(null);
      setTemporaryPassword('');
      emitToast('success', 'Contraseña temporal configurada', `${resettingUser.nombreCompleto} deberá actualizarla al iniciar sesión.`);
    } catch (exception) {
      emitToast('error', 'No se pudo restablecer la contraseña', exception instanceof Error ? exception.message : 'Ocurrió un error inesperado.');
    } finally {
      setConfirmLoading(false);
    }
  }

  return (
    <main className="content-grid">
      <ModuleHeader
        eyebrow="Seguridad"
        title="Gestión de Usuarios"
        description="Administra cuentas corporativas, roles operativos y acceso al sistema."
        icon={<UsersRound size={21} />}
        tone="purple"
        actions={<button className="action-button" type="button" onClick={() => setCreating(true)}><Plus size={16} /> Nuevo usuario</button>}
      />

      <section className="summary-strip summary-strip--three">
        <article className="summary-pill summary-pill--green"><span className="summary-pill__icon"><UsersRound size={18} /></span><strong>{usuarios.length}</strong><span>Usuarios</span><small>cuentas registradas</small></article>
        <article className="summary-pill summary-pill--blue"><span className="summary-pill__icon"><UserCheck size={18} /></span><strong>{activos}</strong><span>Activos</span><small>con acceso habilitado</small></article>
        <article className="summary-pill summary-pill--purple"><span className="summary-pill__icon"><ShieldCheck size={18} /></span><strong>{roleLabels.length}</strong><span>Roles</span><small>perfiles corporativos</small></article>
      </section>

      <section className="panel-card panel-card--interactive">
        <div className="module-toolbar-card module-toolbar-card--filters">
          <label className="filter-toolbar__search">
            <Search size={16} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar usuario, correo o rol..." />
          </label>
          <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}>
            <option value="TODOS">Todos los roles</option>
            {availableRoles.map((role) => <option key={role.id} value={role.codigo}>{role.nombreVisible}</option>)}
          </select>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="TODOS">Todos los estados</option>
            <option value="ACTIVO">Activo</option>
            <option value="INACTIVO">Inactivo</option>
          </select>
        </div>

        <div className="data-table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Actualización</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((usuario) => (
                <tr key={usuario.id}>
                  <td>
                    <div className="user-row">
                      <span className={`user-row__avatar user-row__avatar--${usuario.avatarColor || 'emerald'}`}>{initials(usuario.nombreCompleto)}</span>
                      <div>
                        <strong>{usuario.nombreCompleto}</strong>
                        <small>{usuario.cargo || usuario.email}</small>
                      </div>
                    </div>
                  </td>
                  <td>{roleBadge(usuario.rol)}</td>
                  <td><StatusBadge value={usuario.activo ? 'Activo' : 'Inactivo'} /></td>
                  <td>{dateShort(usuario.fechaActualizacion || usuario.fechaCreacion)}</td>
                  <td>
                    <div className="icon-actions">
                      <button type="button" className="icon-action" title="Ver detalle" onClick={() => setSelectedUser(usuario)}><Eye size={15} /></button>
                      <button type="button" className="icon-action" title="Editar" onClick={() => setEditingUser(usuario)}><Pencil size={15} /></button>
                      <button type="button" className="icon-action" title="Restablecer contraseña" onClick={() => setResettingUser(usuario)}><KeyRound size={15} /></button>
                      <button type="button" className="icon-action" title={usuario.activo ? 'Desactivar usuario' : 'Activar usuario'} onClick={() => setConfirmUser(usuario)}><UserX size={15} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filtered.length === 0 ? (
          <div className="empty-state empty-state--inline">
            <ShieldCheck size={24} />
            <strong>No hay usuarios con esos filtros</strong>
            <span>Prueba con otro correo, rol o estado.</span>
          </div>
        ) : null}

        <div className="table-footer-note">Mostrando {filtered.length} de {usuarios.length} usuarios registrados</div>
      </section>

      <DetailDrawer
        open={Boolean(selectedUser)}
        title={selectedUser?.nombreCompleto || 'Detalle de usuario'}
        subtitle={selectedUser?.email || selectedUser?.username}
        onClose={() => setSelectedUser(null)}
        actions={selectedUser ? (
          <>
            <button type="button" className="ghost-button" onClick={() => setResettingUser(selectedUser)}><KeyRound size={15} /> Restablecer contraseña</button>
            <button type="button" className="ghost-button" onClick={() => setConfirmUser(selectedUser)}><KeyRound size={15} /> {selectedUser.activo ? 'Desactivar' : 'Activar'}</button>
            <button type="button" className="action-button" onClick={() => setEditingUser(selectedUser)}><Pencil size={15} /> Editar usuario</button>
          </>
        ) : null}
      >
        {selectedUser ? (
          <>
            <InfoGrid
              items={[
                { label: 'Usuario', value: selectedUser.username, tone: 'green' },
                { label: 'Rol', value: roleBadge(selectedUser.rol), tone: 'purple' },
                { label: 'Estado', value: <StatusBadge value={selectedUser.activo ? 'Activo' : 'Inactivo'} />, tone: selectedUser.activo ? 'blue' : 'neutral' },
                { label: 'Correo', value: selectedUser.email },
                { label: 'Cargo', value: selectedUser.cargo || 'Sin cargo registrado' },
                { label: 'Teléfono', value: selectedUser.telefono || 'Sin teléfono registrado' },
                { label: 'Creación', value: dateShort(selectedUser.fechaCreacion), tone: 'orange' },
                { label: 'Actualización', value: dateShort(selectedUser.fechaActualizacion || selectedUser.fechaCreacion) }
              ]}
            />
            <section className="drawer-section drawer-section--soft">
              <h3>Cuenta empresarial</h3>
              <p>Este usuario opera con un correo corporativo autorizado y sus permisos dependen del rol asignado por administración.</p>
            </section>
          </>
        ) : null}
      </DetailDrawer>

      <Modal
        open={creating}
        title="Nuevo usuario corporativo"
        description="Crea una cuenta operativa para el equipo del vivero."
        size="md"
        onClose={() => setCreating(false)}
      >
        <UserForm roles={availableRoles} onSubmit={createUser} onCancel={() => setCreating(false)} />
      </Modal>

      <Modal
        open={Boolean(editingUser)}
        title="Editar usuario"
        description="Actualiza datos, rol, estado o contraseña sin salir del módulo."
        size="md"
        onClose={() => setEditingUser(null)}
      >
        {editingUser ? (
          <UserForm
            roles={availableRoles}
            initialData={toUserPayload(editingUser)}
            editing
            submitLabel="Guardar cambios"
            onSubmit={updateUser}
            onCancel={() => setEditingUser(null)}
          />
        ) : null}
      </Modal>

      <Modal open={Boolean(resettingUser)} title="Restablecer contraseña" description="Configura una contraseña temporal. La persona deberá cambiarla antes de volver a operar." size="md" onClose={() => { setResettingUser(null); setTemporaryPassword(''); }}>
        <form className="form-shell" onSubmit={(event) => { event.preventDefault(); void resetPassword(); }}>
          <FormSection title="Acceso temporal" description="Usa una contraseña de al menos 12 caracteres y comunícala por un canal seguro.">
            <div className="form-grid">
              <p className="form-grid__full form-readonly-note">Cuenta: {resettingUser?.nombreCompleto || 'Usuario seleccionado'}</p>
              <label className="form-grid__full"><span>Contraseña temporal</span>
                <input type="password" value={temporaryPassword} onChange={(event) => setTemporaryPassword(event.target.value)} required minLength={12} maxLength={128} autoComplete="new-password" placeholder="Crea una contraseña temporal segura" />
              </label>
            </div>
          </FormSection>
          <footer className="form-actions form-actions--sticky"><button type="button" className="ghost-button" onClick={() => { setResettingUser(null); setTemporaryPassword(''); }}>Cancelar</button><button type="submit" className="action-button" disabled={confirmLoading || temporaryPassword.trim().length < 12}>Restablecer contraseña</button></footer>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(confirmUser)}
        title={confirmUser?.activo ? 'Desactivar usuario' : 'Activar usuario'}
        description={confirmUser ? `Se cambiará el estado de ${confirmUser.nombreCompleto}. La persona dejará de tener acceso hasta que su cuenta vuelva a activarse.` : ''}
        confirmLabel={confirmUser?.activo ? 'Desactivar' : 'Activar'}
        tone={confirmUser?.activo ? 'danger' : 'success'}
        loading={confirmLoading}
        onCancel={() => setConfirmUser(null)}
        onConfirm={toggleUserStatus}
      />
    </main>
  );
}
