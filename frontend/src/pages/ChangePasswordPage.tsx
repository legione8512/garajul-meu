import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { changePassword } from '../api/endpoints/users.ts'
import { useAuth } from '../auth/useAuth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { maxLength, minLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type Field = 'currentPassword' | 'newPassword'

const rules = {
  currentPassword: [required],
  newPassword: [required, minLength(12), maxLength(128)],
}

/**
 * Screen 16 in specification section 5.
 *
 * <p><strong>Succeeding here ends the session, and the screen says so before the
 * form rather than after it.</strong> The backend revokes every refresh token
 * including the caller's own - section 14 requires that after a reset and the
 * same reasoning applies to a deliberate change: if the password is being
 * changed because somebody else learned it, their sessions must not outlive it.
 *
 * <p>So this signs out on success. The access token would in fact keep working
 * for the rest of its fifteen minutes while the refresh token behind it was
 * already dead - a session that looks alive and dies without explanation at the
 * next refresh. Ending it deliberately is the honest version of what has
 * happened. Sign-out navigates nowhere: the status becomes anonymous and
 * RequireAuth moves the person, with no hand-written redirect to race it.
 */
export function ChangePasswordPage() {
  const { t } = useTranslation()
  const { signOut } = useAuth()

  const submission = useSubmission()
  const [values, setValues] = useState<Record<Field, string>>({
    currentPassword: '',
    newPassword: '',
  })
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  function change(field: Field, value: string) {
    setValues(previous => ({ ...previous, [field]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submission.submit(async () => {
      await changePassword(values.currentPassword, values.newPassword)
    })

    if (failure === null) {
      await signOut()
      return
    }

    setMessages(fieldMessagesFrom(failure, values, rules))
  }

  return (
    <>
      <h1>{t('screens.changePassword')}</h1>

      <p><Link to={paths.profile}>{t('profile.back')}</Link></p>

      <p>{t('changePassword.warning')}</p>

      <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={submission.error} />

        <TextField
          label={t('fields.currentPassword')}
          type="password"
          autoComplete="current-password"
          value={values.currentPassword}
          onChange={(value) => { change('currentPassword', value) }}
          message={messages.currentPassword}
        />

        <TextField
          label={t('fields.newPassword')}
          type="password"
          autoComplete="new-password"
          value={values.newPassword}
          onChange={(value) => { change('newPassword', value) }}
          message={messages.newPassword}
        />

        <button type="submit" disabled={submission.pending}>{t('changePassword.submit')}</button>
      </form>
    </>
  )
}