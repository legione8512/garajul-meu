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
  documentsPattern: '/garage/:vehicleId/documents',
  documents: (vehicleId: string) => `/garage/${vehicleId}/documents`,
  documentPattern: '/garage/:vehicleId/documents/:documentId',
  document: (vehicleId: string, documentId: string) =>
    `/garage/${vehicleId}/documents/${documentId}`,
  historyPattern: '/garage/:vehicleId/history',
  history: (vehicleId: string) => `/garage/${vehicleId}/history`,
  profile: '/profile',
  /**
   * Screens 16, 17 and 22, nested under the profile they are reached from.
   * All three are literal segments, so none of them competes with anything.
   */
  changePassword: '/profile/password',
  changeEmail: '/profile/email',
  deleteAccount: '/profile/delete',
  /**
   * Screens 19 and 20, public. Somebody deciding whether to create an account
   * has to be able to read both before they have one.
   */
  terms: '/terms',
  privacy: '/privacy',
  /**
   * Not one of section 5's twenty-two screens. Added on 2026-08-29 with the
   * owner's approval, for the same reason the legal pages are public: somebody
   * deciding whether to create an account needs to be able to read what the
   * application does before they have one, and to send that as a link.
   */
  features: '/features',
} as const