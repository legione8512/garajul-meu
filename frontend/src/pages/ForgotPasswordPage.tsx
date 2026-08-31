import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'

import { forgotPassword } from '../api/endpoints/auth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, maxLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type Field = 'email'

const rules = {
  email: [required, emailShape, maxLength(320)],
}

/**
 * Screen 5 in specification section 5, first half.
 *
 * <p>The backend answers 204 whether or not the address holds an account, per
 * section 14: this endpoint needs no cooperation from the account holder, so a
 * truthful answer would be a free membership oracle for anyone with a list of
 * addresses. The wording here has to match that - "if an account exists" rather
 * than "we have sent you an email" - and moving on to the next screen regardless
 * is what keeps the two cases indistinguishable.
 */
export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({ email: '' })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submit(async () => {
      await forgotPassword(values.email)
    })

    if (failure === null) {
      navigate(paths.resetPassword, { state: { email: values.email } })
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.forgotPassword')}</h1>
      
      <p><Link to={paths.welcome}>{t('common.backToStart')}</Link></p>
      <p>{t('forgotPassword.instructions')}</p>

      <form data-panel onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={error} />

        <TextField
          label={t('fields.email')}
          type="email"
          autoComplete="email"
          value={values.email}
          onChange={(email) => { setValues({ email }) }}
          message={messages.email}
        />

        <button type="submit" disabled={pending}>{t('forgotPassword.submit')}</button>
      </form>

      <p><Link to={paths.login}>{t('forgotPassword.remembered')}</Link></p>
    </>
  )
}