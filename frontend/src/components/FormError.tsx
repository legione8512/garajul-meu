import { useTranslation } from 'react-i18next'

import type { ApiError } from '../api/ApiError.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'

/** Failures the reader can do nothing about except quote the reference. */
const OPAQUE_CODES = new Set(['UNKNOWN', 'INTERNAL_ERROR'])

/**
 * role="alert" so the message is announced when it appears. A visually obvious
 * red box that a screen reader never mentions is not an error message.
 */
export function FormError({ error }: { error: ApiError | null }) {
  const { t } = useTranslation()

  if (error === null) {
    return null
  }

  const showReference = OPAQUE_CODES.has(error.code) && error.requestId !== null

  return (
    <p role="alert">
      {t(errorMessageKey(error.code))}
      {showReference ? ` ${t('common.reference', { requestId: error.requestId })}` : null}
    </p>
  )
}