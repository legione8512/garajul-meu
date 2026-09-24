import { CONTACT_EMAIL, OPERATOR_NAME, type LegalDocuments } from './document.ts'

/**
 * The privacy policy, replacing the placeholder that stood here since phase 13.
 *
 * <p><strong>Written on 2026-09-14 from what the code does</strong> - the eleven
 * migrations, `UserService.deleteAccount`, `EmailMessages`, `AuthRateLimit`, the
 * Sentry settings in application.yml and the providers in PROJECT_STATE's
 * production table - and approved by the developer, who confirmed what the code
 * could not say: the operator and the contact address, Railway in EU West
 * (Amsterdam), Neon's six-hour restore window, and seven days of logs on
 * Railway's Hobby plan. It is not legal advice, and the draft said so.
 *
 * <p><strong>Updated on 2026-09-16, when production moved from Neon to
 * Supabase</strong> (free plan, Frankfurt): the provider in section 3, and the
 * backup sentence in section 4. Supabase's free plan keeps no project backups -
 * the dashboard says so in those words - so the six hours that were Neon's
 * restore window became "no backups", which is also the more honest thing to
 * tell somebody deciding what to store.
 *
 * <p>TRIGGER for revisiting, because each makes a sentence here false: a new
 * table or column holding personal data, a new external service, a change of
 * Railway plan or region, a Supabase plan with backups or a different region,
 * analytics or crash reporting inside the application, or email beyond the
 * account's own messages.
 *
 * <p><strong>Updated on 2026-09-23 for 1.1's recurring payments</strong> - V13's
 * `vehicle_payments`, which the first trigger above names: the dates of a loan
 * or CASCO instalment, and the reminders before them. No amount is stored, and
 * the text says so. The document list gained the extinguisher and the first-aid
 * kit, which 1.0.2 added without this file following.
 *
 * <p><strong>And for 1.1's Sugestii tab</strong> - V14's `feedback_messages`,
 * and email beyond the account's own messages, both on the trigger list: what
 * is kept, that it is emailed to the contact address, and the legal basis.
 */
