import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import {
  documentTypes, historyPath, type DocumentDetails,
} from '../api/endpoints/documents.ts'
import type { PageResponse } from '../api/page.ts'
import { useResource } from '../api/useResource.ts'
import { SelectField } from '../components/SelectField.tsx'
import { dateFormatter, stateOf } from '../documents/status.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 14 in specification section 5.
 *
 * <p>The history is the records themselves: section 1 asks for "document renewal
 * history without overwriting previous records", and the backend keeps no event
 * log because the not-overwriting *is* the history. Ordered by when each was
 * entered, which is what section 10.4 annotates `created_at` for.
 *
 * <p>The filter and the page are held here and folded into the path, because
 * `useResource` keys on the path string - so changing either reloads the screen
 * with no further wiring. They are deliberately not in the URL: that would make
 * a page shareable and the browser's back button useful, and is worth doing when
 * anything asks for it rather than before.
 *
 * <p>Where you are and how much there is are **two paragraphs, not one**. Two
 * interpolated sentences in a single element read as one run of text to anything
 * matching on content - a screen reader announces them as one thought, and a
 * test cannot match either alone.
 */
export function VehicleHistoryPage() {
  const { t, i18n } = useTranslation()
  const { vehicleId = '' } = useParams()

  const [type, setType] = useState('')
  const [page, setPage] = useState(0)

  const { data, error, loading } = useResource<PageResponse<DocumentDetails>>(
    historyPath(vehicleId, { type, page }),
  )

  const formatDate = dateFormatter(i18n.language)

  return (
    <>
      <h1>{t('history.title')}</h1>

      <p><Link to={paths.documents(vehicleId)}>{t('documents.backToList')}</Link></p>

      <SelectField
        label={t('history.filter')}
        value={type}
        options={[
          { value: '', label: t('history.allTypes') },
          ...documentTypes.map(one => ({ value: one, label: t(`documents.type.${one}`) })),
        ]}
        onChange={(value) => {
          // Back to the first page: page three of the old filter is very likely
          // past the end of the new one, and an empty screen would read as "no
          // history" rather than "no such page".
          setPage(0)
          setType(value)
        }}
      />

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.items.length === 0 && <p>{t('history.none')}</p>}

      {data !== null && data.items.length > 0 && (
        <>
          <ul>
            {data.items.map((document) => {
              const state = stateOf(document, formatDate)

              return (
                <li data-card key={document.id}>
                  <h2>
                    <Link to={paths.document(vehicleId, document.id)}>
                      {t(`documents.type.${document.type}`)}
                    </Link>
                  </h2>

                  <p data-subtitle>
                    {document.validFrom == null
                      ? t('documents.period', { until: formatDate(document.validUntil) })
                      : t('documents.periodFrom', {
                          from: formatDate(document.validFrom),
                          until: formatDate(document.validUntil),
                        })}
                  </p>

                  <p data-tone={state.tone}>{t(state.key, state.values)}</p>
                </li>
              )
            })}
          </ul>

          {/*
            One row rather than four stacked blocks. The position and the total
            answer the same question - where am I in how much - and the two
            controls that change it belong beside the answer, not underneath it.
          */}
          <div data-pagination>
            <button
              data-quiet
              type="button"
              disabled={data.page === 0}
              onClick={() => { setPage(previous => previous - 1) }}
            >
              {t('history.previous')}
            </button>

          <button
            type="button"
            data-quiet
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => { setPage(previous => previous + 1) }}
          >
            {t('history.next')}
          </button>
            <p data-subtitle>
              {t('history.page', { page: data.page + 1, pages: data.totalPages })}
            </p>
            <p data-subtitle>
              {t('history.total', { total: data.totalElements })}
            </p>
          </div>
        </>
      )}
    </>
  )
}