# frontend — Xtrim

Angular 22 with SSR. The application lives in `src/`:

| Path | What it is |
| --- | --- |
| `src/app/core/api/api-base-url.ts` | The `API_BASE_URL` token: relative `/api` in the browser, absolute `PUBLIC_API_URL` on the server |
| `src/app/app.config.ts` | Browser configuration |
| `src/app/app.config.server.ts` | Server configuration — reads `PUBLIC_API_URL` from the environment |
| `src/server.ts` | The Node/Express server that Render runs; proxies `/api` to the API |
| `proxy.conf.json` | Dev-server proxy: `/api` → `http://localhost:8080` |

## Commands

```bash
npm ci          # install
npm start       # dev server with SSR on http://localhost:4200
npm run build   # production build (browser + server bundles)
npm test        # unit tests
```

The full setup — the stack, the backend and the environment contract — is
documented in the [root README](../README.md).
