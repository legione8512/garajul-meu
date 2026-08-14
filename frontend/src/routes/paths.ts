/**
 * Every URL the application knows, named once.
 *
 * A route path and the links pointing at it live in different files. When they
 * are two separate string literals, a typo in one produces a link that silently
 * lands on the not-found page instead of failing loudly. Naming them here makes
 * that particular mistake impossible to write.
 */
export const paths = {
  welcome: '/',
  register: '/register',
  verifyEmail: '/verify-email',
  login: '/login',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
} as const