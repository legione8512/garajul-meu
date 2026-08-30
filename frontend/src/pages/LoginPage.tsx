import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '../auth/useAuth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, maxLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { carriedEmail, returnTo } from '../routes/carriedEmail.ts'
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
  const location = useLocation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({
    email: carriedEmail(location.state),
    password: '',
  })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  // Where a protected route turned this person away from, if that is how they
  // arrived. Landing them on the dashboard instead would silently discard what
  // they actually asked for - a link to /garage would never open /garage.
  const destination = returnTo(location.state) ?? paths.dashboard

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
      // replace, so the back button does not return to a sign-in form for a
      // session that already exists.
      navigate(destination, { replace: true })
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.login')}</h1>

      <p><Link to={paths.welcome}>{t('common.backToStart')}</Link></p>

      {/*
        noValidate on purpose. The browser's own validation messages are written
        in the browser's language, not the application's, so leaving them on
        would put untranslated text beside translated text on the same form.
      */}
        <form data-panel onSubmit={(event) => { void handleSubmit(event) }} noValidate>
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
          label={t('fields.password')}
          type="password"
          autoComplete="current-password"
          value={values.password}
          onChange={(password) => { setValues({ ...values, password }) }}
          message={messages.password}
        />

        <button type="submit" disabled={pending}>{t('login.submit')}</button>
      </form>

      {/*
        One container rather than two paragraphs. Each paragraph carried its own
        margin and each link its own 44-pixel box, which pushed two related
        choices a third of a screen apart.
      */}
      <div data-form-links>
        <Link to={paths.forgotPassword}>{t('login.forgotPassword')}</Link>
        <Link to={paths.register}>{t('login.noAccount')}</Link>
      </div>
    </>
  )
}