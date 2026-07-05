import { FormEvent, useState } from 'react';
import { ArrowRight, Eye, EyeOff, HelpCircle, Loader2, LockKeyhole, ShieldCheck, UserRound } from 'lucide-react';
import { blueberryApi } from '../lib/api';
import blueberryLogoMark from '../assets/brand/blueberry-logo-mark.webp';
import type { AuthenticatedUserResponse } from '../types/api';

interface LoginPageProps {
  onAuthenticated: (user: AuthenticatedUserResponse) => Promise<void> | void;
}

export function LoginPage({ onAuthenticated }: LoginPageProps) {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setLoading(true);
      setError(null);
      const user = await blueberryApi.login({ identifier, password });
      await onAuthenticated(user);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo iniciar sesión.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="vlv-login-shell vlv-login-shell--reference">
        <section className="vlv-login-hero vlv-login-hero--solid" aria-label="Presentación BlueberryTrace">
            <div className="vlv-login-brand" aria-label="BlueberryTrace">
                <img src={blueberryLogoMark} alt="Logo BlueberryTrace" />
                <strong><span>Blueberry</span>Trace</strong>
            </div>

            <div className="vlv-login-copy">
                <h1>Control agrícola inteligente</h1>
                <p>
                    Administre y supervise la información agrícola del vivero de forma ordenada,
                    segura y eficiente desde una plataforma moderna y fácil de usar.
                </p>
            </div>

            <p className="vlv-login-hero-footer">© 2026 BlueberryTrace. Sistema interno del vivero.</p>
        </section>

      <section className="vlv-login-form-panel" aria-label="Inicio de sesión">
        <form className="vlv-login-card" onSubmit={handleSubmit}>
          <strong className="vlv-login-product">BlueberryTrace</strong>
          <div className="vlv-login-card__heading">
            <h2>Bienvenido</h2>
            <p>Ingrese con su usuario o correo institucional.</p>
          </div>

          <label className="vlv-field">
            <span>Usuario o correo</span>
            <div className="vlv-field__control">
              <UserRound size={21} strokeWidth={1.8} />
              <input
                value={identifier}
                onChange={(event) => setIdentifier(event.target.value)}
                autoComplete="username"
                placeholder="Usuario o correo"
                required
              />
            </div>
          </label>

          <label className="vlv-field">
            <span>Contraseña</span>
            <div className="vlv-field__control">
              <LockKeyhole size={21} strokeWidth={1.8} />
              <input
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Contraseña"
                required
              />
              <button
                type="button"
                className="vlv-field__icon-button"
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                onClick={() => setShowPassword((current) => !current)}
              >
                {showPassword ? <EyeOff size={19} /> : <Eye size={19} />}
              </button>
            </div>
          </label>

          {error && <p className="vlv-login-error">{error}</p>}

          <button className="vlv-login-submit" type="submit" disabled={loading}>
            <span>{loading ? 'Validando acceso' : 'Iniciar sesión'}</span>
            {loading ? <Loader2 className="spin" size={20} /> : <ArrowRight size={22} />}
          </button>

          <button type="button" className="vlv-login-help">
            <HelpCircle size={19} />
            ¿Necesita ayuda para iniciar sesión?
          </button>

          <div className="vlv-login-secure-note">
            <ShieldCheck size={30} strokeWidth={1.75} />
            <div>
              <strong>Acceso seguro y confidencial</strong>
              <small>BlueberryTrace es una aplicación interna.</small>
            </div>
          </div>
        </form>
      </section>
    </main>
  );
}
