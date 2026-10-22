#!/usr/bin/env bash
# ============================================================================
# Xtrim — arranque local
#
#   scripts/dev.sh start    levanta la pila, la API y el sitio
#   scripts/dev.sh stop     los para
#   scripts/dev.sh status   dice cómo está cada cosa
#   scripts/dev.sh restart  stop + start
#
# Los puertos salen del contrato de configuración (.env):
#   SERVER_PORT       el de la API          (8080 por defecto)
#   POSTGRES_PORT     el de la base de datos
#
# El sitio de desarrollo reenvía /api al API_PROXY_TARGET, que por defecto es
# el puerto de la API. Si la API se mueve, hay que mover también el proxy:
#
#   API_PROXY_TARGET=http://localhost:8081 npm start
# ============================================================================

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS="$ROOT/.logs"
mkdir -p "$LOGS"

# --- el contrato de configuración -------------------------------------------
if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

API_PORT="${SERVER_PORT:-8080}"
WEB_PORT="${WEB_PORT:-4200}"

# En local, el SSR y el proxy hablan SIEMPRE con la API de esta máquina: el
# PUBLIC_API_URL del .env es el del despliegue (Render), no el de aquí. Por eso
# estas dos direcciones se derivan del puerto y no se leen del .env.
PROXY_TARGET="http://localhost:${API_PORT}"
PUBLIC_API_URL="http://localhost:${API_PORT}/api"
SITE_BASE_URL="http://localhost:${WEB_PORT}"

log()  { printf '\033[1;36m»\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m✗\033[0m %s\n' "$*" >&2; exit 1; }

# --- esperar a que algo responda ---------------------------------------------
wait_for() {
  local url="$1" what="$2" tries="${3:-60}"
  for _ in $(seq 1 "$tries"); do
    if curl -fsS -o /dev/null --max-time 3 "$url" 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  return 1
}

stop() {
  log "parando"
  pkill -f 'mvnw spring-boot:run' 2>/dev/null || true
  pkill -f 'com.anderk.ecommerce.EcommerceApplication' 2>/dev/null || true
  pkill -f 'ng serve' 2>/dev/null || true
  pkill -f 'node dist/frontend/server/server.mjs' 2>/dev/null || true
  # Y lo que quede escuchando en el puerto de la API.
  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${API_PORT}/tcp" 2>/dev/null || true
  fi
  sleep 2
  log "parado"
}

start() {
  command -v docker >/dev/null || die "hace falta Docker"
  command -v node >/dev/null || die "hace falta Node.js"

  log "la pila de datos (PostgreSQL y Redis)"
  (cd "$ROOT" && docker compose up -d)

  log "la API, en el puerto ${API_PORT}"
  (cd "$ROOT/backend" && setsid nohup ./mvnw spring-boot:run > "$LOGS/api.log" 2>&1 &)

  if ! wait_for "http://localhost:${API_PORT}/api/health" "la API" 120; then
    warn "la API no respondió a tiempo — mira $LOGS/api.log"
  fi

  log "el sitio, en el puerto ${WEB_PORT}"
  (cd "$ROOT/frontend" && setsid nohup env \
      API_PROXY_TARGET="$PROXY_TARGET" \
      PUBLIC_API_URL="$PUBLIC_API_URL" \
      SITE_BASE_URL="$SITE_BASE_URL" \
      npm start > "$LOGS/web.log" 2>&1 &)

  if ! wait_for "http://localhost:${WEB_PORT}/" "el sitio" 120; then
    warn "el sitio no respondió a tiempo — mira $LOGS/web.log"
  fi

  echo
  log "listo"
  printf '    sitio   http://localhost:%s\n' "$WEB_PORT"
  printf '    API     http://localhost:%s/api/public/home\n' "$API_PORT"
  printf '    panel   http://localhost:%s/admin/login  (admin@xtrim.ec / admin123)\n' "$WEB_PORT"
  echo
  warn "si el navegador muestra la página en blanco, recarga fuerte (Ctrl+Shift+R):"
  warn "el servidor de desarrollo cambia los nombres de los módulos cuando"
  warn "reoptimiza las dependencias, y una pestaña vieja pide los anteriores."
}

status() {
  printf '  la pila   '
  (cd "$ROOT" && docker compose ps --format '{{.Service}}: {{.Status}}' | tr '\n' ' ') ; echo
  printf '  la API    '
  curl -fsS -o /dev/null --max-time 3 "http://localhost:${API_PORT}/api/health" 2>/dev/null \
    && echo "en pie (${API_PORT})" || echo "caída"
  printf '  el sitio  '
  curl -fsS -o /dev/null --max-time 3 "http://localhost:${WEB_PORT}/" 2>/dev/null \
    && echo "en pie (${WEB_PORT})" || echo "caído"
}

case "${1:-start}" in
  start)   start ;;
  stop)    stop ;;
  restart) stop; start ;;
  status)  status ;;
  *)       die "uso: scripts/dev.sh {start|stop|restart|status}" ;;
esac
