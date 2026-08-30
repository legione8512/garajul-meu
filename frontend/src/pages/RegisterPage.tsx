import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'

import { register } from '../api/endpoints/auth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, maxLength, minLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type Field = 'fullName' | 'email' | 'password'

/** The same bounds RegisterRequest declares on the backend. */
const rules = {
  fullName: [required, maxLength(120)],
  email: [required, emailShape, maxLength(320)],
  password: [required, minLength(12), maxLength(128)],
}

/** Screen 2 in specification section 5. */
export function RegisterPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>({ fullName: '', email: '', password: '' })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submit(async () => {
      // The language is not a form field. Whatever the switcher currently shows
      // is what the account is created with - the obvious guess, and one the
      // person can already see and change before submitting.
      await register(values.fullName, values.email, values.password, i18n.resolvedLanguage ?? 'ro')
    })

    if (failure === null) {
      // The address travels in route state, not in the URL. The next screen
      // still shows it as an editable field, so a reload costs nothing but
      // typing it again.
      navigate(paths.verifyEmail, { state: { email: values.email } })
      return
    }

    if (failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.register')}</h1>

      <p><Link to={paths.welcome}>{t('common.backToStart')}</Link></p>

      <form data-panel onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={error} />

        <TextField
          label={t('fields.fullName')}
          autoComplete="name"
          value={values.fullName}
          onChange={(fullName) => { setValues({ ...values, fullName }) }}
          message={messages.fullName}
        />

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
          autoComplete="new-password"
          value={values.password}
          onChange={(password) => { setValues({ ...values, password }) }}
          message={messages.password}
        />

        <button type="submit" disabled={pending}>{t('register.submit')}</button>
      </form>

      <div data-form-links>
        <Link to={paths.login}>{t('register.haveAccount')}</Link>
      </div>
    </>
  )
}