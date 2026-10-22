# syntax=docker/dockerfile:1.7
# ============================================================================
# Xtrim — the site
#
# The Angular application is built with Node and served by its own Node
# server: the SSR needs a running process, not static files.
#
# The runtime must carry the production dependencies (`express`,
# `compression`, `@angular/ssr`): the server imports them, and `dist/` alone
# cannot resolve them. The build installs once, builds, and prunes the tree to
# production before the runtime stage copies it.
# ============================================================================

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .
RUN npm run build
RUN --mount=type=cache,target=/root/.npm \
    npm prune --omit=dev

FROM node:22-alpine
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

# The user exists before the copies, so every file arrives owned by it:
# `COPY --chown` writes the ownership in the layer, while `chown -R` after the
# copy walks the whole `node_modules` tree on every build (it took 92 s here).
RUN addgroup -S xtrim && adduser -S xtrim -G xtrim

COPY --from=build --chown=xtrim:xtrim /app/node_modules ./node_modules
COPY --from=build --chown=xtrim:xtrim /app/dist ./dist
COPY --from=build --chown=xtrim:xtrim /app/package.json ./package.json

USER xtrim

EXPOSE 4000

CMD ["node", "dist/frontend/server/server.mjs"]
