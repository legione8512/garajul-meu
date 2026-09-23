import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import {
  paymentFrequencies, paymentKinds, paymentLeads, type NewPayment,
} from '../api/endpoints/payments.ts'
import { CheckboxField } from '../components/CheckboxField.tsx'
import { FormError } from '../components/FormError.tsx'
import { SelectField } from '../components/SelectField.tsx'
import { TextField } from '../components/TextField.tsx'
import type { ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import {
  leadKey, paymentBody, paymentRules, textValuesOf, type PaymentTextField, type PaymentValues,
} from './fields.ts'

interface PaymentFormProps {
  readonly heading: string
  readonly submitLabel: string
  readonly initial: PaymentValues
  /** Resolves when the backend has accepted the payment; throws what it refused. */
  readonly onSave: (payment: NewPayment) => Promise<void>
  /** Offered when the form corrects a stored payment, so the person can back out. */
  readonly onCancel?: () => void
}

/**
 * Adding and correcting a payment are the same form, as documents' period
 * fields are shared: what differs is the heading, the button, and whether a
 * way back is offered.
 *
 * <p>The end is a choice between two inputs rather than two inputs at once,
 * because the backend accepts exactly one of them - a form showing both would
 * invite the one combination it refuses.
 */
export function PaymentForm({ heading, submitLabel, initial, onSave, onCancel }: PaymentFormProps) {
  const { t } = useTranslation()
  const saving = useSubmission()

  const [values, setValues] = useState<PaymentValues>(initial)
  const [messages, setMessages] = useState<Partial<Record<PaymentTextField, ValidationMessage>>>({})

  function change(patch: Partial<PaymentValues>) {
    setValues(previous => ({ ...previous, ...patch }))
  }

  function toggleLead(lead: (typeof paymentLeads)[number], on: boolean) {
    setValues(previous => ({
      ...previous,
      leads: on
        ? [...previous.leads.filter(one => one !== lead), lead]
        : previous.leads.filter(one => one !== lead),
    }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(textValuesOf(values), paymentRules(values))
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await saving.submit(async () => {
      await onSave(paymentBody(values))
    })

    if (failure === null) {
      setValues(initial)
    }
  }

  return (
    <form data-card onSubmit={(event) => { void handleSubmit(event) }} noValidate>
      <h2>{heading}</h2>

      <FormError error={saving.error} />

      <SelectField
        label={t('payments.fields.kind')}
        value={values.kind}
        options={paymentKinds.map(one => ({ value: one, label: t(`payments.kind.${one}`) }))}
        onChange={(value) => { change({ kind: value }) }}
      />

      <TextField
        label={t('payments.fields.firstDueDate')}
        type="date"
        value={values.firstDueDate}
        onChange={(value) => { change({ firstDueDate: value }) }}
        message={messages.firstDueDate}
      />

      <SelectField
        label={t('payments.fields.frequency')}
        value={values.frequency}
        options={paymentFrequencies.map(one => ({
          value: one, label: t(`payments.frequency.${one}`),
        }))}
        onChange={(value) => { change({ frequency: value }) }}
      />

      <SelectField
        label={t('payments.fields.endBy')}
        value={values.endBy}
        options={[
          { value: 'count', label: t('payments.endBy.count') },
          { value: 'date', label: t('payments.endBy.date') },
        ]}
        onChange={(value) => { change({ endBy: value === 'date' ? 'date' : 'count' }) }}
      />

      {values.endBy === 'date'
        ? (
          <TextField
            label={t('payments.fields.lastDueDate')}
            type="date"
            value={values.lastDueDate}
            onChange={(value) => { change({ lastDueDate: value }) }}
            message={messages.lastDueDate}
          />
          )
        : (
          <TextField
            label={t('payments.fields.instalmentCount')}
            inputMode="numeric"
            maxLength={3}
            value={values.instalmentCount}
            onChange={(value) => { change({ instalmentCount: value }) }}
            message={messages.instalmentCount}
          />
          )}

      <fieldset>
        <legend>{t('payments.fields.leads')}</legend>

        {paymentLeads.map(lead => (
          <CheckboxField
            key={lead}
            label={t(leadKey(lead))}
            checked={values.leads.includes(lead)}
            onChange={(on) => { toggleLead(lead, on) }}
          />
        ))}
      </fieldset>

      <p data-actions>
        <button type="submit" disabled={saving.pending}>{submitLabel}</button>
        {onCancel !== undefined && (
          <button data-quiet type="button" onClick={onCancel}>{t('payments.cancel')}</button>
        )}
      </p>
    </form>
  )
}
