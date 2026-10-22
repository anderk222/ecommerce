#!/usr/bin/env bash
# ============================================================================
# Xtrim — the whole product with Docker Compose
#
#   scripts/stack.sh up       build, start and wait until everything is healthy
#   scripts/stack.sh smoke    prove the site, the proxy, the login and the SEO
#   scripts/stack.sh status   what is running and where
#   scripts/stack.sh logs     follow the logs
#   scripts/stack.sh down     stop the stack (the database volume survives)
#   scripts/stack.sh reset    stop and DELETE the database volume (asks first)
#
# `up` needs Docker and Compose only. It runs the same Dockerfiles Render
# deploys; no JDK, Maven or Node is needed on the host.
#
# Host ports come from .env (SERVER_PORT, WEB_PORT). Inside the containers
# the API always listens on 8080 and the site on 4000.
# ============================================================================

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

log()  { printf '\033[1;36m»\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m✓\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m✗\033[0m %s\n' "$*" >&2; exit 1; }

require_env() {
  [[ -f "$ROOT/.env" ]] || die "falta .env — haz: cp .env.example .env"
}

require_docker() {
  command -v docker >/dev/null 2>&1 || die "hace falta Docker"
  docker info >/dev/null 2>&1 || die "el demonio de Docker no responde"

  local version
  version="$(docker compose version --short 2>/dev/null | sed 's/^v//')"
  [[ -n "$version" ]] || die "hace falta Docker Compose v2"
  # --wait-timeout needs Compose 2.20 or newer.
  printf '%s\n%s\n' "2.20" "$version" | sort -V -C \
    || die "Docker Compose ${version} es demasiado antiguo (hace falta 2.20+)"
}

# Reads a value from the environment (higher precedence) or from .env.
# The shell override is what lets a second stack run beside a busy one:
#   SERVER_PORT=18081 WEB_PORT=14200 scripts/stack.sh up
env_value() {
  local name="$1" fallback="$2" value="${!1:-}"
  if [[ -z "$value" && -f "$ROOT/.env" ]]; then
    value="$(sed -nE "s/^[[:space:]]*${name}[[:space:]]*=[[:space:]]*//p" "$ROOT/.env" | tail -1)"
    value="${value%\"}"; value="${value#\"}"
    value="${value%\'}"; value="${value#\'}"
  fi
  printf '%s' "${value:-$fallback}"
}

API_PORT="$(env_value SERVER_PORT 8080)"
WEB_PORT_VALUE="$(env_value WEB_PORT 4200)"

compose() {
  docker compose --profile app "$@"
}

up() {
  require_env
  require_docker
  log "construyendo y levantando la pila completa"
  if ! compose up -d --build --wait --wait-timeout 300; then
    warn "si el error dice 'address already in use', el puerto está ocupado:"
    warn "cambia SERVER_PORT/WEB_PORT en .env, o pásalos en el comando."
    exit 1
  fi
  echo
  ok "la pila está sana"
  printf '    sitio   http://localhost:%s\n' "$WEB_PORT_VALUE"
  printf '    panel   http://localhost:%s/admin/login  (admin@xtrim.ec / admin123)\n' "$WEB_PORT_VALUE"
  printf '    API     http://localhost:%s/api/health\n' "$API_PORT"
}

down() {
  require_env
  require_docker
  log "parando la pila (el volumen de datos se conserva)"
  compose down
  ok "parada — los datos siguen ahí"
}

reset() {
  require_env
  require_docker
  if [[ "${1:-}" != "-y" && "${1:-}" != "--yes" ]]; then
    [[ -t 0 ]] || die "reset necesita una terminal o el flag -y"
    warn "esto BORRA la base de datos local (docker compose down -v)"
    read -r -p "escribe 'yes' para continuar: " answer
    [[ "$answer" == "yes" ]] || die "cancelado"
  fi
  compose down -v
  ok "borrada — el próximo 'up' vuelve a sembrar la base con Flyway"
}

