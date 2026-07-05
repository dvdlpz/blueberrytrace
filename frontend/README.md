# Frontend BlueberryTrace

Cliente React/Vite/TypeScript de BlueberryTrace.

## Variables

Copia `.env.example` a `.env` solo para desarrollo. La producción usa `.env.production` y consume `/api/v1` bajo el mismo dominio mediante Nginx.

```bash
npm ci
npm run build
npm run dev
```

## Reportes

El módulo de reportes técnicos exporta PDF y XLSX nativos desde la vista filtrada. La API no se expone en otro dominio para mantener sesión, CSRF y CORS coherentes.
