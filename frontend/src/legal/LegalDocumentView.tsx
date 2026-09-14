import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import { isSupportedLanguage } from '../i18n/language.ts'
import { CONTACT_EMAIL, type LegalBlock, type LegalDocuments } from './document.ts'

/**
 * The contact address as a link wherever a sentence names it, and nothing else
 * interpreted - the documents are plain text by design, see document.ts.
 */
function withContactLink(text: string): ReactNode[] {
  return text.split(CONTACT_EMAIL).flatMap((part, index) => index === 0
    ? [part]
    : [<a key={index} href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>, part])
}

function Block({ block }: { block: LegalBlock }) {
  if ('paragraph' in block) {
    return <p>{withContactLink(block.paragraph)}</p>
  }

  if ('list' in block) {
    return (
      <ul>
        {block.list.map(item => <li key={item}>{withContactLink(item)}</li>)}
      </ul>
    )
  }

  // Scrolls inside its own box on a phone rather than widening the page.
  return (
    <div data-legal-table>
      <table>
        <thead>
          <tr>{block.table.head.map(cell => <th key={cell} scope="col">{cell}</th>)}</tr>
        </thead>
        <tbody>
          {block.table.rows.map(row => (
            <tr key={row[0]}>
              {row.map((cell, index) => index === 0
                ? <th key={cell} scope="row">{cell}</th>
                : <td key={`${String(index)}-${cell}`}>{cell}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/**
 * A legal document in the language the interface is in, Romanian when that
 * cannot be told. The page title comes from the locale, like every other
 * screen's; the document itself does not, see document.ts.
 */
export function LegalDocumentView({ title, documents }: { title: string, documents: LegalDocuments }) {
  const { t, i18n } = useTranslation()
  const resolved = i18n.resolvedLanguage ?? null
  const document = documents[isSupportedLanguage(resolved) ? resolved : 'ro']

  return (
    <>
      <h1>{title}</h1>
      <p data-subtitle>{t('legal.updated', { date: document.updated })}</p>

      {document.sections.map(section => (
        <section key={section.title}>
          <h2>{section.title}</h2>
          {section.blocks.map((block, index) => <Block key={index} block={block} />)}
        </section>
      ))}
    </>
  )
}