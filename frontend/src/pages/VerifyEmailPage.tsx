import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router'

import { resendVerification, verifyEmail } from '../api/endpoints/auth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, required, sixDigitCode } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { carriedEmail } from '../routes/carriedEmail.ts'
import { paths } from '../routes/paths.ts'

type Field = 'email' | 'code'

const rules = {
  email: [required, emailShape],
  code: [required, sixDigitCode],
}

/** Screen 3 in specification section 5. */
export function VerifyEmailPage() {
  const { t } = useTranslation()
  const location = useLocation()
  const navigate = useNavigate()

  const confirmation = useSubmission()
  // A second submission of its own, so a failed resend does not clear the
  // message about a wrong code, and vice versa.
  const resend = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({
    email: carriedEmail(location.state),
    code: '',
  })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})
  const [resent, setResent] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await confirmation.submit(async () => {
      await verifyEmail(values.email, values.code)
    })

    if (failure === null) {
      navigate(paths.login, { state: { email: values.email } })
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  async function handleResend() {
    setResent(false)

    const failure = await resend.submit(async () => {
      await resendVerification(values.email)
    })

    if (failure === null) {
      setResent(true)
    }
  }

  return (
    <>
      <h1>{t('screens.verifyEmail')}</h1>
      <p>{t('verifyEmail.instructions')}</p>

      <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={confirmation.error} />

        <TextField
          label={t('fields.email')}
          type="email"
          autoComplete="email"
          value={values.email}
          onChange={(email) => { setValues({ ...values, email }) }}
          message={messages.email}
        />

        <TextField
          label={t('fields.code')}
          autoComplete="one-time-code"
          inputMode="numeric"
          maxLength={6}
          value={values.code}
          onChange={(code) => { setValues({ ...values, code }) }}
          message={messages.code}
        />

        <button type="submit" disabled={confirmation.pending}>{t('verifyEmail.submit')}</button>
      </form>

      <FormError error={resend.error} />
      {resent ? <p role="status">{t('verifyEmail.resent')}</p> : null}

      <button type="button" disabled={resend.pending} onClick={() => { void handleResend() }}>
        {t('verifyEmail.resend')}
      </button>
    </>
  )
}