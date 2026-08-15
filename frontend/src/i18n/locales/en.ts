import type { ro } from './ro.ts'

/**
 * Typed as the Romanian object, so this file cannot drift: a missing key and an
 * invented one are both compile errors.
 */
export const en: typeof ro = {
  app: {
    name: 'Garajul Meu',
  },
  common: {
    reference: 'Reference: {{requestId}}',
    loading: 'Loading…',
    retry: 'Try again',
  },
  language: {
    label: 'Language',
  },
  navigation: {
    label: 'Primary navigation',
    home: 'Home',
    garage: 'Garage',
    profile: 'Profile',
  },
  fields: {
    email: 'Email address',
    password: 'Password',
    newPassword: 'New password',
    fullName: 'Full name',
    code: 'Verification code',
    registrationNumber: 'Registration number',
    make: 'Make',
    commercialDescription: 'Model',
    vin: 'VIN (chassis number)',
    displayName: 'Nickname (optional)',
  },
  register: {
    submit: 'Create account',
    haveAccount: 'Already have an account? Sign in.',
  },
  verifyEmail: {
    instructions: 'We have sent you a six-digit code. Enter it below.',
    submit: 'Confirm address',
    resend: 'Send another code',
    resent: 'A new code is on its way.',
  },
  dashboard: {
    empty: 'Documents about to expire will appear here. You have no vehicles yet.',
  },
  garage: {
    empty: 'Your garage is empty. Vehicles you add will appear here.',
    add: 'Add a vehicle',
  },
  addVehicle: {
    instructions: 'These are the details printed on the registration certificate.',
    submit: 'Add the vehicle',
  },
  vehicle: {
    backToGarage: 'Back to the garage',
    rename: 'Save nickname',
    delete: 'Delete this vehicle',
    confirmDelete: 'Delete this vehicle? Its data is lost for good.',
    confirmDeleteYes: 'Yes, delete it',
    cancel: 'Cancel',
  },
  profile: {
    signOut: 'Sign out',
  },
  forgotPassword: {
    instructions: 'Enter the account address. If an account exists for it, we will send a code.',
    submit: 'Send the code',
    remembered: 'Remembered your password? Sign in.',
  },
  resetPassword: {
    instructions: 'Enter the code from the email and your new password.',
    submit: 'Change password',
  },
  login: {
    submit: 'Sign in',
    forgotPassword: 'Forgotten your password?',
    noAccount: 'No account yet? Create one.',
  },
  welcome: {
    signIn: 'Sign in',
    createAccount: 'Create an account',
  },
  screens: {
    welcome: 'Welcome',
    register: 'Create account',
    verifyEmail: 'Email verification',
    login: 'Sign in',
    forgotPassword: 'Forgot your password',
    resetPassword: 'New password',
    notFound: 'Page not found',
    dashboard: 'Home',
    garage: 'My garage',
    profile: 'Profile',
    addVehicle: 'New vehicle',
    vehicleDetails: 'Vehicle details',
  },
  validation: {
    required: 'Please fill in this field.',
    email: 'That does not look like an email address.',
    minLength: 'Minimum length: {{min}} characters.',
    maxLength: 'Maximum length: {{max}} characters.',
    sixDigits: 'The code is exactly six digits.',
    invalid: 'That value is not accepted.',
  },
  errors: {
    AUTHENTICATION_REQUIRED: 'Your session has expired. Please sign in again.',
    INVALID_CREDENTIALS: 'That email address or password is not correct.',
    EMAIL_NOT_VERIFIED: 'This email address is not confirmed yet. Check your inbox.',
    REFRESH_TOKEN_INVALID: 'This session is no longer valid. Please sign in again.',
    REFRESH_TOKEN_REUSED: 'The session was closed for security reasons. Please sign in again.',
    VERIFICATION_CODE_INVALID: 'That code is not correct.',
    VERIFICATION_CODE_EXPIRED: 'That code has expired. Request a new one.',
    EMAIL_ALREADY_EXISTS: 'An account with this email address already exists.',
    USER_NOT_FOUND: 'Account not found.',
    INVALID_CURRENT_PASSWORD: 'Your current password is not correct.',
    VEHICLE_NOT_FOUND: 'Vehicle not found.',
    VEHICLE_DUPLICATE_VIN: 'You already have a vehicle with this VIN.',
    VALIDATION_ERROR: 'Please check the details you entered.',
    MALFORMED_REQUEST: 'The request could not be read. Please try again.',
    RESOURCE_NOT_FOUND: 'Not found.',
    METHOD_NOT_ALLOWED: 'That operation is not allowed here.',
    RATE_LIMITED: 'Too many attempts. Please wait a moment and try again.',
    INTERNAL_ERROR: 'Something went wrong. Please try again later.',
    UNKNOWN: 'Something went wrong. Please try again later.',
  },
}