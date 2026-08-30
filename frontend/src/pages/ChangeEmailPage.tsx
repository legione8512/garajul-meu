import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { confirmEmailChange, requestEmailChange } from '../api/endpoints/users.ts'
import { useAuth } from '../auth/useAuth.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { emailShape, maxLength, required, sixDigitCode } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type RequestField = 'newEmail' | 'currentPassword'

const requestRules = {
  newEmail: [required, emailShape, maxLength(320)],
  currentPassword: [required],
}

const codeRules = { code: [required, sixDigitCode] }

/**
 * Screen 17 in specification section 5, both halves on one screen.
 *
 * <p><strong>The code goes to the address currently on file, never to the new
 * one</strong>, and the screen says so before anything is typed. Somebody who
 * does not know that goes looking in the new inbox, finds nothing, and concludes
 * the feature is broken. It is also the property the design rests on: proving
 * control of the inbox that already owns the account is what makes a stolen
 * access token useless here.
 *
 * <p><strong>The session deliberately survives</strong>, unlike a password
 * change. Confirming moves the address and leaves it unverified, and if it was
 * mistyped the only route back is this same screen - which needs the person
 * still signed in.
 */
export function ChangeEmailPage() {
  const { t } = useTranslation()
  const { profileChanged } = useAuth()

  const request = useSubmission()
  const confirm = useSubmission()

  const [values, setValues] = useState<Record<RequestField, string>>({
    newEmail: '',
    currentPassword: '',
  })
  const [requestMessages, setRequestMessages] = useState<FieldMessages<RequestField>>({})

  const [code, setCode] = useState('')
  const [codeMessages, setCodeMessages] = useState<FieldMessages<'code'>>({})

  const [codeSent, setCodeSent] = useState(false)
  const [done, setDone] = useState(false)

  function change(field: RequestField, value: string) {
    setValues(previous => ({ ...previous, [field]: value }))
  }

  async function handleRequest(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, requestRules)
    setRequestMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await request.submit(async () => {
      await requestEmailChange(values.newEmail, values.currentPassword)
    })

    if (failure === null) {
      setCodeSent(true)
      return
    }

    setRequestMessages(fieldMessagesFrom(failure, values, requestRules))
  }

  async function handleConfirm(event: FormEvent) {
    event.preventDefault()

    const broken = validate({ code }, codeRules)
    setCodeMessages(broken)

    if (broken.code !== undefined) {
      return
    }

    let saved = null

    const failure = await confirm.submit(async () => {
      saved = await confirmEmailChange(code)
    })

    if (failure === null && saved !== null) {
      profileChanged(saved)
      setDone(true)
      return
    }

    if (failure !== null) {
      setCodeMessages(fieldMessagesFrom(failure, { code }, codeRules))
    }
  }

  return (
    <>
      <h1>{t('screens.changeEmail')}</h1>

      <p><Link to={paths.profile}>{t('profile.back')}</Link></p>

      {done
        ? <p role="status">{t('changeEmail.done')}</p>
        : (
          <>
            <p>{t('changeEmail.instructions')}</p>

            <form data-panel onSubmit={(event) => { void handleRequest(event) }} noValidate>
              <FormError error={request.error} />

              <TextField
                label={t('fields.newEmail')}
                type="email"
                autoComplete="email"
                value={values.newEmail}
                onChange={(value) => { change('newEmail', value) }}
                message={requestMessages.newEmail}
              />

              <TextField
                label={t('fields.currentPassword')}
                type="password"
                autoComplete="current-password"
                value={values.currentPassword}
                onChange={(value) => { change('currentPassword', value) }}
                message={requestMessages.currentPassword}
              />

              <button type="submit" disabled={request.pending}>
                {t('changeEmail.request')}
              </button>
            </form>

            {codeSent && (
              <form data-panel onSubmit={(event) => { void handleConfirm(event) }} noValidate>
                <p role="status">{t('changeEmail.codeSent')}</p>

                <FormError error={confirm.error} />

                <TextField
                  label={t('fields.code')}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  value={code}
                  onChange={(value) => { setCode(value) }}
                  message={codeMessages.code}
                />

                <button type="submit" disabled={confirm.pending}>
                  {t('changeEmail.confirm')}
                </button>
              </form>
            )}
          </>
          )}
    </>
  )
}