import { useTranslation } from 'react-i18next'

import { TextField } from '../components/TextField.tsx'
import type { ValidationMessage } from '../forms/rules.ts'
import type { PeriodField, PeriodValues } from './fields.ts'

interface DocumentFieldsProps {
  readonly values: PeriodValues
  readonly messages: Partial<Record<PeriodField, ValidationMessage>>
  readonly onChange: (field: PeriodField, value: string) => void
}

/**
 * The five inputs a period needs, in one place because three forms need them -
 * adding on screen 11, correcting and renewing on screen 13. The same reasoning
 * the backend applies with `DocumentPeriod`: what differs between the three is
 * the type, and everything else stated three times is three chances to drift.
 */
export function DocumentFields({ values, messages, onChange }: DocumentFieldsProps) {
  const { t } = useTranslation()

  return (
    <>
      <TextField
        label={t('documents.fields.validFrom')}
        type="date"
        value={values.validFrom}
        onChange={(value) => { onChange('validFrom', value) }}
        message={messages.validFrom}
      />

      <TextField
        label={t('documents.fields.validUntil')}
        type="date"
        value={values.validUntil}
        onChange={(value) => { onChange('validUntil', value) }}
        message={messages.validUntil}
      />

      <TextField
        label={t('documents.fields.provider')}
        value={values.provider}
        maxLength={160}
        onChange={(value) => { onChange('provider', value) }}
        message={messages.provider}
      />

      <TextField
        label={t('documents.fields.referenceNumber')}
        value={values.referenceNumber}
        maxLength={64}
        onChange={(value) => { onChange('referenceNumber', value) }}
        message={messages.referenceNumber}
      />

      <TextField
        label={t('documents.fields.notes')}
        multiline
        value={values.notes}
        onChange={(value) => { onChange('notes', value) }}
        message={messages.notes}
      />
    </>
  )
}