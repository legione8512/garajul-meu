/**
 * Where the API lives.
 *
 * The default is the local backend, so a fresh clone runs with no .env file at
 * all. Deployment sets VITE_API_BASE_URL, and it must be an origin rather than
 * a path: section 21 puts the API on its own subdomain, a separate origin from
 * the application.
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/**
 * Requests to these paths must never trigger a token refresh. A 401 from
 * /auth/login means the password was wrong, not that a token expired -
 * refreshing there would be nonsense and would spend the 60-per-hour budget for
 * nothing.
 */
export const AUTH_PATH_PREFIX = '/api/v1/auth/'