status() {
  require_env
  require_docker
  compose ps
  echo
  printf '  API     '
  curl -fsS -o /dev/null --max-time 3 "http://localhost:${API_PORT}/api/health" \
    && echo "en pie (${API_PORT})" || echo "caída"
  printf '  sitio   '
  curl -fsS -o /dev/null --max-time 3 "http://localhost:${WEB_PORT_VALUE}/" \
    && echo "en pie (${WEB_PORT_VALUE})" || echo "caído"
}

logs() {
  require_env
  require_docker
  compose logs -f --tail=100
}

# --- the smoke test ---------------------------------------------------------
# A deployment that cannot log in is not a deployment. Each check prints its
# own line; the first failure stops the script with a non-zero status.
smoke() {
  require_env
  require_docker

  local site="http://localhost:${WEB_PORT_VALUE}"
  local api="http://localhost:${API_PORT}"

  local home
  home="$(curl -fsS --max-time 15 "$site/")" \
    || die "S1 el sitio no responde"
  grep -q "Router WiFi 6 AX3000" <<<"$home" \
    || die "S1 el HTML no trae el catálogo sembrado (¿SSR vacío?)"
  ok "S1 home renderizada por el servidor"

  curl -fsS --max-time 10 -o /dev/null "$site/api/health" \
    || die "S2 el proxy /api del sitio no llega a la API"
  ok "S2 proxy /api → API"

  local login token
  login="$(curl -fsS --max-time 10 \
    -H 'Content-Type: application/json' \
    -d '{"email":"admin@xtrim.ec","password":"admin123"}' \
    "$site/api/auth/login")" || die "S3 el login falló"
  token="$(sed -nE 's/.*"token"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p' <<<"$login")"
  [[ -n "$token" ]] || die "S3 la respuesta no trae token"
  ok "S3 login del administrador"

  curl -fsS --max-time 10 -o /dev/null \
    -H "Authorization: Bearer $token" \
    "$site/api/admin/catalog?page=0&size=1" \
    || die "S4 la lectura autenticada del catálogo falló"
  ok "S4 lectura autenticada del catálogo"

  curl -fsS --max-time 10 "$site/robots.txt" | grep -q "Sitemap:" \
    || die "S5 robots.txt no declara el sitemap"
  curl -fsS --max-time 10 "$site/robots.txt" | grep -q "Disallow: /admin" \
    || die "S5 robots.txt no bloquea el panel"
  ok "S5 robots.txt"

  curl -fsS --max-time 15 "$site/sitemap.xml" | grep -q "<urlset" \
    || die "S6 sitemap.xml inválido"
  curl -fsS --max-time 15 "$site/sitemap.xml" | grep -q "router-wifi-6-ax3000" \
    || die "S6 el sitemap no lista el catálogo sembrado"
  ok "S6 sitemap.xml"

  curl -fsS --max-time 10 -o /dev/null "$api/api/health" \
    || die "S7 la API no responde directamente"
  ok "S7 API directa"

  echo
  ok "smoke completo — el despliegue responde"
}

usage() {
  cat <<'EOF'
uso: scripts/stack.sh {up|down|reset [-y]|status|logs|smoke}

  up        construye las imágenes y espera a que todo esté sano
  down      para la pila; el volumen de PostgreSQL se conserva
  reset     para la pila y BORRA el volumen (pide confirmación)
  status    muestra los servicios y la salud de la API y del sitio
  logs      sigue los logs de todos los servicios
  smoke     comprueba el sitio, el proxy, el login y el SEO
EOF
}

case "${1:-up}" in
  up)     up ;;
  down)   down ;;
  reset)  shift; reset "${1:-}" ;;
  status) status ;;
  logs)   logs ;;
  smoke)  smoke ;;
  help|-h|--help) usage ;;
  *)      usage; exit 1 ;;
esac
