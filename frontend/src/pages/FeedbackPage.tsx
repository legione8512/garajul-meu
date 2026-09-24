import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import {
  FEEDBACK_MAX_LENGTH, feedbackCategories, sendFeedback,
} from '../api/endpoints/feedback.ts'
import { FormError } from '../components/FormError.tsx'
import { SelectField } from '../components/SelectField.tsx'
import { TextField } from '../components/TextField.tsx'
import { clientInfo } from '../feedback/clientInfo.ts'
import { maxLength, required, type ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'

const rules = { message: [required, maxLength(FEEDBACK_MAX_LENGTH)] }

/**
 * The Sugestii tab, 1.1, at the owner's request: an idea, a problem or a
 * wished-for function, sent to the operator. The reply comes by email, to the
 * account's own address, which the screen says before anybody writes.
 *
 * <p>The thanks replaces nothing: the form stays, emptied, so a second message
 * is one more tap - and the thanks is a status, so a screen reader hears it.
 */
export function FeedbackPage() {
  const { t } = useTranslation()
  const sending = useSubmission()

  const [category, setCategory] = useState<string>('IDEA')
  const [message, setMessage] = useState('')
  const [problem, setProblem] = useState<ValidationMessage | undefined>(undefined)
  const [sent, setSent] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSent(false)

    const broken = validate({ message }, rules)
    setProblem(broken.message)

    if (broken.message !== undefined) {
      return
    }

    const failure = await sending.submit(async () => {
      const client = await clientInfo()
      await sendFeedback({ category, message: message.trim(), ...client })
    })

    if (failure === null) {
      setMessage('')
      setCategory('IDEA')
      setSent(true)
    }
  }

  return (
    <>
      <h1>{t('screens.feedback')}</h1>

      <p>{t('feedback.intro')}</p>

      <form data-card onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={sending.error} />

        {sent && <p role="status" data-tone="ok">{t('feedback.thanks')}</p>}

        <SelectField
          label={t('feedback.category')}
          value={category}
          options={feedbackCategories.map(one => ({ value: one, label: t(`feedback.categories.${one}`) }))}
          onChange={(value) => { setCategory(value) }}
        />

        <TextField
          label={t('feedback.message')}
          multiline
          maxLength={FEEDBACK_MAX_LENGTH}
          value={message}
          onChange={(value) => { setMessage(value); setSent(false) }}
          message={problem}
        />

        <button type="submit" disabled={sending.pending}>{t('feedback.send')}</button>
      </form>
    </>
  )
}
