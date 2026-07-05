import { ChangeEvent, FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { ImagePlus, KeyRound, Loader2, Mail, Phone, Save, ShieldCheck, Trash2, UploadCloud, UserRound } from 'lucide-react';
import { blueberryApi } from '../lib/api';
import { initials } from '../lib/format';
import type { AuthenticatedUserResponse, PasswordChangePayload, ProfileUpdatePayload } from '../types/api';
import { Modal } from './Modal';

interface ProfileSettingsModalProps {
  open: boolean;
  user: AuthenticatedUserResponse | null;
  onClose: () => void;
  onUpdated: (user: AuthenticatedUserResponse) => void;
  onPasswordChanged?: () => void;
  requiresPasswordChange?: boolean;
}

const avatarColors = [
  { key: 'emerald', label: 'Verde corporativo' },
  { key: 'blue', label: 'Azul operativo' },
  { key: 'purple', label: 'Arándano' },
  { key: 'orange', label: 'Despacho' },
  { key: 'slate', label: 'Administrativo' }
];

const allowedAvatarTypes = ['image/jpeg', 'image/png', 'image/webp'];
const maxAvatarSize = 1_000_000;

function normalizeCorporateEmail(value: string) {
  return value.trim().toLowerCase();
}


async function hasExpectedImageSignature(file: File) {
  const bytes = new Uint8Array(await file.slice(0, 12).arrayBuffer());
  const png = bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47 && bytes[4] === 0x0d && bytes[5] === 0x0a && bytes[6] === 0x1a && bytes[7] === 0x0a;
  const jpeg = bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
  const webp = bytes.length >= 12 && String.fromCharCode(...bytes.slice(0, 4)) === 'RIFF' && String.fromCharCode(...bytes.slice(8, 12)) === 'WEBP';
  return (file.type === 'image/png' && png) || (file.type === 'image/jpeg' && jpeg) || (file.type === 'image/webp' && webp);
}

function readAvatarFile(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('No se pudo leer la imagen seleccionada.'));
    reader.readAsDataURL(file);
  });
}

