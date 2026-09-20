import type { DocumentStatus } from '../api/endpoints/documents.ts'
import { countForm, type CountForm } from '../i18n/countForm.ts'

/**
 * How urgent something is, for styling only.
 *
 * <p>Never the sole carrier of meaning. Every state also produces a sentence,
 * because a colour is invisible to a screen reader and ambiguous to a good part
 * of everybody else - the same reason 9.4 gave the three scan outlines different
 * line styles rather than three colours.
 */
export type DocumentTone = 'ok' | 'soon' | 'urgent' | 'gap' | 'unset'

/** The five sentences that count days, each in the three forms countForm names. */
type CountedState = 'active' | 'soon' | 'urgent' | 'lapsed' | 'lapsedUntil'

/**
 * The twenty sentences a state can produce, as literals.
 *
 * <p>Not `string`. i18next types `t` over the keys that actually exist, so a key
 * assembled at runtime resolves to the overload whose second argument is a
 * default *string* and the call stops compiling. Naming them here keeps the
 * checking rather than casting it away - the template below expands to fifteen
 * literal keys, and a form missing from either locale is a compile error, which
 * is the point.
 */
export type DocumentStateKey =
  | `documents.state.${CountedState}.${CountForm}`
  | 'documents.state.expiresToday'
  | 'documents.state.startsOn'
  | 'documents.state.notStarted'
  | 'documents.state.notCovered'
  | 'documents.state.notConfigured'

/** The key for a counted sentence in the form its number needs. */
function counted(state: CountedState, days: number): DocumentStateKey {
  return `documents.state.${state}.${countForm(days)}` as const
}

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
  /** A stored document's own start; a dashboard line has none. */
  readonly validFrom?: string | null
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
      ? { tone: 'gap', key: counted('lapsed', days), values: { days } }
      : {
          tone: 'gap',
          key: counted('lapsedUntil', days),
          values: { days, date: formatDate(resumes) },
        }
  }

  const days = facts.daysRemaining ?? 0

  if (hasNotStarted(facts)) {
    return {
      tone: 'ok',
      key: 'documents.state.notStarted',
      values: { date: formatDate(facts.validFrom ?? '') },
    }
  }

  switch (facts.status) {
    case 'EXPIRES_TODAY':
      return { tone: 'urgent', key: 'documents.state.expiresToday', values: {} }
    case 'URGENT':
      return { tone: 'urgent', key: counted('urgent', days), values: { days } }
    case 'EXPIRING_SOON':
      return { tone: 'soon', key: counted('soon', days), values: { days } }
    default:
      return { tone: 'ok', key: counted('active', days), values: { days } }
  }
}

const DAY_MS = 86_400_000

/**
 * Whether a stored document's period is still ahead of it.
 *
 * <p><strong>Found on 2026-09-15 on the demo account for App Review</strong>: an
 * RCA running from 5 December 2026 read "Valabil încă 445 zile" on its card in
 * September. The backend's per-document status is computed from `valid_until`
 * alone, which is right for how long a document lasts and wrong for whether it
 * has begun; section 11 forbids showing a policy that has not started as though
 * it were active, and the dashboard already honoured that through
 * `upcomingFrom`.
 *
 * <p><strong>Answered here without a clock.</strong> `daysRemaining` is the
 * backend's count from the reader's today, in the reader's timezone, to the last
 * valid day; the period's own length is the days from its first day to its last,
 * both plain dates. The document has not started exactly when more days remain
 * than the period holds. Two date-only strings subtracted as UTC midnights
 * carry no timezone, so this cannot disagree with the backend about which day
 * today is - reading a clock in the browser could.
 *
 * <p>The day a document starts is not "not started": remaining equals the
 * length, and it reads as active from that morning.
 */
function hasNotStarted(facts: CoverageFacts): boolean {
  const from = facts.validFrom ?? null
  const until = facts.validUntil ?? null
  const remaining = facts.daysRemaining ?? null

  if (from === null || until === null || remaining === null) {
    return false
  }

  const length = Math.round((Date.parse(until) - Date.parse(from)) / DAY_MS)
  return !Number.isNaN(length) && remaining > length
}

/**
 * The English the application writes is British in its dates: day, month in
 * words, year - the order Romanian uses, for readers who mostly live here.
 */
const LOCALE_FOR_DATES: Record<string, string> = {
  en: 'en-GB',
}

/**
 * A date as the reader reads it: "5 decembrie 2026", "5 December 2026". Falls
 * back to what arrived rather than throwing: a malformed date from the server is
 * a bad line on a screen, not a blank one.
 *
 * <p><strong>Changed on 2026-09-15, for two reasons found the same day.</strong>
 * `toLocaleDateString('en')` wrote US order, so "12/5/2026" meant 5 December and
 * read as 12 May to anybody who writes dates as Romania does. The month is now a
 * word, which no reader can take for a day. And the dates here are date-only
 * strings, which `Date` parses as UTC midnight: formatted in the device's own
 * timezone they showed the previous day anywhere west of Greenwich. They are
 * formatted in UTC, the zone they were parsed in.
 */
export function dateFormatter(language: string): (iso: string) => string {
  const format = new Intl.DateTimeFormat(LOCALE_FOR_DATES[language] ?? language, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })

  return (iso: string) => {
    const parsed = new Date(iso)
    return Number.isNaN(parsed.getTime()) ? iso : format.format(parsed)
  }
}