import { describe, expect, it } from 'vitest'

import { cookieSessionChannel } from './cookieChannel.ts'
import { nativeSessionChannel } from './nativeChannel.ts'
import { unchosenSecureStore, type SecureStore } from './secureStore.ts'

/** A store that remembers, so the channel's own behaviour is what is measured. */
function fakeStore(initial: string | null = null): SecureStore & { held: string | null } {
  return {
    held: initial,
    read() { return Promise.resolve(this.held) },
    write(value: string) { this.held = value; return Promise.resolve() },
    clear() { this.held = null; return Promise.resolve() },
  }
}

describe('the cookie channel', () => {
  it('does not carry the token itself, so login never asks for one in the body', () => {
    expect(cookieSessionChannel.carriesTokenItself).toBe(false)
  })

  /**
   * The assertion that keeps this whole change invisible on the web: a null here
   * is what leaves the refresh body as `{}`, which is what makes the backend
   * read the cookie.
   */
  it('presents nothing, because the cookie is what carries it', async () => {
    await expect(cookieSessionChannel.present()).resolves.toBeNull()
  })

  it('remembers and forgets without doing anything at all', async () => {
    await expect(cookieSessionChannel.remember(null)).resolves.toBeUndefined()
    await expect(cookieSessionChannel.remember('ignored')).resolves.toBeUndefined()
    await expect(cookieSessionChannel.forget()).resolves.toBeUndefined()
  })
})

describe('the native channel', () => {
  it('carries the token itself, which is what login has to declare', () => {
    expect(nativeSessionChannel(fakeStore()).carriesTokenItself).toBe(true)
  })

  it('presents what the device is holding', async () => {
    const channel = nativeSessionChannel(fakeStore('held-on-device'))

    await expect(channel.present()).resolves.toBe('held-on-device')
  })

  it('writes the token it is handed', async () => {
    const store = fakeStore('old')

    await nativeSessionChannel(store).remember('rotated')

    expect(store.held).toBe('rotated')
  })

  /**
   * The contract-drift case, and the reason it throws rather than shrugging: a
   * native client that accepted a null would hold a session it can never renew,
   * which looks exactly like a successful sign-in until the access token expires
   * ten minutes later and cannot be replaced.
   */
  it('refuses a null token rather than holding a session it can never renew', async () => {
    const store = fakeStore('old')

    await expect(nativeSessionChannel(store).remember(null)).rejects.toThrow(/refresh token/i)
    expect(store.held).toBe('old')
  })

  it('clears the device when the session ends', async () => {
    const store = fakeStore('spent')

    await nativeSessionChannel(store).forget()

    expect(store.held).toBeNull()
  })
})

describe('the unchosen secure store', () => {
  it('refuses every operation rather than inventing a place to put a credential', async () => {
    await expect(unchosenSecureStore.read()).rejects.toThrow()
    await expect(unchosenSecureStore.write('anything')).rejects.toThrow()
    await expect(unchosenSecureStore.clear()).rejects.toThrow()
  })

  /**
   * Names, not just refuses. A stub that fails anonymously sends whoever meets
   * it hunting through the code; this one says which decision has not been taken.
   */
  it('names the deferred decision in the failure', async () => {
    await expect(unchosenSecureStore.read()).rejects.toThrow(/section 35/)
  })
})
