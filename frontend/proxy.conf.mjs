/**
 * The proxy of the development server.
 *
 * <p>The browser always calls the API through the relative path `/api`, and
 * the dev server forwards it to the API. The target comes from the
 * environment, so a machine with **two checkouts** —or with the port of the
 * API already taken— can point the proxy somewhere else without touching
 * the code:
 *
 * ```bash
 * API_PROXY_TARGET=http://localhost:8081 npm start
 * ```
 *
 * The default is the port of the contract, 8080.
 */
const target = process.env['API_PROXY_TARGET'] ?? 'http://localhost:8080';

export default {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
  },
};
