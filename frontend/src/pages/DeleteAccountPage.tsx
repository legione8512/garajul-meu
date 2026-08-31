import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { deleteAccount } from '../api/endpoints/users.ts'
import { useAuth } from '../auth/useAuth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

const rules = { currentPassword: [required] }

/**
 * Screen 22 in specification section 5.
 *
 * <p>Two deliberate obstacles, and they guard different things. The password is
 * the backend's: an access token stolen for fifteen minutes must not be enough
 * to destroy an account no support process can restore. The confirmation step is
 * this screen's, against the person's own hand - the same in-place confirmation
 * the vehicle and the photograph use, for the same reason window.confirm is
 * never used here.
 *
 * <p>Signing out afterwards is not tidying up. The rows behind the session no
 * longer exist, so the status has to be corrected before anything tries to use
 * them; RequireAuth then moves the person, with nothing hand-written to race it.
 */
export function DeleteAccountPage() {
  const { t } = useTranslation()
  const { signOut } = useAuth()

  const submission = useSubmission()
  const [currentPassword, setCurrentPassword] = useState('')
  const [messages, setMessages] = useState<FieldMessages<'currentPassword'>>({})
  const [confirming, setConfirming] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate({ currentPassword }, rules)
    setMessages(broken)

    if (broken.currentPassword !== undefined) {
      return
    }

    setConfirming(true)
  }

  async function handleConfirm() {
    const failure = await submission.submit(async () => {
      await deleteAccount(currentPassword)
    })

    if (failure === null) {
      await signOut()
      return
    }

    setConfirming(false)
  }

  return (
    <>
      <h1>{t('screens.deleteAccount')}</h1>

      <p><Link to={paths.profile}>{t('profile.back')}</Link></p>

      <p>{t('deleteAccount.warning')}</p>

      <form data-panel onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={submission.error} />

        <TextField
          label={t('fields.currentPassword')}
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(value) => { setCurrentPassword(value) }}
          message={messages.currentPassword}
        />

        {/*
          The most irreversible action in the application, and until now the one
          painted most invitingly. Both buttons carry `data-destructive`, the
          first as well as the confirmation: this screen exists to do one thing,
          and there is no competing primary action for the brand colour to mean.
          The warning above says what goes; the colour should not disagree with it.
        */}
        {confirming
          ? (
            <div>
              <p>{t('deleteAccount.confirm')}</p>
              <p data-actions>
                <button
                  data-destructive
                  type="button"
                  onClick={() => { void handleConfirm() }}
                  disabled={submission.pending}
                >
                  {t('deleteAccount.submit')}
                </button>
                <button data-quiet type="button" onClick={() => { setConfirming(false) }}>
                  {t('deleteAccount.cancel')}
                </button>
              </p>
            </div>
            )
          : <button data-destructive type="submit">{t('deleteAccount.submit')}</button>}
      </form>
    </>
  )
}