import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'

import {
  isSupportedLanguage, languageNames, supportedLanguages, type SupportedLanguage,
} from '../i18n/language.ts'

/**
 * The flags, drawn rather than fetched.
 *
 * <p>Inline SVG because both alternatives are worse. An image file per language
 * is a request and a density problem; an emoji flag is not available at all -
 * measured on 2026-08-31, Windows renders the regional indicator pair as two
 * letters, so `🇷🇴` reads "RO". Ordinary emoji work there, flags specifically
 * have no font.
 *
 * <p>Square viewBoxes, rounded to a circle by the stylesheet. That is what a
 * circular flag set does: the flag is fitted to the circle rather than drawn at
 * its own proportions, which is why the Union Flag here is not 1:2.
 *
 * <p>`aria-hidden`, because the language's own name sits beside it and a reader
 * announcing both would say the same thing twice.
 *
 * <p>The Union Flag is simplified - its red saltire is centred rather than
 * counterchanged against the white. At twenty pixels the offset is under one
 * pixel, and the shape is what carries the recognition.
 */
function Flag({ language }: { language: SupportedLanguage }) {
  if (language === 'ro') {
    return (
      <svg data-flag viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <rect width="8" height="24" fill="#002B7F" />
        <rect x="8" width="8" height="24" fill="#FCD116" />
        <rect x="16" width="8" height="24" fill="#CE1126" />
      </svg>
    )
  }

  return (
    <svg data-flag viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect width="24" height="24" fill="#012169" />
      <path d="M0 0 24 24M24 0 0 24" stroke="#fff" strokeWidth="5" />
      <path d="M0 0 24 24M24 0 0 24" stroke="#C8102E" strokeWidth="2.5" />
      <path d="M12 0V24M0 12H24" stroke="#fff" strokeWidth="8" />
      <path d="M12 0V24M0 12H24" stroke="#C8102E" strokeWidth="4.5" />
    </svg>
  )
}

function Chevron() {
  return (
    <svg data-chevron viewBox="0 0 12 8" aria-hidden="true" focusable="false">
      <path d="M1 1.5 6 6.5 11 1.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

/**
 * Choosing a language, each named in itself and carrying its flag.
 *
 * <p><strong>Not a select, and the flag is why.</strong> An `option` element
 * holds text and nothing else - no image, no markup - so a dropdown built from
 * one could never show a picture. This is the shape the owner asked for on
 * 2026-08-31, after the native control had been ruled out by that limit.
 *
 * <p><strong>`details` rather than a hand-written menu.</strong> Opening,
 * closing, the keyboard and the expanded state a screen reader announces all
 * come from the browser. A custom popup would have to reimplement every one of
 * them, which is the same trade the certificate overlay refused when it left
 * panning to ordinary scrolling.
 *
 * <p>Two behaviours the element does not provide are added, because their
 * absence is what makes a menu feel broken: Escape closes it and returns focus
 * to the trigger, and a press anywhere else dismisses it.
 *
 * <p>The trigger's accessible name says what the control is as well as what it
 * currently shows - "Limbă: Română" - because the visible text alone announces
 * a language without ever saying it can be changed. The visible words are
 * contained in it, which is what WCAG 2.5.3 asks of a label that adds to them.
 */
export function LanguageSwitcher() {
  const { i18n, t } = useTranslation()
  const details = useRef<HTMLDetailsElement>(null)
  // i18next reports whatever it resolved to as a plain string, and that can be
  // a language this application does not have - the detector reads the browser,
  // which is not bounded by the list here. Narrowing rather than casting means
  // an unknown one falls back to Romanian, instead of indexing the name table
  // with a key that is not in it and drawing an empty trigger.
  const resolved = i18n.resolvedLanguage ?? null
  const current = isSupportedLanguage(resolved) ? resolved : 'ro'

  function close() {
    if (details.current !== null) {
      details.current.open = false
    }
  }

  useEffect(() => {
    function onPointerDown(event: PointerEvent) {
      if (details.current?.open === true
        && !details.current.contains(event.target as Node)) {
        close()
      }
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && details.current?.open === true) {
        close()
        details.current.querySelector('summary')?.focus()
      }
    }

    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)

    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [])

  return (
    <details data-language-switch ref={details}>
      <summary
        data-language-trigger
        aria-label={`${t('language.label')}: ${languageNames[current]}`}
      >
        <Flag language={current} />
        <span>{languageNames[current]}</span>
        <Chevron />
      </summary>

      <div data-language-menu>
        {supportedLanguages.map((language) => (
          <button
            key={language}
            data-language-option
            type="button"
            aria-current={language === current}
            onClick={() => {
              void i18n.changeLanguage(language)
              close()
            }}
          >
            <Flag language={language} />
            <span>{languageNames[language]}</span>
          </button>
        ))}
      </div>
    </details>
  )
}