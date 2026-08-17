import type { DocumentStatus } from '../api/endpoints/documents.ts'

/**
 * How urgent something is, for styling only.
 *
 * <p>Never the sole carrier of meaning. Every state also produces a sentence,
 * because a colour is invisible to a screen reader and ambiguous to a good part
 * of everybody else - the same reason 9.4 gave the three scan outlines different
 * line styles rather than three colours.
 */
export type DocumentTone = 'ok' | 'soon' | 'urgent' | 'gap' | 'unset'

/**
 * The nine sentences a state can produce, as literals.
 *
 * <p>Not `string`. i18next types `t` over the keys that actually exist, so a key
 * assembled at runtime resolves to the overload whose second argument is a
 * default *string* and the call stops compiling. Naming the nine here keeps the
 * checking rather than casting it away - a tenth sentence added to the locale
 * without being added here is a compile error, which is the point.
 */
export type DocumentStateKey =
  | 'documents.state.active'
  | 'documents.state.soon'
  | 'documents.state.urgent'
  | 'documents.state.expiresToday'
  | 'documents.state.lapsed'
  | 'documents.state.lapsedUntil'
  | 'documents.state.startsOn'
  | 'documents.state.notCovered'
  | 'documents.state.notConfigured'

/** A type alias, not an interface: `values` is handed straight to i18next. */
export type DocumentState = {
  readonly tone: DocumentTone
  readonly key: DocumentStateKey
  readonly values: Record<string, string | number>
}

/**
 * The subset both a stored document and a dashboard line satisfy, so one
 * function reads both. A stored document always has a `validUntil` and never a
 * `upcomingFrom`; a dashboard line may have neither, one, or both, and that is
 * exactly what has to be told apart.
 */
export interface CoverageFacts {
  readonly status: DocumentStatus
  readonly validUntil?: string | null
  readonly daysRemaining?: number | null
  readonly upcomingFrom?: string | null
}

/**
 * The sentence a state deserves.
 *
 * <p><strong>Three different situations reach this function as EXPIRED</strong>,
 * and section 11 offers no status to separate them - it has no state meaning
 * "bought but not started", which is the one place its vocabulary is thinner
 * than the data. The dates do the separating: cover that ran out, cover that ran
 * out with more already arranged, and cover that has never yet begun are three
 * sentences, not one red badge repeated.
 *
 * <p>The formatter is passed in rather than reached for. A date is written
 * differently in Romanian and in English, the language lives in i18next, and a
 * pure function that needed a hook could not be tested without one.
 */
export function stateOf(facts: CoverageFacts, formatDate: (iso: string) => string): DocumentState {
  if (facts.status === 'NOT_CONFIGURED') {
    return { tone: 'unset', key: 'documents.state.notConfigured', values: {} }
  }

  if (facts.status === 'EXPIRED') {
    const resumes = facts.upcomingFrom ?? null
    const lapsed = facts.validUntil ?? null

    if (lapsed === null) {
      return resumes === null
        ? { tone: 'gap', key: 'documents.state.notCovered', values: {} }
        : { tone: 'gap', key: 'documents.state.startsOn', values: { date: formatDate(resumes) } }
    }

    const days = Math.abs(facts.daysRemaining ?? 0)

    return resumes === null
      ? { tone: 'gap', key: 'documents.state.lapsed', values: { days } }
      : {
          tone: 'gap',
          key: 'documents.state.lapsedUntil',
          values: { days, date: formatDate(resumes) },
        }
  }

  const days = facts.daysRemaining ?? 0

  switch (facts.status) {
    case 'EXPIRES_TODAY':
      return { tone: 'urgent', key: 'documents.state.expiresToday', values: {} }
    case 'URGENT':
      return { tone: 'urgent', key: 'documents.state.urgent', values: { days } }
    case 'EXPIRING_SOON':
      return { tone: 'soon', key: 'documents.state.soon', values: { days } }
    default:
      return { tone: 'ok', key: 'documents.state.active', values: { days } }
  }
}

/**
 * A date as the reader writes them. Falls back to what arrived rather than
 * throwing: a malformed date from the server is a bad line on a screen, not a
 * blank one.
 */
export function dateFormatter(language: string): (iso: string) => string {
  return (iso: string) => {
    const parsed = new Date(iso)
    return Number.isNaN(parsed.getTime()) ? iso : parsed.toLocaleDateString(language)
  }
}