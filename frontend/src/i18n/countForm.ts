/**
 * The form a counted phrase needs in Romanian, as the name of the locale key
 * that holds it.
 *
 * <p>Romanian counts in three forms: "o zi", "5 zile", "20 de zile". The "de"
 * belongs to every number whose last two digits are 00 or 20 to 99 - "101
 * zile", but "120 de zile" and "200 de zile". That is CLDR's few/other boundary
 * for the language, and the same rule as the backend's `ReminderMessage.needsDe`,
 * so a reminder's title and the sentence under the document it is about never
 * disagree. Zero is "few" here, as in CLDR ("0 zile"); the backend never counts
 * zero, which it writes as "azi".
 *
 * <p>Chosen here rather than by i18next's own plurals for the reason
 * reminderState.ts records: `t` is typed over the literal keys of the locale,
 * and a plural's base key exists in none of them. English needs only one and
 * other, and gives "few" and "many" the same sentence.
 *
 * <p>Written on 2026-09-19, when the Play screenshots showed "Expiră în 1 zile"
 * and "Valabil încă 223 zile" - on every card, in every build since phase 10.
 */
export type CountForm = 'one' | 'few' | 'many'

export function countForm(count: number): CountForm {
  const whole = Math.abs(Math.trunc(count))

  if (whole === 1) {
    return 'one'
  }

  const lastTwo = whole % 100
  return whole !== 0 && (lastTwo === 0 || lastTwo >= 20) ? 'many' : 'few'
}
