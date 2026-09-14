import { describe, expect, it } from 'vitest'

import { CONTACT_EMAIL, OPERATOR_NAME, type LegalBlock, type LegalDocument } from './document.ts'
import { privacyPolicy } from './privacy.ts'
import { termsAndConditions } from './terms.ts'

/** A document's structure without its words: what a translation must keep. */
function shapeOf(document: LegalDocument): string[] {
  return document.sections.flatMap(section => section.blocks.map((block: LegalBlock) => {
    if ('paragraph' in block) return 'paragraph'
    if ('list' in block) return `list:${String(block.list.length)}`
    return `table:${String(block.table.head.length)}x${String(block.table.rows.length)}`
  }))
}

const documents = [
  ['privacy policy', privacyPolicy],
  ['terms and conditions', termsAndConditions],
] as const

describe.each(documents)('the %s', (_, doc) => {
  /**
   * The job `typeof ro` does for the locales. A sentence added to one language
   * and forgotten in the other would otherwise go unnoticed until somebody who
   * reads both happened to compare them.
   */
  it('has the same sections, lists and tables in Romanian and in English', () => {
    expect(doc.en.sections).toHaveLength(doc.ro.sections.length)
    expect(shapeOf(doc.en)).toEqual(shapeOf(doc.ro))
  })

  it('names the operator and the contact address in both languages', () => {
    for (const language of [doc.ro, doc.en]) {
      const text = JSON.stringify(language)
      expect(text).toContain(OPERATOR_NAME)
      expect(text).toContain(CONTACT_EMAIL)
    }
  })
})

describe('the privacy policy', () => {
  /**
   * Every external service the application sends personal data to, as listed
   * in PROJECT_STATE's production table on 2026-09-14. A provider added to the
   * application and not to this list is the omission that makes the policy
   * untrue, so the list is asserted rather than trusted.
   */
  it('lists every provider the application uses, in both languages', () => {
    const providers = ['Neon', 'Railway', 'Cloudflare', 'Document AI', 'Firebase', 'Apple Push', 'Resend', 'Sentry']

    for (const language of [privacyPolicy.ro, privacyPolicy.en]) {
      const text = JSON.stringify(language)
      for (const provider of providers) {
        expect(text, provider).toContain(provider)
      }
    }
  })
})