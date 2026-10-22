#!/usr/bin/env bash
# ============================================================================
# Xtrim — la medición de Lighthouse en modo móvil (HU-30)
#
#   scripts/lighthouse.sh [url-base]
#
# Mide el **build de producción**, servido por el servidor de SSR, con la CPU
# y la red limitadas como las de un móvil.
#
# La herramienta NO es una dependencia del proyecto: se instala fuera, para
# que el `package.json` no cargue con dependencias de medición.
#
#   npm install --prefix /tmp/lighthouse lighthouse
#   CHROME_PATH=/ruta/al/chrome scripts/lighthouse.sh
#
# Sin CHROME_PATH se usa el Chrome que hay instalado en el sistema.
# ============================================================================

set -euo pipefail

log()  { printf '\033[1;36m»\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m✗\033[0m %s\n' "$*" >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${LIGHTHOUSE_PORT:-4300}"
BASE="${1:-http://localhost:${PORT}}"
API="${PUBLIC_API_URL:-http://localhost:8081/api}"
OUT="${LIGHTHOUSE_OUT:-${ROOT}/.logs/lighthouse}"
LIGHTHOUSE="${LIGHTHOUSE_BIN:-lighthouse}"

mkdir -p "$OUT"

# Lighthouse creates a temporary directory for the browser profile. If the
# machine has no usable TMPDIR, it creates it in the working directory —with
# the name of the temporary folder of the system, which is not a name for a
# repository. Here it always goes to the logs of the project.
export TMPDIR="${OUT}/tmp"
mkdir -p "$TMPDIR"

PAGES=(
  "home:${BASE}/"
  "listing:${BASE}/productos"
  "detail:${BASE}/productos/laptop-nova-14-pulgadas"
)

if ! command -v "$LIGHTHOUSE" >/dev/null 2>&1; then
  die "Lighthouse no está instalado. Instálalo fuera del proyecto:
       npm install --prefix /tmp/lighthouse lighthouse
       export LIGHTHOUSE_BIN=/tmp/lighthouse/node_modules/.bin/lighthouse"
fi

if [ ! -d "$ROOT/frontend/dist/frontend/server" ]; then
  log "el build no existe; se compila"
  (cd "$ROOT/frontend" && npm run build)
fi

log "el sitio, en el puerto ${PORT} (la API en ${API})"
(cd "$ROOT/frontend" && setsid nohup env \
    PORT="$PORT" \
    PUBLIC_API_URL="$API" \
    SITE_BASE_URL="$BASE" \
    NG_ALLOWED_HOSTS="localhost,127.0.0.1" \
    node dist/frontend/server/server.mjs > "${ROOT}/.logs/lighthouse-web.log" 2>&1 &)

cleanup() {
  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${PORT}/tcp" 2>/dev/null || true
  fi
}
trap cleanup EXIT

for _ in $(seq 1 60); do
  if curl -fsS -o /dev/null --max-time 3 "${BASE}/" 2>/dev/null; then
    break
  fi
  sleep 1
done

if ! curl -fsS -o /dev/null --max-time 3 "${BASE}/" 2>/dev/null; then
  die "el sitio no respondió; mira ${ROOT}/.logs/lighthouse-web.log"
fi

for entry in "${PAGES[@]}"; do
  name="${entry%%:*}"
  url="${entry#*:}"
  log "${name} :: ${url}"
  "$LIGHTHOUSE" "$url" \
    --form-factor=mobile \
    --screenEmulation.mobile=true \
    --throttling-method=simulate \
    --only-categories=performance,accessibility,best-practices,seo \
    --output=json \
    --output-path="${OUT}/${name}.json" \
    --chrome-flags="--headless=new --no-sandbox --disable-dev-shm-usage --disable-gpu" \
    --quiet >/dev/null 2>&1 || warn "Lighthouse falló en ${name}"
done

log "el resumen"
node "${ROOT}/scripts/lighthouse-summary.mjs" "${OUT}"
