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
  /**
   * Nested under the garage rather than living at the root, because that is
   * where it is reached from and section 5 fixes the primary navigation at three
   * destinations. Static, so it keeps winning over the vehicle pattern below -
   * React Router ranks a literal segment above a dynamic one regardless of the
   * order they are declared in.
   */
  addVehicle: '/garage/new',
  /** The route definition takes the pattern; every link takes the builder. */
  vehiclePattern: '/garage/:vehicleId',
  vehicle: (vehicleId: string) => `/garage/${vehicleId}`,
  certificatePattern: '/garage/:vehicleId/certificate',
  certificate: (vehicleId: string) => `/garage/${vehicleId}/certificate`,
  profile: '/profile',
} as const