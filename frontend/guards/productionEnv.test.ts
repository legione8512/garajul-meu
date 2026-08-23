import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

/**
 * Guards the one file `.gitignore` was opened up to admit.
 *
 * <p>The blanket `.env.*` rule exists because a committed environment file is
 * the classic way a credential reaches a public repository. `.env.production`
 * is exempted because it holds a public URL, and the exemption is only safe
 * while that stays true - so this reads the file and checks.
 *
 * <p>The check that matters is the prefix. Vite inlines every `VITE_` value
 * into the bundle it serves to the browser, so such a value is public whether
 * or not it is committed here; a key without the prefix is not read by Vite at
 * all and can only have arrived by a misunderstanding. Both cases are worth
 * failing on, and the second is the dangerous one: it looks like configuration
 * and behaves like nothing.
 *
 * <p><strong>It lives in `guards/` rather than in `src/` because it reads the
 * filesystem.</strong> `src` is the browser application and `tsconfig.app.json`
 * types it as such - no Node types at all, so that nobody can reach for
 * `node:fs` inside a React component and find out at runtime. Admitting this
 * one test would have meant handing `process` and `Buffer` to every component
 * in the application. `guards/` is the Node side of the frontend, typed by
 * `tsconfig.e2e.json` alongside the Playwright suite, and collected by Vitest.
 */
describe('the committed production environment file', () => {
  // Split on \r?\n rather than \n, and it is not defensive habit.
  //
  // This file is written on Windows, so its lines end CRLF. Splitting on \n
  // alone leaves a trailing \r on every line - and in JavaScript `.` does not
  // match a line terminator while `$` without the `m` flag means the very end
  // of the string. So `#.*$` cannot reach past the \r to the end, fails to
  // match at all, and every comment line survives stripping intact. The first
  // version of this test read its own explanatory header as configuration.
  const settings = readFileSync('.env.production', 'utf8')
    .split(/\r?\n/)
    .map(line => line.replace(/#.*$/, '').trim())
    .filter(line => line.length > 0)

  const keys = settings.map(line => line.split('=')[0])

  it('names something, so an empty file cannot pass by default', () => {
    expect(keys).not.toHaveLength(0)
  })

  it('exposes only VITE_ keys, because only those are read and all of them are public', () => {
    expect(keys.filter(key => !key.startsWith('VITE_'))).toEqual([])
  })

  /**
   * Names, not values: a real credential is unrecognisable, but somebody
   * reaching for one almost always says so in the key. Catching
   * `VITE_RESEND_API_KEY` before it is committed is worth more than any attempt
   * to detect the shape of a secret.
   */
  it('carries no key that announces itself as a secret', () => {
    const suspicious = keys.filter(key =>
      /SECRET|PASSWORD|PRIVATE|CREDENTIAL|_KEY|TOKEN|DSN/.test(key),
    )

    expect(suspicious).toEqual([])
  })

  it('points the API at the production origin over https', () => {
    const apiBase = settings
      .find(line => line.startsWith('VITE_API_BASE_URL='))
      ?.slice('VITE_API_BASE_URL='.length)

    expect(apiBase).toBe('https://api.cyber-half.com')
  })
})