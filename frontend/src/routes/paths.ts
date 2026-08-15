/**
 * Every URL the application knows, named once.
 *
 * A route path and the links pointing at it live in different files. When they
 * are two separate string literals, a typo in one produces a link that silently
 * lands on the not-found page instead of failing loudly.
 */
export const paths = {
  welcome: '/',
  register: '/register',
  verifyEmail: '/verify-email',
  login: '/login',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  dashboard: '/dashboard',
  garage: '/garage',
  profile: '/profile',
} as const