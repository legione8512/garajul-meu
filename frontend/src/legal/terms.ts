import { CONTACT_EMAIL, OPERATOR_NAME, type LegalDocuments } from './document.ts'

/**
 * The terms and conditions, replacing the placeholder that stood here since
 * phase 13. Drafted on 2026-09-14 alongside the privacy policy and approved by
 * the developer; not legal advice.
 *
 * <p>Section 5 is the one that matters most for this application: a reminder is
 * help, not a guarantee, and the person stays responsible for renewing their
 * documents. Section 8 is where the make emblems of 2026-09-14 are accounted for.
 *
 * <p>TRIGGER for revisiting: the application stops being free, gains paid
 * features or advertising, or is operated by a company rather than a person.
 */
export const termsAndConditions: LegalDocuments = {
  ro: {
    updated: '15 septembrie 2026',
    sections: [
      {
        title: '1. Cine oferă serviciul',
        blocks: [
          { paragraph: `Garajul Meu este oferit de ${OPERATOR_NAME}, persoană fizică. Contact: ${CONTACT_EMAIL}.` },
        ],
      },
      {
        title: '2. Ce oferă aplicația',
        blocks: [
          { paragraph: 'Garajul Meu te ajută să ții evidența documentelor vehiculelor tale (RCA, ITP, CASCO, rovinietă), să citești automat datele din certificatul de înmatriculare și să primești memento-uri înainte de expirarea documentelor. Aplicația este gratuită.' },
        ],
      },
      {
        title: '3. Contul',
        blocks: [
          {
            list: [
              'Pentru a folosi aplicația îți creezi un cont cu o adresă de email validă.',
              'Ești responsabil pentru păstrarea parolei și pentru activitatea din contul tău.',
              'Poți șterge contul oricând, din Profil. Ștergerea este definitivă.',
            ],
          },
        ],
      },
      {
        title: '4. Obligațiile tale',
        blocks: [
          {
            list: [
              'Introduci date corecte și date pe care ai dreptul să le folosești, inclusiv datele altor persoane care apar pe certificatul de înmatriculare.',
              'Verifici valorile citite automat din certificat înainte de salvare. Citirea automată poate greși.',
              'Nu folosești aplicația în scopuri ilegale, nu încerci să accesezi conturile altor persoane și nu perturbi funcționarea serviciului.',
            ],
          },
        ],
      },
      {
        title: '5. Memento-urile au rol informativ',
        blocks: [
          { paragraph: 'Memento-urile și starea documentelor afișată în aplicație te ajută să nu uiți, dar nu înlocuiesc verificarea documentelor oficiale. Rămâi responsabil pentru reînnoirea la timp a documentelor vehiculului.' },
          { paragraph: 'Un memento poate să nu ajungă, de exemplu dacă notificările sunt oprite, dispozitivul nu are conexiune sau un serviciu extern nu funcționează. În limitele permise de lege, nu răspundem pentru amenzi, pagube sau alte consecințe rezultate din documente expirate sau din date introduse greșit.' },
        ],
      },
      {
        title: '6. Disponibilitatea serviciului',
        blocks: [
          { paragraph: 'Facem eforturi rezonabile ca aplicația să funcționeze, dar nu garantăm că va fi disponibilă permanent sau fără erori. Putem modifica, suspenda sau opri serviciul; în cazul opririi definitive te anunțăm din timp, pentru a-ți putea nota datele.' },
        ],
      },
      {
        title: '7. Suspendarea contului',
        blocks: [
          { paragraph: 'Putem suspenda sau închide un cont folosit abuziv sau cu încălcarea acestor termeni.' },
        ],
      },
      {
        title: '8. Proprietate intelectuală',
        blocks: [
          { paragraph: 'Aplicația, designul și conținutul ei aparțin operatorului. Emblemele și denumirile mărcilor de vehicule afișate în aplicație aparțin proprietarilor lor și sunt folosite doar pentru a identifica marca vehiculului tău.' },
        ],
      },
      {
        title: '9. Datele personale',
        blocks: [
          { paragraph: 'Modul în care prelucrăm datele personale este descris în Politica de confidențialitate.' },
        ],
      },
      {
        title: '10. Modificarea termenilor',
        blocks: [
          { paragraph: 'Putem actualiza acești termeni. Versiunea nouă se publică pe această pagină, cu data actualizării; pentru schimbări importante te anunțăm în aplicație sau prin email. Dacă nu ești de acord, poți șterge contul.' },
        ],
      },
      {
        title: '11. Legea aplicabilă',
        blocks: [
          { paragraph: 'Acești termeni sunt guvernați de legea română. Dacă ești consumator, beneficiezi de protecția oferită de legislația privind protecția consumatorilor. Eventualele neînțelegeri se soluționează pe cale amiabilă, iar în lipsa unei înțelegeri, de instanțele competente din România.' },
        ],
      },
    ],
  },

  en: {
    updated: '15 September 2026',
    sections: [
      {
        title: '1. Who provides the service',
        blocks: [
          { paragraph: `Garajul Meu is provided by ${OPERATOR_NAME}, a natural person. Contact: ${CONTACT_EMAIL}.` },
        ],
      },
      {
        title: '2. What the application offers',
        blocks: [
          { paragraph: 'Garajul Meu helps you keep track of your vehicles\' documents (liability insurance, roadworthiness test, comprehensive cover, road tax), read the registration certificate\'s details automatically, and receive reminders before documents expire. The application is free.' },
        ],
      },
      {
        title: '3. Your account',
        blocks: [
          {
            list: [
              'To use the application you create an account with a valid email address.',
              'You are responsible for keeping your password safe and for the activity in your account.',
              'You can delete your account at any time, from your profile. Deletion is permanent.',
            ],
          },
        ],
      },
      {
        title: '4. Your obligations',
        blocks: [
          {
            list: [
              'You enter accurate data, and data you are entitled to use, including the details of other people who appear on the registration certificate.',
              'You check the values read automatically from the certificate before saving them. Automatic reading can be wrong.',
              'You do not use the application for unlawful purposes, try to access other people\'s accounts, or disrupt the service.',
            ],
          },
        ],
      },
      {
        title: '5. Reminders are for information',
        blocks: [
          { paragraph: 'Reminders and the document status shown in the application help you not to forget, but they do not replace checking the official documents. You remain responsible for renewing your vehicle\'s documents on time.' },
          { paragraph: 'A reminder may not arrive, for example if notifications are turned off, the device has no connection, or an external service fails. To the extent permitted by law, we are not liable for fines, damage or other consequences arising from expired documents or from incorrectly entered data.' },
        ],
      },
      {
        title: '6. Availability of the service',
        blocks: [
          { paragraph: 'We make reasonable efforts to keep the application working, but we do not guarantee that it will always be available or free of errors. We may change, suspend or discontinue the service; if we discontinue it permanently, we will tell you in advance so that you can keep a note of your data.' },
        ],
      },
      {
        title: '7. Suspending an account',
        blocks: [
          { paragraph: 'We may suspend or close an account that is used abusively or in breach of these terms.' },
        ],
      },
      {
        title: '8. Intellectual property',
        blocks: [
          { paragraph: 'The application, its design and its content belong to the operator. The vehicle make emblems and names shown in the application belong to their owners and are used only to identify the make of your vehicle.' },
        ],
      },
      {
        title: '9. Personal data',
        blocks: [
          { paragraph: 'How we process personal data is described in the Privacy policy.' },
        ],
      },
      {
        title: '10. Changes to these terms',
        blocks: [
          { paragraph: 'We may update these terms. The new version is published on this page with the date it was updated; for significant changes we let you know in the application or by email. If you do not agree, you can delete your account.' },
        ],
      },
      {
        title: '11. Governing law',
        blocks: [
          { paragraph: 'These terms are governed by Romanian law. If you are a consumer, you benefit from the protection of consumer protection law. Any disagreement is settled amicably first and, failing that, by the competent courts in Romania.' },
        ],
      },
    ],
  },
}