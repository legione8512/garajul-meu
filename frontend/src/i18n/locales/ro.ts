/**
 * The reference locale. Every other locale is typed against this object, so a
 * key added here and forgotten elsewhere is a compile error rather than a
 * string that silently renders in the wrong language.
 *
 * Deliberately not `as const`: literal value types would force every other
 * locale to repeat the Romanian wording.
 */
export const ro = {
  app: {
    name: 'Garajul Meu',
  },
  common: {
    reference: 'Referință: {{requestId}}',
  },
  language: {
    label: 'Limbă',
  },
    fields: {
    email: 'Adresă de email',
    password: 'Parolă',
  },
  login: {
    submit: 'Autentifică-te',
    forgotPassword: 'Ai uitat parola?',
    noAccount: 'Nu ai cont? Creează unul.',
  },
  welcome: {
    signIn: 'Autentifică-te',
    createAccount: 'Creează un cont',
    signedInAs: 'Ești autentificat ca {{name}}.',
    signOut: 'Ieși din cont',
  },
  screens: {
    welcome: 'Bun venit',
    register: 'Cont nou',
    verifyEmail: 'Verificarea adresei de email',
    login: 'Autentificare',
    forgotPassword: 'Ai uitat parola',
    resetPassword: 'Parolă nouă',
    notFound: 'Pagina nu există',
  },
  /**
   * Phrased as labels rather than sentences on purpose. Romanian requires "de"
   * before a noun after numbers of twenty and above - "12 caractere" but "120
   * de caractere" - and interpolation cannot choose between them. i18next can,
   * through plural forms, and that is worth doing when real copy is written;
   * until then this phrasing is correct for every number.
   */
  validation: {
    required: 'Completează acest câmp.',
    email: 'Adresa de email nu pare validă.',
    minLength: 'Lungime minimă: {{min}} caractere.',
    maxLength: 'Lungime maximă: {{max}} caractere.',
    sixDigits: 'Codul are exact șase cifre.',
    invalid: 'Valoarea nu este acceptată.',
  },
  /**
   * Keyed by the backend's error codes, letter for letter. That is the whole
   * point of section 17: the backend sends a stable code and no prose, and this
   * is the one place the code becomes something a person can read.
   *
   * Only the codes the authentication and account surface can produce are here.
   * The vehicle, certificate, document, OCR and storage codes arrive with the
   * phases that can actually raise them.
   */
  errors: {
    AUTHENTICATION_REQUIRED: 'Sesiunea a expirat. Autentifică-te din nou.',
    INVALID_CREDENTIALS: 'Adresa de email sau parola nu sunt corecte.',
    EMAIL_NOT_VERIFIED: 'Adresa de email nu este confirmată. Verifică-ți inbox-ul.',
    REFRESH_TOKEN_INVALID: 'Sesiunea nu mai este validă. Autentifică-te din nou.',
    REFRESH_TOKEN_REUSED: 'Sesiunea a fost închisă din motive de siguranță. Autentifică-te din nou.',
    VERIFICATION_CODE_INVALID: 'Codul nu este corect.',
    VERIFICATION_CODE_EXPIRED: 'Codul a expirat. Cere unul nou.',
    EMAIL_ALREADY_EXISTS: 'Există deja un cont cu această adresă de email.',
    USER_NOT_FOUND: 'Contul nu a fost găsit.',
    INVALID_CURRENT_PASSWORD: 'Parola actuală nu este corectă.',
    VALIDATION_ERROR: 'Verifică datele introduse.',
    MALFORMED_REQUEST: 'Cererea nu a putut fi citită. Încearcă din nou.',
    RESOURCE_NOT_FOUND: 'Resursa nu a fost găsită.',
    METHOD_NOT_ALLOWED: 'Operațiunea nu este permisă aici.',
    RATE_LIMITED: 'Prea multe încercări. Așteaptă puțin și încearcă din nou.',
    INTERNAL_ERROR: 'A apărut o problemă. Încearcă din nou mai târziu.',
    /** Not a backend code. What a code this frontend has never heard of becomes. */
    UNKNOWN: 'A apărut o problemă. Încearcă din nou mai târziu.',
  },
}