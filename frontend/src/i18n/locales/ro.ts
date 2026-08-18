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
    currentPassword: 'Parola actuală',
    newEmail: 'Adresa nouă de email',
    fullName: 'Nume complet',
    code: 'Cod de verificare',
    registrationNumber: 'Număr de înmatriculare',
    make: 'Marcă',
    commercialDescription: 'Model',
    vin: 'VIN (serie de șasiu)',
    displayName: 'Poreclă (opțional)',
    timezone: 'Fus orar',
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
    configure: 'Documente și expirări',
  },
  garage: {
    empty: 'Garajul tău e gol. Vehiculele pe care le adaugi vor apărea aici.',
    add: 'Adaugă un vehicul',
  },
  addVehicle: {
    instructions: 'Datele sunt cele de pe certificatul de înmatriculare.',
    scan: {
      review: 'Verifică aceste câmpuri, fotografia nu a fost destul de clară: {{fields}}.',
    },
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
  vehicleImage: {
    title: 'Fotografia vehiculului',
    none: 'Acest vehicul nu are o fotografie.',
    alt: 'Fotografia vehiculului',
    choose: 'Adaugă o fotografie',
    replace: 'Înlocuiește fotografia',
    accepted: 'JPEG sau PNG.',
    uploading: 'Se încarcă fotografia…',
    delete: 'Șterge fotografia',
    confirmDelete: 'Ștergi fotografia? Vehiculul rămâne, doar poza dispare.',
    confirmDeleteYes: 'Da, șterge',
    cancel: 'Renunță',
  },
  certificate: {
    open: 'Vezi certificatul de înmatriculare',
    backToVehicle: 'Înapoi la vehicul',
    save: 'Salvează certificatul',
    saved: 'Certificatul a fost salvat.',
    scan: {
      choose: 'Scanează talonul dintr-o fotografie',
      pending: 'Se citește fotografia…',
      result: 'Câmpuri completate: {{detected}}. De verificat: {{needsReview}}. Negăsite: {{notDetected}}.',
      note: 'Valorile citite sunt propuneri. Verifică-le și corectează-le înainte de a salva.',
      status: {
        DETECTED: 'completat din fotografie',
        NEEDS_REVIEW: 'citit nesigur, verifică',
        NOT_DETECTED: 'nu a fost găsit în fotografie',
      },
    },
    problems: 'Verifică următoarele câmpuri:',
    zoomIn: 'Mărește',
    zoomOut: 'Micșorează',
    zoomReset: 'Mărime inițială',
    zoomLevel: 'Mărire: {{percent}}%',
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
  documents: {
    title: 'Documente și expirări',
    open: 'Documente și expirări',
    openOne: 'Deschide documentul',
    detailsTitle: 'Document',
    backToList: 'Înapoi la documente',
    correct: 'Corectează documentul',
    saveCorrection: 'Salvează corectura',
    renew: 'Reînnoiește',
    renewNote: 'Reînnoirea creează o înregistrare nouă. Cea de acum rămâne în istoric, neatinsă.',
    saveRenewal: 'Salvează reînnoirea',
    backToVehicle: 'Înapoi la vehicul',
    none: 'Niciun document înregistrat pentru acest vehicul.',
    add: 'Adaugă un document',
    save: 'Salvează documentul',
    delete: 'Șterge',
    confirmDelete: 'Ștergi acest document? Dispare și din istoric.',
    confirmDeleteYes: 'Da, șterge',
    cancel: 'Renunță',
    period: 'Valabil până la {{until}}.',
    periodFrom: 'Valabil de la {{from}} până la {{until}}.',
    type: {
      RCA: 'RCA',
      CASCO: 'CASCO',
      ITP: 'ITP',
      ROVINIETA: 'Rovinietă',
    },
    fields: {
      type: 'Tipul documentului',
      validFrom: 'Valabil de la',
      validUntil: 'Valabil până la',
      provider: 'Asigurător sau stație',
      referenceNumber: 'Număr poliță sau referință',
      notes: 'Observații',
    },
    state: {
      active: 'Valabil încă {{days}} zile.',
      soon: 'Expiră în {{days}} zile.',
      urgent: 'Expiră în {{days}} zile — urgent.',
      expiresToday: 'Expiră astăzi.',
      lapsed: 'Expirat de {{days}} zile.',
      lapsedUntil: 'Expirat de {{days}} zile. Acoperirea reîncepe pe {{date}}.',
      startsOn: 'Nu ești acoperit azi. Acoperirea începe pe {{date}}.',
      notCovered: 'Nu ești acoperit azi.',
      notConfigured: 'Neconfigurat.',
    },
  },
    history: {
    title: 'Istoric',
    open: 'Istoricul documentelor',
    filter: 'Arată doar',
    allTypes: 'Toate tipurile',
    none: 'Nicio înregistrare.',
    page: 'Pagina {{page}} din {{pages}}.',
    total: '{{total}} înregistrări în total.',
    previous: 'Pagina anterioară',
    next: 'Pagina următoare',
  },
  reminders: {
    title: 'Notificări',
    // Section 18 makes push native-only, and section 18 also names what the web
    // does instead. Both halves are said here because a screen listing "sent"
    // reminders without the first half is telling every current user something
    // false about their own car.
    nativeOnly: 'Notificările push ajung în aplicația de telefon, care încă nu există. '
      + 'Până atunci, starea documentelor e mereu vizibilă pe tabloul de bord.',
    none: 'Niciun reminder programat pentru acest document.',
    lead: {
      onTheDay: 'În ziua expirării:',
      oneDay: 'Cu o zi înainte:',
      fewDays: 'Cu {{days}} zile înainte:',
      manyDays: 'Cu {{days}} de zile înainte:',
    },
    outcome: {
      scheduled: 'programat pentru {{when}}.',
      sending: 'se trimite acum.',
      // "Marcat ca trimis", not "trimis": in V1 web the reminder completed
      // having reached no device at all, and the wording says so.
      sent: 'marcat ca trimis pe {{when}}.',
      failed: 'nu a putut fi trimis.',
      cancelled: 'anulat.',
    },
  },
  profile: {
    signOut: 'Ieși din cont',
    account: 'Datele contului',
    save: 'Salvează modificările',
    saved: 'Modificările au fost salvate.',
    emailVerified: '(confirmată)',
    emailNotVerified: '(neconfirmată)',
    security: 'Securitate',
    changePassword: 'Schimbă parola',
    changeEmail: 'Schimbă adresa de email',
    deleteAccount: 'Șterge contul',
    back: 'Înapoi la profil',
  },
  changePassword: {
    // Said before the form, not after. Somebody who learns this only once every
    // device has been signed out has been surprised by their own action.
    warning: 'Schimbarea parolei închide toate sesiunile, pe toate dispozitivele, '
      + 'inclusiv aceasta. Va trebui să te autentifici din nou.',
    submit: 'Schimbă parola',
  },
  changeEmail: {
    // The property the whole design rests on, and the one thing a person will
    // otherwise get wrong: they will go looking in the new inbox.
    instructions: 'Codul de confirmare merge la adresa ta actuală, nu la cea nouă. '
      + 'Așa știm că tu ceri schimbarea.',
    request: 'Trimite codul',
    codeSent: 'Am trimis un cod la adresa ta actuală. Introdu-l mai jos.',
    confirm: 'Confirmă adresa nouă',
    done: 'Adresa a fost schimbată. Acum e neconfirmată: '
      + 'caută emailul de verificare trimis la noua adresă.',
  },
  deleteAccount: {
    warning: 'Ștergerea contului este definitivă. Dispar vehiculele, certificatele, '
      + 'documentele, fotografiile și istoricul. Nu există cale de întoarcere.',
    confirm: 'Am înțeles, șterge contul',
    submit: 'Șterge contul definitiv',
    cancel: 'Renunță',
  },
  notificationPreferences: {
    title: 'Preferințe de notificare',
    enabled: 'Trimite-mi notificări',
    leads: 'Cu cât timp înainte să te anunțăm',
    remind30Days: 'Cu 30 de zile înainte',
    remind14Days: 'Cu 14 zile înainte',
    remind7Days: 'Cu 7 zile înainte',
    remind3Days: 'Cu 3 zile înainte',
    remind1Day: 'Cu o zi înainte',
    remindOnExpiry: 'În ziua expirării',
    time: 'Ora la care le primești',
    save: 'Salvează preferințele',
    saved: 'Preferințele au fost salvate.',
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
    changePassword: 'Schimbarea parolei',
    changeEmail: 'Schimbarea adresei de email',
    deleteAccount: 'Ștergerea contului',
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
    OCR_FILE_INVALID: 'Fișierul nu este o fotografie pe care o putem citi. Încearcă un JPEG sau un PNG clar.',
    OCR_RATE_LIMITED: 'Ai folosit deocamdată toate scanările disponibile. Încearcă din nou mai târziu.',
    IMAGE_TOO_LARGE: 'Fotografia este prea mare. Încearcă una mai mică.',
    IMAGE_INVALID_TYPE: 'Fișierul nu este o fotografie pe care o putem păstra. Încearcă un JPEG sau un PNG.',
    STORAGE_PROVIDER_UNAVAILABLE: 'Nu putem salva fotografii chiar acum. Încearcă din nou în câteva minute.',
    OCR_PROVIDER_UNAVAILABLE: 'Serviciul de citire a fotografiilor nu răspunde acum. Încearcă din nou în câteva minute.',
    OCR_PROCESSING_FAILED: 'Fotografia nu a putut fi procesată. Poți completa datele manual.',
    VALIDATION_ERROR: 'Verifică datele introduse.',
    MALFORMED_REQUEST: 'Cererea nu a putut fi citită. Încearcă din nou.',
    RESOURCE_NOT_FOUND: 'Resursa nu a fost găsită.',
    METHOD_NOT_ALLOWED: 'Operațiunea nu este permisă aici.',
    RATE_LIMITED: 'Prea multe încercări. Așteaptă puțin și încearcă din nou.',
    INTERNAL_ERROR: 'A apărut o problemă. Încearcă din nou mai târziu.',
    /** Not a backend code. What a code this frontend has never heard of becomes. */
    UNKNOWN: 'A apărut o problemă. Încearcă din nou mai târziu.',
    DOCUMENT_NOT_FOUND: 'Documentul nu a fost găsit.',
    DOCUMENT_INVALID_DATE_RANGE: 'Data de început este după cea de sfârșit.',
    DOCUMENT_TYPE_INVALID: 'Tipul de document nu este recunoscut.',
  },
}