import type { SupportedLanguage } from '../i18n/language.ts'

/**
 * Who operates the application, and where to write. Named once, because the
 * privacy policy, the terms and the support section of the features page all
 * print them, and a second spelling of an address is the one nobody updates.
 *
 * <p>Confirmed by the developer on 2026-09-14: the operator is a natural
 * person, and this address is the public contact. It is also the support
 * contact App Store Connect asks for.
 */
export const OPERATOR_NAME = 'Marius Gheorghe'
export const CONTACT_EMAIL = 'in.garaj.meu@gmail.com'

/**
 * A legal document as data rather than as markup, in each language.
 *
 * <p><strong>Not in the locale files, and on purpose.</strong> Those hold the
 * interface's strings, key by key, and a key per sentence of a privacy policy
 * would turn a document somebody has to read top to bottom into a scatter of
 * keys nobody can review as a whole. Here each language is one document, in
 * reading order, and `documents.test.ts` holds the two languages to the same
 * shape - the job `typeof ro` does for the locales.
 *
 * <p>Plain text only. The contact address is made a link where it appears;
 * nothing else is interpreted, so a document cannot inject markup.
 */
export type LegalBlock =
  | { readonly paragraph: string }
  | { readonly list: readonly string[] }
  | { readonly table: { readonly head: readonly string[], readonly rows: readonly (readonly string[])[] } }

export interface LegalSection {
  readonly title: string
  readonly blocks: readonly LegalBlock[]
}

export interface LegalDocument {
  /** When the wording last changed, as a date a person reads. */
  readonly updated: string
  readonly sections: readonly LegalSection[]
}

export type LegalDocuments = Readonly<Record<SupportedLanguage, LegalDocument>>