export const privacyPolicy: LegalDocuments = {
  ro: {
    updated: '23 septembrie 2026',
    sections: [
      {
        title: '1. Cine suntem',
        blocks: [
          {
            paragraph: `Aplicația Garajul Meu (aplicația mobilă pentru iOS și Android și site-ul app.cyber-half.com) este operată de ${OPERATOR_NAME}, persoană fizică, care este operatorul datelor tale personale în sensul Regulamentului (UE) 2016/679 („GDPR”).`,
          },
          { paragraph: `Contact pentru orice întrebare despre datele tale: ${CONTACT_EMAIL}.` },
        ],
      },
      {
        title: '2. Ce date prelucrăm și de ce',
        blocks: [
          { paragraph: 'Contul tău:' },
          {
            list: [
              'numele complet, adresa de email și parola; parola nu este păstrată ca atare, ci doar ca amprentă criptografică Argon2, din care nu poate fi recuperată;',
              'limba preferată și fusul orar, pentru a-ți afișa aplicația și a-ți trimite memento-urile la ora potrivită;',
              'data creării contului și dacă adresa de email a fost confirmată.',
            ],
          },
          { paragraph: 'Scop: crearea și administrarea contului. Temei: executarea contractului (art. 6 alin. (1) lit. b) GDPR).' },
          { paragraph: 'Vehiculele și documentele lor:' },
          {
            list: [
              'datele vehiculului pe care le introduci sau pe care aplicația le citește din certificatul de înmatriculare: numărul de înmatriculare, seria de șasiu (VIN), marca, modelul, datele tehnice, datele de înmatriculare și celelalte câmpuri ale certificatului;',
              'opțional, numele și adresa proprietarului și ale utilizatorului, așa cum apar pe certificat (câmpurile C.2 și C.3); aceste câmpuri nu sunt obligatorii și nu sunt folosite niciodată pentru memento-uri;',
              'documentele vehiculului (RCA, ITP, CASCO, rovinietă, extinctor, trusă medicală): perioada de valabilitate, emitentul, numărul documentului și notițele tale;',
              'ratele pe care le adaugi pentru un vehicul (rata la leasing sau credit auto și rata CASCO): data primei rate, cât de des vin, data ultimei rate și cu câte zile înainte vrei memento-ul; nu păstrăm sumele, contractul sau numele finanțatorului;',
              'o fotografie a vehiculului, dacă alegi să adaugi una.',
            ],
          },
          { paragraph: 'Scop: evidența documentelor și afișarea stării lor. Temei: executarea contractului. Dacă introduci date despre alte persoane, de exemplu un proprietar sau un utilizator diferit de tine, ești responsabil să ai dreptul de a le introduce.' },
          {
            paragraph: 'Fotografia certificatului de înmatriculare: când fotografiezi certificatul, imaginea este trimisă serviciului Google Cloud Document AI, printr-un punct de procesare din Uniunea Europeană, doar pentru a citi câmpurile. Aplicația nu păstrează imaginea certificatului, ci numai valorile pe care le verifici și le salvezi. Păstrăm un contor zilnic al citirilor, pentru a limita folosirea abuzivă. Temei: executarea contractului.',
          },
          { paragraph: 'Memento-urile și notificările:' },
          {
            list: [
              'preferințele tale de notificare: dacă memento-urile sunt pornite, cu câte zile înainte și la ce oră;',
              'memento-urile programate pentru fiecare document și pentru fiecare rată, și rezultatul trimiterii lor;',
              'pentru fiecare dispozitiv pe care ai activat notificările: platforma (iOS sau Android) și tokenul de notificări emis de Firebase Cloud Messaging, păstrat criptat.',
            ],
          },
          { paragraph: 'Scop: trimiterea memento-urilor înainte de expirarea documentelor. Temei: executarea contractului. Notificările push sunt trimise numai după ce le permiți în sistemul dispozitivului, iar permisiunea o poți retrage oricând din setări.' },
          { paragraph: 'Emailuri: îți trimitem emailuri numai pentru cont — confirmarea adresei de email, resetarea parolei și codul de schimbare a adresei. Nu trimitem newsletter sau publicitate.' },
          { paragraph: 'Sugestiile: când ne scrii din fila Sugestii, păstrăm tipul mesajului, textul lui, platforma și versiunea aplicației și data trimiterii. Mesajul ne este trimis și pe email, prin Resend, la adresa de contact de mai sus, împreună cu numele și adresa de email a contului, ca să-ți putem răspunde. Scop: să îți răspundem și să îmbunătățim aplicația. Temei: interesul nostru legitim (art. 6 alin. (1) lit. f) GDPR). Nu scrie în mesaj date pe care nu vrei să ni le trimiți.' },
          { paragraph: 'Securitate și funcționare:' },
          {
            list: [
              'sesiunile de autentificare, păstrate ca amprentă criptografică și valabile 30 de zile;',
              'codurile de confirmare și de resetare a parolei, care expiră automat;',
              'adresa IP, folosită doar în memorie pentru a limita încercările repetate de autentificare, fără a fi salvată în baza de date;',
              'rapoarte despre erorile tehnice ale serverului, trimise serviciului Sentry fără date personale (fără adresa IP și fără conținutul cererilor).',
            ],
          },
          { paragraph: 'Temei: interesul legitim de a proteja conturile și de a menține aplicația funcțională (art. 6 alin. (1) lit. f) GDPR).' },
          { paragraph: 'Ce nu facem: nu vindem datele, nu afișăm reclame, nu folosim servicii de analiză sau de urmărire și nu creăm profiluri de marketing.' },
        ],
      },
      {
        title: '3. Cine primește datele',
        blocks: [
          { paragraph: 'Folosim următorii furnizori, doar pentru a face aplicația să funcționeze:' },
          {
            table: {
              head: ['Furnizor', 'Pentru ce', 'Unde'],
              rows: [
                ['Supabase (baza de date)', 'Stocarea datelor contului, vehiculelor și documentelor', 'UE — Frankfurt'],
                ['Railway (serverul aplicației)', 'Rularea serverului', 'UE — Amsterdam'],
                ['Cloudflare (R2 și Pages)', 'Stocarea fotografiilor vehiculelor; găzduirea site-ului', 'Fotografii: preferință de locație UE; site: rețea globală'],
                ['Google Cloud Document AI', 'Citirea câmpurilor din fotografia certificatului', 'UE'],
                ['Google Firebase Cloud Messaging', 'Trimiterea notificărilor push', 'Global'],
                ['Apple Push Notification service', 'Livrarea notificărilor pe iPhone și iPad', 'Global'],
                ['Resend', 'Trimiterea emailurilor legate de cont', 'UE — Irlanda'],
                ['Sentry', 'Rapoarte de erori tehnice, fără date personale', 'UE'],
              ],
            },
          },
          { paragraph: 'Unii furnizori (Google, Apple, Cloudflare) pot prelucra date și în afara Spațiului Economic European. În aceste cazuri, transferul se face pe baza garanțiilor prevăzute de GDPR, de exemplu Clauzele contractuale standard ale Comisiei Europene sau Cadrul UE–SUA privind protecția datelor.' },
          { paragraph: 'Nu transmitem datele altor persoane, cu excepția cazurilor în care legea ne obligă.' },
        ],
      },
      {
        title: '4. Cât timp păstrăm datele',
        blocks: [
          {
            list: [
              'Datele contului, vehiculelor, documentelor și fotografiile: cât timp ai cont.',
              'Fotografia unui vehicul se șterge când o înlocuiești, când ștergi vehiculul sau când ștergi contul.',
              'Sesiunile de autentificare expiră după 30 de zile; codurile trimise prin email expiră după 15 minute.',
              'Ștergerea contului este definitivă și imediată: odată cu contul se șterg vehiculele, certificatele, documentele, ratele, memento-urile, dispozitivele înregistrate, fotografiile și mesajele trimise din fila Sugestii. Copia unui mesaj primită pe email o păstrăm cât este nevoie pentru a-ți răspunde.',
              'Nu păstrăm copii de siguranță ale bazei de date, deci datele șterse nu mai pot fi recuperate. Jurnalele tehnice ale serverului dispar automat după cel mult 7 zile.',
            ],
          },
        ],
      },
      {
        title: '5. Drepturile tale',
        blocks: [
          { paragraph: 'Ai dreptul:' },
          {
            list: [
              'să afli ce date avem despre tine și să primești o copie a lor (acces și portabilitate);',
              'să corectezi datele — le poți modifica direct în aplicație;',
              'să ceri ștergerea datelor — poți șterge contul direct din aplicație, din Profil;',
              'să ceri restricționarea prelucrării sau să te opui prelucrării bazate pe interesul legitim;',
              'să îți retragi consimțământul pentru notificări, din setările dispozitivului.',
            ],
          },
          { paragraph: `Pentru oricare dintre aceste drepturi scrie-ne la ${CONTACT_EMAIL}. Îți răspundem în cel mult o lună.` },
          { paragraph: 'Dacă consideri că datele tale nu sunt prelucrate corect, poți depune o plângere la Autoritatea Națională de Supraveghere a Prelucrării Datelor cu Caracter Personal (ANSPDCP), www.dataprotection.ro.' },
        ],
      },
      {
        title: '6. Stocarea locală pe dispozitiv',
        blocks: [
          {
            list: [
              'Site-ul folosește un singur cookie strict necesar, pentru a te menține autentificat. Nu folosim cookie-uri de analiză sau de publicitate.',
              'Aplicația păstrează local limba aleasă și, în aplicația mobilă, tokenul de notificări înregistrat.',
              'În aplicația mobilă, sesiunea este păstrată în depozitul securizat al sistemului: Keychain pe iOS, Keystore pe Android.',
            ],
          },
        ],
      },
      {
        title: '7. Copii',
        blocks: [
          { paragraph: 'Aplicația nu se adresează persoanelor sub 16 ani și nu colectăm cu bună știință date despre acestea.' },
        ],
      },
      {
        title: '8. Modificări',
        blocks: [
          { paragraph: 'Dacă modificăm această politică, publicăm versiunea nouă pe această pagină, cu data actualizării. Pentru schimbări importante te anunțăm în aplicație sau prin email.' },
        ],
      },
    ],
  },

  en: {
    updated: '23 September 2026',
    sections: [
      {
        title: '1. Who we are',
        blocks: [
          {
            paragraph: `Garajul Meu (the mobile application for iOS and Android and the website app.cyber-half.com) is operated by ${OPERATOR_NAME}, a natural person, who is the controller of your personal data within the meaning of Regulation (EU) 2016/679 (the "GDPR").`,
          },
          { paragraph: `For any question about your data, write to ${CONTACT_EMAIL}.` },
        ],
      },
      {
        title: '2. What data we process and why',
        blocks: [
          { paragraph: 'Your account:' },
          {
            list: [
              'your full name, email address and password; the password is not kept as such, only as an Argon2 cryptographic hash from which it cannot be recovered;',
              'your preferred language and time zone, to show you the application and send your reminders at the right time;',
              'when the account was created and whether its email address has been confirmed.',
            ],
          },
          { paragraph: 'Purpose: creating and managing your account. Legal basis: performance of a contract (Article 6(1)(b) GDPR).' },
          { paragraph: 'Your vehicles and their documents:' },
          {
            list: [
              'the vehicle details you enter or that the application reads from the registration certificate: registration number, vehicle identification number (VIN), make, model, technical data, registration dates and the certificate\'s other fields;',
              'optionally, the name and address of the owner and of the user as they appear on the certificate (fields C.2 and C.3); these fields are not required and are never used for reminders;',
              'the vehicle\'s documents (liability insurance, roadworthiness test, comprehensive cover, road tax, fire extinguisher, first-aid kit): validity period, issuer, document number and your notes;',
              'the instalments you add for a vehicle (a car-loan or leasing instalment and a CASCO instalment): the first due date, how often they fall due, the last due date and how many days ahead you want to be reminded; we keep no amounts, no contract and no lender\'s name;',
              'a photograph of the vehicle, if you choose to add one.',
            ],
          },
          { paragraph: 'Purpose: keeping track of the documents and showing their status. Legal basis: performance of a contract. If you enter data about other people, such as an owner or user other than yourself, you are responsible for being entitled to enter it.' },
          {
            paragraph: 'The photograph of the registration certificate: when you photograph the certificate, the image is sent to Google Cloud Document AI, through a processing location in the European Union, only to read its fields. The application does not keep the image of the certificate, only the values you check and save. We keep a daily count of readings, to limit abuse. Legal basis: performance of a contract.',
          },
          { paragraph: 'Reminders and notifications:' },
          {
            list: [
              'your notification preferences: whether reminders are on, how many days in advance and at what time;',
              'the reminders scheduled for each document and each instalment, and the outcome of sending them;',
              'for each device on which you turned notifications on: its platform (iOS or Android) and the notification token issued by Firebase Cloud Messaging, stored encrypted.',
            ],
          },
          { paragraph: 'Purpose: sending reminders before documents expire. Legal basis: performance of a contract. Push notifications are sent only after you allow them in your device\'s settings, and you can withdraw that permission at any time.' },
          { paragraph: 'Email: we send email only about your account — confirming your email address, resetting your password and the code for changing your address. We send no newsletters and no advertising.' },
          { paragraph: 'Suggestions: when you write to us from the Suggestions tab, we keep the kind of message, its text, the app\'s platform and version and the date it was sent. The message is also emailed to us, through Resend, at the contact address above, together with your account\'s name and email address, so that we can reply. Purpose: replying to you and improving the app. Legal basis: our legitimate interest (Article 6(1)(f) GDPR). Please do not put in a message anything you do not want to send us.' },
          { paragraph: 'Security and operation:' },
          {
            list: [
              'sign-in sessions, stored as a cryptographic hash and valid for 30 days;',
              'confirmation and password reset codes, which expire automatically;',
              'your IP address, used only in memory to limit repeated sign-in attempts and never saved to the database;',
              'reports of the server\'s technical errors, sent to Sentry without personal data (no IP address and no request content).',
            ],
          },
          { paragraph: 'Legal basis: our legitimate interest in protecting accounts and keeping the application working (Article 6(1)(f) GDPR).' },
          { paragraph: 'What we do not do: we do not sell data, show advertising, use analytics or tracking services, or build marketing profiles.' },
        ],
      },
      {
        title: '3. Who receives the data',
        blocks: [
          { paragraph: 'We use the following providers, only to make the application work:' },
          {
            table: {
              head: ['Provider', 'What for', 'Where'],
              rows: [
                ['Supabase (database)', 'Storing account, vehicle and document data', 'EU — Frankfurt'],
                ['Railway (application server)', 'Running the server', 'EU — Amsterdam'],
                ['Cloudflare (R2 and Pages)', 'Storing vehicle photographs; hosting the website', 'Photographs: EU location preference; website: global network'],
                ['Google Cloud Document AI', 'Reading the fields of the certificate photograph', 'EU'],
                ['Google Firebase Cloud Messaging', 'Sending push notifications', 'Global'],
                ['Apple Push Notification service', 'Delivering notifications to iPhone and iPad', 'Global'],
                ['Resend', 'Sending account email', 'EU — Ireland'],
                ['Sentry', 'Technical error reports, without personal data', 'EU'],
              ],
            },
          },
          { paragraph: 'Some providers (Google, Apple, Cloudflare) may also process data outside the European Economic Area. Where they do, the transfer relies on the safeguards the GDPR provides, such as the European Commission\'s Standard Contractual Clauses or the EU–US Data Privacy Framework.' },
          { paragraph: 'We do not pass your data to anyone else, except where the law requires us to.' },
        ],
      },
      {
        title: '4. How long we keep data',
        blocks: [
          {
            list: [
              'Account, vehicle and document data, and photographs: for as long as you have an account.',
              'A vehicle\'s photograph is deleted when you replace it, when you delete the vehicle, or when you delete your account.',
              'Sign-in sessions expire after 30 days; codes sent by email expire after 15 minutes.',
              'Deleting your account is permanent and immediate: the vehicles, certificates, documents, instalments, reminders, registered devices, photographs and the messages sent from the Suggestions tab are deleted with it. The copy of a message received by email is kept for as long as it takes to reply to you.',
              'We keep no backups of the database, so deleted data cannot be recovered. The server\'s technical logs disappear automatically after at most 7 days.',
            ],
          },
        ],
      },
      {
        title: '5. Your rights',
        blocks: [
          { paragraph: 'You have the right:' },
          {
            list: [
              'to know what data we hold about you and to receive a copy of it (access and portability);',
              'to correct your data — you can change it directly in the application;',
              'to have your data erased — you can delete your account directly in the application, from your profile;',
              'to ask for processing to be restricted, or to object to processing based on legitimate interest;',
              'to withdraw your consent to notifications, from your device\'s settings.',
            ],
          },
          { paragraph: `To exercise any of these rights, write to ${CONTACT_EMAIL}. We answer within one month.` },
          { paragraph: 'If you believe your data is not being processed correctly, you can complain to the Romanian data protection authority, the National Supervisory Authority for Personal Data Processing (ANSPDCP), www.dataprotection.ro.' },
        ],
      },
      {
        title: '6. Storage on your device',
        blocks: [
          {
            list: [
              'The website uses a single strictly necessary cookie, to keep you signed in. We use no analytics or advertising cookies.',
              'The application stores your chosen language locally and, in the mobile application, the notification token it registered.',
              'In the mobile application, your session is kept in the system\'s secure storage: the Keychain on iOS and the Keystore on Android.',
            ],
          },
        ],
      },
      {
        title: '7. Children',
        blocks: [
          { paragraph: 'The application is not intended for people under 16, and we do not knowingly collect their data.' },
        ],
      },
      {
        title: '8. Changes',
        blocks: [
          { paragraph: 'If we change this policy, we publish the new version on this page with the date it was updated. For significant changes we let you know in the application or by email.' },
        ],
      },
    ],
  },
}