export function ProfileSettingsModal({ open, user, onClose, onUpdated, onPasswordChanged, requiresPasswordChange = false }: ProfileSettingsModalProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [profile, setProfile] = useState<ProfileUpdatePayload>({
    nombreCompleto: '',
    email: '',
    cargo: '',
    telefono: '',
    avatarColor: 'emerald',
    avatarImage: null
  });
  const [passwordPayload, setPasswordPayload] = useState<PasswordChangePayload>({ currentPassword: '', newPassword: '' });
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [avatarMessage, setAvatarMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!user || !open) {
      return;
    }
    setProfile({
      nombreCompleto: user.nombreCompleto || '',
      email: user.email || '',
      cargo: user.cargo || '',
      telefono: user.telefono || '',
      avatarColor: user.avatarColor || 'emerald',
      avatarImage: user.avatarImage || null
    });
    setPasswordPayload({ currentPassword: '', newPassword: '' });
    setProfileError(null);
    setPasswordError(null);
    setAvatarMessage(null);
  }, [user, open]);

  const roleLabel = useMemo(() => user?.rol || user?.authorities?.[0] || 'Operario', [user]);
  const avatarPreview = profile.avatarImage || null;

  async function handleAvatarFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }

    if (!allowedAvatarTypes.includes(file.type)) {
      setAvatarMessage('Formato no permitido. Usa PNG, JPG o WEBP.');
      return;
    }

    if (file.size > maxAvatarSize) {
      setAvatarMessage('La imagen debe pesar menos de 1 MB para guardarse en el perfil.');
      return;
    }

    if (!await hasExpectedImageSignature(file)) {
      setAvatarMessage('El contenido no coincide con el formato de imagen indicado.');
      return;
    }

    try {
      const avatarImage = await readAvatarFile(file);
      setProfile((current) => ({ ...current, avatarImage }));
      setAvatarMessage('Imagen lista para guardar.');
    } catch (exception) {
      setAvatarMessage(exception instanceof Error ? exception.message : 'No se pudo cargar la imagen.');
    }
  }

  async function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setSavingProfile(true);
      setProfileError(null);
      const updated = await blueberryApi.updateProfile({
        ...profile,
        nombreCompleto: profile.nombreCompleto.trim(),
        email: normalizeCorporateEmail(profile.email),
        cargo: profile.cargo?.trim() || undefined,
        telefono: profile.telefono?.trim() || undefined,
        avatarColor: profile.avatarColor || 'emerald',
        avatarImage: profile.avatarImage || null
      });
      onUpdated(updated);
    } catch (exception) {
      setProfileError(exception instanceof Error ? exception.message : 'No se pudo actualizar el perfil.');
    } finally {
      setSavingProfile(false);
    }
  }

  async function submitPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setSavingPassword(true);
      setPasswordError(null);
      await blueberryApi.changePassword({
        currentPassword: passwordPayload.currentPassword,
        newPassword: passwordPayload.newPassword.trim()
      });
      setPasswordPayload({ currentPassword: '', newPassword: '' });
      onPasswordChanged?.();
    } catch (exception) {
      setPasswordError(exception instanceof Error ? exception.message : 'No se pudo actualizar la contraseña.');
    } finally {
      setSavingPassword(false);
    }
  }

  return (
    <Modal
      open={open}
      title="Perfil del trabajador"
      description={requiresPasswordChange ? 'Debes actualizar tu contraseña temporal antes de continuar.' : 'Gestiona tus datos corporativos, foto de perfil y credenciales de acceso.'}
      size="xl"
      dismissible={!requiresPasswordChange}
      onClose={onClose}
    >
      <div className="profile-settings-layout profile-settings-layout--modern">
        <aside className="profile-identity-card profile-identity-card--modern">
          <span className={`profile-avatar profile-avatar--${profile.avatarColor || 'emerald'} ${avatarPreview ? 'profile-avatar--image' : ''}`}>
            {avatarPreview ? <img src={avatarPreview} alt="Foto de perfil" /> : initials(profile.nombreCompleto || user?.nombreCompleto)}
          </span>
          <strong>{profile.nombreCompleto || user?.nombreCompleto || 'Trabajador VLV'}</strong>
          <small>{profile.cargo || roleLabel}</small>
          <div className="profile-identity-card__meta">
            <p><Mail size={15} /> {profile.email || 'correo no registrado'}</p>
            <p><Phone size={15} /> {profile.telefono || 'teléfono no registrado'}</p>
            <p><ShieldCheck size={15} /> {roleLabel}</p>
          </div>
        </aside>

        <div className="profile-settings-panels">
          <form className="profile-settings-panel profile-settings-panel--modern" onSubmit={submitProfile}>
            <header>
              <span><UserRound size={18} /></span>
              <div>
                <strong>Datos personales</strong>
                <small>Información visible para supervisión y auditoría operativa.</small>
              </div>
            </header>

            {profileError ? <div className="form-alert">{profileError}</div> : null}

            <section className="avatar-upload-card" aria-label="Foto de perfil">
              <input ref={fileInputRef} type="file" accept="image/png,image/jpeg,image/webp" hidden onChange={handleAvatarFile} />
              <div className="avatar-upload-card__icon"><ImagePlus size={20} /></div>
              <div className="avatar-upload-card__content">
                <strong>Foto de perfil</strong>
                <small>Sube una imagen desde tu dispositivo. Formatos PNG, JPG o WEBP.</small>
                {avatarMessage ? <em>{avatarMessage}</em> : null}
              </div>
              <div className="avatar-upload-card__actions">
                <button type="button" className="ghost-button" onClick={() => fileInputRef.current?.click()}>
                  <UploadCloud size={15} /> Subir imagen
                </button>
                {avatarPreview ? (
                  <button type="button" className="ghost-button ghost-button--danger" onClick={() => { setProfile((current) => ({ ...current, avatarImage: null })); setAvatarMessage('La foto se quitará al guardar.'); }}>
                    <Trash2 size={15} /> Quitar
                  </button>
                ) : null}
              </div>
            </section>

            <div className="form-grid form-grid--two">
              <label>
                <span>Nombre completo</span>
                <input value={profile.nombreCompleto} onChange={(event) => setProfile({ ...profile, nombreCompleto: event.target.value })} required maxLength={150} />
              </label>
              <label>
                <span>Correo empresarial</span>
                <input type="email" value={profile.email} onBlur={() => setProfile((current) => ({ ...current, email: normalizeCorporateEmail(current.email) }))} onChange={(event) => setProfile({ ...profile, email: event.target.value })} required maxLength={120} />
              </label>
              <label>
                <span>Cargo</span>
                <input value={profile.cargo || ''} onChange={(event) => setProfile({ ...profile, cargo: event.target.value })} maxLength={90} placeholder="Supervisor de Producción" />
              </label>
              <label>
                <span>Teléfono</span>
                <input value={profile.telefono || ''} onChange={(event) => setProfile({ ...profile, telefono: event.target.value })} maxLength={30} placeholder="+51 956 000 100" />
              </label>
            </div>

            <div className="avatar-color-list" aria-label="Color del avatar">
              {avatarColors.map((color) => (
                <button
                  key={color.key}
                  type="button"
                  className={profile.avatarColor === color.key ? `avatar-color avatar-color--${color.key} avatar-color--active` : `avatar-color avatar-color--${color.key}`}
                  onClick={() => setProfile({ ...profile, avatarColor: color.key })}
                >
                  <span />
                  {color.label}
                </button>
              ))}
            </div>

            <footer className="profile-settings-actions">
              <button type="submit" className="action-button" disabled={savingProfile}>
                {savingProfile ? <Loader2 className="spin" size={16} /> : <Save size={16} />} Guardar perfil
              </button>
            </footer>
          </form>

          <form className="profile-settings-panel profile-settings-panel--modern" onSubmit={submitPassword}>
            <header>
              <span><KeyRound size={18} /></span>
              <div>
                <strong>Seguridad de acceso</strong>
                <small>Actualiza tu contraseña sin modificar los datos operativos.</small>
              </div>
            </header>

            {requiresPasswordChange ? <div className="form-alert">Tu cuenta tiene una contraseña temporal. Actualízala ahora para habilitar los demás módulos.</div> : null}
            {passwordError ? <div className="form-alert">{passwordError}</div> : null}

            <div className="form-grid form-grid--two">
              <label>
                <span>Contraseña actual</span>
                <input type="password" value={passwordPayload.currentPassword} onChange={(event) => setPasswordPayload({ ...passwordPayload, currentPassword: event.target.value })} required autoComplete="current-password" />
              </label>
              <label>
                <span>Nueva contraseña</span>
                <input type="password" value={passwordPayload.newPassword} onChange={(event) => setPasswordPayload({ ...passwordPayload, newPassword: event.target.value })} required minLength={12} maxLength={120} autoComplete="new-password" />
              </label>
            </div>

            <footer className="profile-settings-actions">
              <button type="submit" className="ghost-button" disabled={savingPassword}>
                {savingPassword ? <Loader2 className="spin" size={16} /> : <KeyRound size={16} />} Cambiar contraseña
              </button>
            </footer>
          </form>
        </div>
      </div>
    </Modal>
  );
}
