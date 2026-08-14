import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router'

import { resetPassword } from '../api/endpoints/auth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, maxLength, minLength, required, sixDigitCode } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { carriedEmail } from '../routes/carriedEmail.ts'
import { paths } from '../routes/paths.ts'

type Field = 'email' | 'code' | 'newPassword'

/** The same bounds ResetPasswordRequest declares on the backend. */
const rules = {
  email: [required, emailShape],
  code: [required, sixDigitCode],
  newPassword: [required, minLength(12), maxLength(128)],
}

/** Screen 5 in specification section 5, second half. */
export function ResetPasswordPage() {
  const { t } = useTranslation()
  const location = useLocation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({
    email: carriedEmail(location.state),
    code: '',
    newPassword: '',
  })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submit(async () => {
      await resetPassword(values.email, values.code, values.newPassword)
    })

    if (failure === null) {
      // A successful reset also ends every session and verifies the address, so
      // signing in with the new password is the only thing left to do.
      navigate(paths.login, { state: { email: values.email } })
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.resetPassword')}</h1>
      <p>{t('resetPassword.instructions')}</p>

      <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={error} />

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

        <TextField
          label={t('fields.newPassword')}
          type="password"
          autoComplete="new-password"
          value={values.newPassword}
          onChange={(newPassword) => { setValues({ ...values, newPassword }) }}
          message={messages.newPassword}
        />

        <button type="submit" disabled={pending}>{t('resetPassword.submit')}</button>
      </form>
    </>
  )
}