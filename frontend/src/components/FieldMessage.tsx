import { useTranslation } from 'react-i18next'

import type { ValidationMessage } from '../forms/rules.ts'

/**
 * The id is not optional by accident: the input points at it with
 * aria-describedby, which is what makes a screen reader read the problem
 * together with the field rather than as loose text somewhere nearby.
 */
export function FieldMessage({ id, message }: { id: string; message: ValidationMessage | undefined }) {
  const { t } = useTranslation()

  if (message === undefined) {
    return null
  }

  return <p id={id}>{t(message.key, message.values)}</p>
}