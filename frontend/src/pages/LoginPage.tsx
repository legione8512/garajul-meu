import { useId, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'

import { useAuth } from '../auth/useAuth.ts'
import { FieldMessage } from '../components/FieldMessage.tsx'
import { FormError } from '../components/FormError.tsx'
import { emailShape, maxLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type Field = 'email' | 'password'

/**
 * No minimum length on the password, deliberately, and for the same reason
 * LoginRequest on the backend carries none: a length policy applies to new
 * passwords. Enforcing today's minimum at sign-in would refuse exactly the
 * people whose password predates it.
 */
const rules = {
  email: [required, emailShape, maxLength(320)],
  password: [required],
}

/** Screen 4 in specification section 5. */
export function LoginPage() {
  const { t } = useTranslation()
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({ email: '', password: '' })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  const emailId = useId()
  const passwordId = useId()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submit(async () => {
      await signIn(values.email, values.password)
    })

    if (failure === null) {
      navigate(paths.welcome)
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.login')}</h1>

      {/*
        noValidate on purpose. The browser's own validation messages are written
        in the browser's language, not the application's, so leaving them on
        would put untranslated text beside translated text on the same form.
      */}
      <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={error} />

        {/*
          div rather than p for the field group: FieldMessage renders a
          paragraph, and a paragraph cannot contain another. The browser closes
          the outer one on the spot, so the rendered structure stops matching
          the source - which jsdom tolerates and a real browser does not.
        */}
        <div>
          <label htmlFor={emailId}>{t('fields.email')}</label>
          <input
            id={emailId}
            type="email"
            autoComplete="email"
            value={values.email}
            onChange={(event) => { setValues({ ...values, email: event.target.value }) }}
            aria-invalid={messages.email !== undefined}
            aria-describedby={messages.email === undefined ? undefined : `${emailId}-message`}
          />
          <FieldMessage id={`${emailId}-message`} message={messages.email} />
        </div>

        <div>
          <label htmlFor={passwordId}>{t('fields.password')}</label>
          <input
            id={passwordId}
            type="password"
            autoComplete="current-password"
            value={values.password}
            onChange={(event) => { setValues({ ...values, password: event.target.value }) }}
            aria-invalid={messages.password !== undefined}
            aria-describedby={messages.password === undefined ? undefined : `${passwordId}-message`}
          />
          <FieldMessage id={`${passwordId}-message`} message={messages.password} />
        </div>

        <button type="submit" disabled={pending}>{t('login.submit')}</button>
      </form>

      <p><Link to={paths.forgotPassword}>{t('login.forgotPassword')}</Link></p>
      <p><Link to={paths.register}>{t('login.noAccount')}</Link></p>
    </>
  )
}