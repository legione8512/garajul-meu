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
    backToStart: 'Back to the home page',
    offline: 'You have no internet connection. The application needs one to save any change.',
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
    currentPassword: 'Current password',
    newEmail: 'New email address',
    fullName: 'Full name',
    code: 'Verification code',
    registrationNumber: 'Registration number',
    make: 'Make',
    commercialDescription: 'Model',
    vin: 'VIN (chassis number)',
    displayName: 'Nickname (optional)',
    timezone: 'Time zone',
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
    configure: 'Documents and expiry dates',
  },
  garage: {
    empty: 'Your garage is empty. Vehicles you add will appear here.',
    add: 'Add a vehicle',
  },
  addVehicle: {
    instructions: 'These are the details printed on the registration certificate.',
    scan: {
      review: 'Check these fields, the photograph was not clear enough: {{fields}}.',
    },
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
  vehicleImage: {
    title: 'Vehicle photograph',
    none: 'This vehicle has no photograph.',
    alt: 'Photograph of the vehicle',
    choose: 'Add a photograph',
    replace: 'Replace the photograph',
    accepted: 'JPEG or PNG.',
    uploading: 'Uploading the photograph…',
    delete: 'Delete the photograph',
    confirmDelete: 'Delete the photograph? The vehicle stays, only the picture goes.',
    confirmDeleteYes: 'Yes, delete it',
    cancel: 'Cancel',
  },
  certificate: {
    open: 'View the registration certificate',
    backToVehicle: 'Back to the vehicle',
    save: 'Save the certificate',
    saved: 'The certificate has been saved.',
    scan: {
      choose: 'Scan the certificate from a photograph',
      pending: 'Reading the photograph…',
      result: 'Fields filled: {{detected}}. To check: {{needsReview}}. Not found: {{notDetected}}.',
      note: 'These values are proposals. Check and correct them before saving.',
      status: {
        DETECTED: 'filled from the photograph',
        NEEDS_REVIEW: 'read with low certainty, please check',
        NOT_DETECTED: 'not found in the photograph',
      },
    },
    problems: 'Please check the following fields:',
    zoomIn: 'Zoom in',
    zoomOut: 'Zoom out',
    zoomReset: 'Original size',
    zoomLevel: 'Zoom: {{percent}}%',
    sensitiveNote: 'Owner and legal-user details are optional. They are never used for reminders.',
    groups: {
      identity: 'Identification',
      dates: 'Dates and validity',
      technical: 'Technical data',
      administrative: 'Administrative',
      owner: 'Owner (C.2)',
      legalUser: 'Legal user (C.3)',
    },
    fields: {
      registrationNumber: 'Registration number',
      vehicleCategory: 'Vehicle category',
      make: 'Make',
      typeVariantVersion: 'Type, variant, version',
      commercialDescription: 'Commercial description',
      vin: 'VIN (chassis number)',
      typeApprovalNumber: 'Type approval number',
      firstRegistrationDate: 'Date of first registration',
      validityPeriod: 'Validity period',
      registrationDate: 'Registration date',
      certificateIssueDate: 'Certificate issue date',
      maximumPermissibleMassKg: 'Maximum permissible mass (kg)',
      vehicleMassKg: 'Mass in service (kg)',
      engineCapacityCc: 'Engine capacity (cc)',
      maximumPowerKw: 'Maximum power (kW)',
      fuelType: 'Fuel type',
      powerWeightRatio: 'Power-to-weight ratio',
      colour: 'Colour',
      seats: 'Seats',
      standingPlaces: 'Standing places',
      civNumber: 'CIV series and number',
      issuingAuthority: 'Issuing authority',
      observations: 'Observations',
      certificateNumber: 'Certificate number',
      ownerNameOrCompany: 'Owner name or company',
      ownerFirstName: 'Owner first name',
      ownerAddress: 'Owner address',
      c2EqualsC1: 'The owner is the holder (C2=C1)',
      userNameOrCompany: 'Legal user name or company',
      userFirstName: 'Legal user first name',
      userAddress: 'Legal user address',
      c3EqualsC1: 'The legal user is the holder (C3=C1)',
    },
  },
  documents: {
    title: 'Documents and expiry dates',
    open: 'Documents and expiry dates',
    openOne: 'Open the document',
    detailsTitle: 'Document',
    backToList: 'Back to documents',
    correct: 'Correct this document',
    saveCorrection: 'Save the correction',
    renew: 'Renew',
    renewNote: 'Renewing creates a new record. This one stays in the history, untouched.',
    saveRenewal: 'Save the renewal',
    backToVehicle: 'Back to the vehicle',
    none: 'No documents recorded for this vehicle.',
    add: 'Add a document',
    save: 'Save the document',
    delete: 'Delete',
    confirmDelete: 'Delete this document? It disappears from the history too.',
    confirmDeleteYes: 'Yes, delete',
    cancel: 'Cancel',
    period: 'Valid until {{until}}.',
    periodFrom: 'Valid from {{from}} until {{until}}.',
    type: {
      RCA: 'RCA',
      CASCO: 'CASCO',
      ITP: 'ITP',
      ROVINIETA: 'Road tax',
    },
    fields: {
      type: 'Document type',
      validFrom: 'Valid from',
      validUntil: 'Valid until',
      provider: 'Insurer or station',
      referenceNumber: 'Policy or reference number',
      notes: 'Notes',
    },
    state: {
      active: 'Valid for another {{days}} days.',
      soon: 'Expires in {{days}} days.',
      urgent: 'Expires in {{days}} days — urgent.',
      expiresToday: 'Expires today.',
      lapsed: 'Expired {{days}} days ago.',
      lapsedUntil: 'Expired {{days}} days ago. Cover resumes on {{date}}.',
      startsOn: 'You are not covered today. Cover starts on {{date}}.',
      notCovered: 'You are not covered today.',
      notConfigured: 'Not configured.',
    },
  },
    history: {
    title: 'History',
    open: 'Document history',
    filter: 'Show only',
    allTypes: 'All types',
    none: 'Every document you have kept will appear here, including the ones a '
      + 'renewal replaced. There are none yet.',
    page: 'Page {{page}} of {{pages}}.',
    total: '{{total}} records in total.',
    previous: 'Previous page',
    next: 'Next page',
  },
  reminders: {
    title: 'Notifications',
    nativeOnly: 'Push notifications arrive in the phone application, which does not exist yet. '
      + 'Until then, document status is always visible on the dashboard.',
    none: 'No reminders scheduled for this document.',
    lead: {
      onTheDay: 'On the day it expires:',
      oneDay: 'One day before:',
      // Identical on purpose. The split exists for Romanian, which needs "de"
      // above nineteen; English gets the same sentence twice rather than a
      // second mechanism to keep the two locales the same shape.
      fewDays: '{{days}} days before:',
      manyDays: '{{days}} days before:',
    },
    outcome: {
      scheduled: 'scheduled for {{when}}.',
      sending: 'sending now.',
      sent: 'marked as sent on {{when}}.',
      failed: 'could not be sent.',
      cancelled: 'cancelled.',
    },
  },
  profile: {
    signOut: 'Sign out',
    account: 'Account details',
    save: 'Save changes',
    saved: 'Your changes have been saved.',
    emailVerified: '(confirmed)',
    emailNotVerified: '(not confirmed)',
    security: 'Security',
    changePassword: 'Change password',
    changeEmail: 'Change email address',
    deleteAccount: 'Delete account',
    back: 'Back to profile',
  },
  changePassword: {
    warning: 'Changing your password signs you out everywhere, on every device, '
      + 'including this one. You will need to sign in again.',
    submit: 'Change password',
  },
  changeEmail: {
    instructions: 'The confirmation code goes to your current address, not the new one. '
      + 'That is how we know the request is yours.',
    request: 'Send the code',
    codeSent: 'We have sent a code to your current address. Enter it below.',
    confirm: 'Confirm the new address',
    done: 'Your address has been changed. It is unconfirmed for now: '
      + 'look for the verification email sent to the new address.',
  },
  deleteAccount: {
    warning: 'Deleting your account is permanent. Your vehicles, certificates, '
      + 'documents, photographs and history all go with it. There is no way back.',
    confirm: 'I understand, delete my account',
    submit: 'Delete my account permanently',
    cancel: 'Cancel',
  },
  notificationPreferences: {
    title: 'Notification preferences',
    enabled: 'Send me notifications',
    leads: 'How far ahead to tell you',
    remind30Days: '30 days before',
    remind14Days: '14 days before',
    remind7Days: '7 days before',
    remind3Days: '3 days before',
    remind1Day: 'One day before',
    remindOnExpiry: 'On the day it expires',
    time: 'The time you receive them',
    save: 'Save preferences',
    saved: 'Your preferences have been saved.',
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
    headline: 'Always know what expires, and when',
    lead: 'Garajul Meu keeps track of the paperwork for every car you own, and shows '
      + 'you at a glance what is coming.',
    features: {
      scanTitle: 'Photograph the certificate',
      scanBody: 'The registration certificate is read for you and the fields fill '
        + 'themselves in. You check and correct before saving - nothing is stored '
        + 'without your say-so.',
      documentsTitle: 'Four documents, one glance',
      documentsBody: 'Liability insurance, roadworthiness, comprehensive cover and '
        + 'road tax, each with its own date. The dashboard shows what is valid, what '
        + 'is running out, and what has never been set up.',
      remindersTitle: 'Early, not on the last day',
      remindersBody: 'Reminders are prepared for every document at thirty, fourteen '
        + 'and seven days before it expires. You choose which of them you want.',
    },
    signIn: 'Sign in',
    createAccount: 'Create an account',
  },

  features: {
    lead: 'Everything the application does, explained. Nothing here is planned or '
      + 'promised - all of it works today.',
    readMore: 'See everything the application does',
    garage: {
      title: 'The garage',
      body: 'Add as many vehicles as you own. Each keeps its own identity - plate, '
        + 'make, model, chassis number - and an optional nickname, so two of the same '
        + 'kind stay apart. You can add a photograph too.',
    },
    certificate: {
      title: 'The registration certificate, read from a photograph',
      body: 'Photograph the certificate and the application reads what it can, '
        + 'filling the fields for you. Nothing is saved on its own: you see what was '
        + 'read confidently, what should be checked and what could not be found, you '
        + 'correct it, and only then do you save. The photograph is not kept.',
    },
    documents: {
      title: 'Documents and expiry dates',
      body: 'Liability insurance, roadworthiness, comprehensive cover and road tax, '
        + 'each with its own period of validity. Renewing does not lose the old '
        + 'record - a new one is added, and the application knows which of them '
        + 'covers today.',
    },
    dashboard: {
      title: 'What is coming, at a glance',
      body: 'For every car and every kind of document you see the state it is in: '
        + 'valid, running out, expired, or never set up. The state is carried by the '
        + 'shape of a line as well as by colour, so it survives a monochrome screen '
        + 'and every kind of colour blindness.',
    },
    history: {
      title: 'History',
      body: 'Every document you have kept, not only the current one. You can see '
        + 'which policies you held and over which periods, filtered by kind.',
    },
    reminders: {
      title: 'Reminders',
      body: 'Reminders are prepared for every document at thirty, fourteen and seven '
        + 'days before it expires, and you choose which of them you want and at what '
        + 'time of day. For now you see them in the application; notifications on '
        + 'your phone arrive with the Android app.',
    },
    account: {
      title: 'Your account and your data',
      body: 'The application speaks Romanian and English, and the language follows '
        + 'you from one device to another. You can change your email address and your '
        + 'password, and you can delete your account entirely - with everything '
        + 'belonging to it, photographs included.',
    },
  },

  legal: {
    terms: 'Terms and conditions',
    privacy: 'Privacy policy',
    placeholder: 'PLACEHOLDER TEXT. This document does not yet carry its final legal '
      + 'wording and has no effect. Section 24 makes it required before public release.',
    termsScope: 'This will carry the conditions of use: what the application offers, '
      + 'what it does not guarantee, and what happens to your account.',
    privacyScope: 'This will carry what data we keep, for how long, and why. What the '
      + 'application does today: certificate owner details are optional and are never '
      + 'used for reminders, deleting your account is permanent, and photographs go '
      + 'with the vehicle they belong to.',
  },
  screens: {
    welcome: 'Welcome',
    register: 'Create account',
    verifyEmail: 'Email verification',
    login: 'Sign in',
    forgotPassword: 'Forgot your password',
    resetPassword: 'New password',
    notFound: 'Page not found',
    features: 'What the application does',
    dashboard: 'Home',
    garage: 'My garage',
    profile: 'Profile',
    addVehicle: 'New vehicle',
    vehicleDetails: 'Vehicle details',
    certificate: 'Registration certificate',
    changePassword: 'Change your password',
    changeEmail: 'Change your email address',
    deleteAccount: 'Delete your account',
    terms: 'Terms and conditions',
    privacy: 'Privacy policy',
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
    OCR_FILE_INVALID: 'That file is not a photograph we can read. Try a clear JPEG or PNG.',
    OCR_RATE_LIMITED: 'You have used all your scans for now. Please try again later.',
    IMAGE_TOO_LARGE: 'That photograph is too large. Try a smaller one.',
    IMAGE_INVALID_TYPE: 'That file is not a photograph we can keep. Try a JPEG or a PNG.',
    STORAGE_PROVIDER_UNAVAILABLE: 'We cannot store photographs right now. Please try again in a few minutes.',
    OCR_PROVIDER_UNAVAILABLE: 'The photograph reading service is not responding right now. Please try again in a few minutes.',
    OCR_PROCESSING_FAILED: 'That photograph could not be processed. You can fill the details in by hand.',
    VALIDATION_ERROR: 'Please check the details you entered.',
    MALFORMED_REQUEST: 'The request could not be read. Please try again.',
    RESOURCE_NOT_FOUND: 'Not found.',
    METHOD_NOT_ALLOWED: 'That operation is not allowed here.',
    RATE_LIMITED: 'Too many attempts. Please wait a moment and try again.',
    INTERNAL_ERROR: 'Something went wrong. Please try again later.',
    UNKNOWN: 'Something went wrong. Please try again later.',
    DOCUMENT_NOT_FOUND: 'The document was not found.',
    DOCUMENT_INVALID_DATE_RANGE: 'The start date is after the end date.',
    DOCUMENT_TYPE_INVALID: 'That document type is not recognised.',
  },
}