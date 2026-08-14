import { useId } from 'react'
import { useTranslation } from 'react-i18next'

import { languageNames, supportedLanguages } from '../i18n/language.ts'

/**
 * The label is translated; the options are not - each language is named in
 * itself. useId rather than a fixed id so this cannot collide if the shell ever
 * renders it twice, and htmlFor rather than a wrapping label so the accessible
 * name is unambiguous. That matters more than usual while
 * eslint-plugin-jsx-a11y is unavailable: nothing else is checking.
 */
export function LanguageSwitcher() {
  const { i18n, t } = useTranslation()
  const selectId = useId()

  return (
    <>
      <label htmlFor={selectId}>{t('language.label')}</label>
      <select
        id={selectId}
        value={i18n.resolvedLanguage ?? 'ro'}
        onChange={(event) => {
          void i18n.changeLanguage(event.target.value)
        }}
      >
        {supportedLanguages.map((language) => (
          <option key={language} value={language}>
            {languageNames[language]}
          </option>
        ))}
      </select>
    </>
  )
}