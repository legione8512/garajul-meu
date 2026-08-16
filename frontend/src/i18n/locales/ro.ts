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
    loading: 'Se încarcă…',
    retry: 'Încearcă din nou',
  },
  language: {
    label: 'Limbă',
  },
  navigation: {
    label: 'Navigare principală',
    home: 'Acasă',
    garage: 'Garaj',
    profile: 'Profil',
  },
  fields: {
    email: 'Adresă de email',
    password: 'Parolă',
    newPassword: 'Parolă nouă',
    fullName: 'Nume complet',
    code: 'Cod de verificare',
    registrationNumber: 'Număr de înmatriculare',
    make: 'Marcă',
    commercialDescription: 'Model',
    vin: 'VIN (serie de șasiu)',
    displayName: 'Poreclă (opțional)',
  },
  register: {
    submit: 'Creează contul',
    haveAccount: 'Ai deja cont? Autentifică-te.',
  },
  verifyEmail: {
    instructions: 'Ți-am trimis un cod din șase cifre. Introdu-l mai jos.',
    submit: 'Confirmă adresa',
    resend: 'Trimite alt cod',
    resent: 'Am trimis un cod nou.',
  },
  dashboard: {
    empty: 'Aici vei vedea documentele care urmează să expire. Deocamdată nu ai niciun vehicul.',
  },
  garage: {
    empty: 'Garajul tău e gol. Vehiculele pe care le adaugi vor apărea aici.',
    add: 'Adaugă un vehicul',
  },
  addVehicle: {
    instructions: 'Datele sunt cele de pe certificatul de înmatriculare.',
    submit: 'Adaugă vehiculul',
  },
  vehicle: {
    backToGarage: 'Înapoi la garaj',
    rename: 'Salvează porecla',
    delete: 'Șterge vehiculul',
    confirmDelete: 'Ștergi acest vehicul? Datele lui se pierd definitiv.',
    confirmDeleteYes: 'Da, șterge',
    cancel: 'Renunță',
  },
  certificate: {
    open: 'Vezi certificatul de înmatriculare',
    backToVehicle: 'Înapoi la vehicul',
    save: 'Salvează certificatul',
    saved: 'Certificatul a fost salvat.',
    problems: 'Verifică următoarele câmpuri:',
    sensitiveNote: 'Datele proprietarului și ale utilizatorului legal sunt opționale. Nu sunt folosite niciodată pentru memento-uri.',
    groups: {
      identity: 'Identificare',
      dates: 'Date și valabilitate',
      technical: 'Date tehnice',
      administrative: 'Administrativ',
      owner: 'Proprietar (C.2)',
      legalUser: 'Utilizator legal (C.3)',
    },
    fields: {
      registrationNumber: 'Număr de înmatriculare',
      vehicleCategory: 'Categoria vehiculului',
      make: 'Marcă',
      typeVariantVersion: 'Tip, variantă, versiune',
      commercialDescription: 'Descriere comercială',
      vin: 'VIN (serie de șasiu)',
      typeApprovalNumber: 'Număr de omologare',
      firstRegistrationDate: 'Data primei înmatriculări',
      validityPeriod: 'Perioada de valabilitate',
      registrationDate: 'Data înmatriculării',
      certificateIssueDate: 'Data eliberării certificatului',
      maximumPermissibleMassKg: 'Masa maximă autorizată (kg)',
      vehicleMassKg: 'Masa proprie (kg)',
      engineCapacityCc: 'Capacitate cilindrică (cm³)',
      maximumPowerKw: 'Putere maximă (kW)',
      fuelType: 'Combustibil',
      powerWeightRatio: 'Raport putere/masă',
      colour: 'Culoare',
      seats: 'Locuri pe scaune',
      standingPlaces: 'Locuri în picioare',
      civNumber: 'Serie și număr CIV',
      issuingAuthority: 'Autoritatea emitentă',
      observations: 'Observații',
      certificateNumber: 'Numărul certificatului',
      ownerNameOrCompany: 'Nume sau denumire proprietar',
      ownerFirstName: 'Prenume proprietar',
      ownerAddress: 'Adresă proprietar',
      c2EqualsC1: 'Proprietarul este deținătorul (C2=C1)',
      userNameOrCompany: 'Nume sau denumire utilizator',
      userFirstName: 'Prenume utilizator',
      userAddress: 'Adresă utilizator',
      c3EqualsC1: 'Utilizatorul este deținătorul (C3=C1)',
    },
  },
  profile: {
    signOut: 'Ieși din cont',
  },
  forgotPassword: {
    instructions: 'Scrie adresa contului. Dacă există un cont cu ea, îți trimitem un cod.',
    submit: 'Trimite codul',
    remembered: 'Ți-ai amintit parola? Autentifică-te.',
  },
  resetPassword: {
    instructions: 'Introdu codul primit prin email și parola nouă.',
    submit: 'Schimbă parola',
  },
  login: {
    submit: 'Autentifică-te',
    forgotPassword: 'Ai uitat parola?',
    noAccount: 'Nu ai cont? Creează unul.',
  },
  welcome: {
    signIn: 'Autentifică-te',
    createAccount: 'Creează un cont',
  },
  screens: {
    welcome: 'Bun venit',
    register: 'Cont nou',
    verifyEmail: 'Verificarea adresei de email',
    login: 'Autentificare',
    forgotPassword: 'Ai uitat parola',
    resetPassword: 'Parolă nouă',
    notFound: 'Pagina nu există',
    dashboard: 'Acasă',
    garage: 'Garajul meu',
    profile: 'Profil',
    addVehicle: 'Vehicul nou',
    vehicleDetails: 'Detalii vehicul',
    certificate: 'Certificat de înmatriculare',
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
   * The vehicle codes arrive with Phase 7, which is the first that can raise
   * them. VEHICLE_ACCESS_DENIED is deliberately absent: the backend answers 404
   * for a vehicle somebody else owns, so nothing in V1 can send it, and a
   * translation for an unreachable code would suggest otherwise. The
   * certificate, document, OCR and storage codes arrive the same way.
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
    VEHICLE_NOT_FOUND: 'Vehiculul nu a fost găsit.',
    VEHICLE_DUPLICATE_VIN: 'Ai deja un vehicul cu acest VIN.',
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