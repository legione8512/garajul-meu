import { X509Certificate } from 'node:crypto'
import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

/**
 * Guards the one `.pem` file `.gitignore` was opened up to admit.
 *
 * <p>The blanket `*.pem` rule exists because a PEM file is the usual shape of a
 * private key, and a committed private key in a public repository is the kind
 * of mistake that cannot be taken back. One file is exempted, on 2026-09-19:
 * ISRG Root X1, Let's Encrypt's public root, bundled so that Android 7.0 - which
 * predates it - can reach the API at all (`network_security_config.xml` says
 * why). A root certificate is published by its owner and carried by every
 * browser, so committing it discloses nothing. The exemption is only safe while
 * the file holds that and nothing else, so this reads it and checks.
 *
 * <p>**Found by `git status`, not by the build.** The file was delivered, applied
 * and byte-identical on disk, and `git status` did not list it, because
 * `*.pem` had quietly ignored it. Committed like that, the fix would have lived
 * on one laptop only: every clean clone would have lost the certificate and
 * failed to build, and the repository would have claimed to support Android
 * 7.0 while holding nothing that made it true.
 *
 * <p>Same shape as `committedEnv.test.ts`, the guard for the other files
 * `.gitignore` admits, and in `guards/` for the same reason: it reads the
 * filesystem, and `src` is typed as the browser.
 */
const TRUST_ANCHOR = 'android/app/src/main/res/raw/isrg_root_x1.pem'

/** As Let's Encrypt publishes it, and as `openssl` read it on 2026-09-19. */
const ISRG_ROOT_X1_SHA256
  = '96:BC:EC:06:26:49:76:F3:74:60:77:9A:CF:28:C5:A7:CF:E8:A3:C0:AA:E1:1A:8F:FC:EE:05:C0:BD:DF:08:C6'

describe('the root certificate committed for Android 7.0', () => {
  const pem = readFileSync(TRUST_ANCHOR, 'utf8')

  /**
   * The assertion the exemption depends on. Every private key format - PKCS#8,
   * PKCS#1, EC, encrypted - opens with a `-----BEGIN ... -----` line of its own,
   * so demanding exactly one such line, and that it announce a certificate,
   * leaves no room for a key beside it or instead of it.
   */
  it('holds one certificate and no key of any kind', () => {
    expect(pem.match(/-----BEGIN [^-]+-----/g)).toEqual(['-----BEGIN CERTIFICATE-----'])
  })

  /**
   * Pinned by fingerprint rather than by name, because a name is the one thing
   * anybody can put in a certificate. A corrupted file, a different root, or
   * a certificate that merely calls itself ISRG Root X1 all fail here.
   */
  it('is ISRG Root X1 itself, by the fingerprint Let\'s Encrypt publishes', () => {
    const certificate = new X509Certificate(pem)

    expect(certificate.fingerprint256).toBe(ISRG_ROOT_X1_SHA256)
    expect(certificate.subject).toContain('CN=ISRG Root X1')
  })
})
