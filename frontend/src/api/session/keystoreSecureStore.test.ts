import { beforeEach, describe, expect, it, vi } from 'vitest'

const plugin = vi.hoisted(() => ({
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}))

vi.mock('@aparajita/capacitor-secure-storage', () => ({ SecureStorage: plugin }))

const { keystoreSecureStore } = await import('./keystoreSecureStore.ts')

/**
 * Three assertions about a mapping, which is all this file is - and the mapping
 * is where it can go wrong, because every wrong choice here still compiles.
 *
 * <p>The plugin cannot be exercised for real anywhere a test runs: its native
 * halves are Kotlin and Swift, and its web half is the `localStorage` shim this
 * application exists to keep out of reach. What is left to check is that the
 * right three methods are called with the right arguments, which is exactly the
 * part a refactor or an autocomplete can quietly change.
 */
describe('the keystore-backed secure store', () => {
  beforeEach(() => {
    plugin.getItem.mockResolvedValue(null)
    plugin.setItem.mockResolvedValue(undefined)
    plugin.removeItem.mockResolvedValue(undefined)
  })

  it('reads and writes the token under one key, unchanged in either direction', async () => {
    plugin.getItem.mockResolvedValue('a-refresh-token')

    await expect(keystoreSecureStore.read()).resolves.toBe('a-refresh-token')
    expect(plugin.getItem).toHaveBeenCalledWith('garajul-meu.refresh-token')

    await keystoreSecureStore.write('a-refresh-token')
    expect(plugin.setItem).toHaveBeenCalledWith('garajul-meu.refresh-token', 'a-refresh-token')
  })

  /** A fresh install and a signed-out one look the same, and both are ordinary. */
  it('reports no token rather than failing when nothing has been stored', async () => {
    await expect(keystoreSecureStore.read()).resolves.toBeNull()
  })

  /**
   * The assertion with a reason. The plugin also offers `clear()`, which removes
   * every entry sharing the key prefix - it works today, when the token is the
   * only thing stored, and silently widens the moment anything else is. Ending a
   * session must remove the session.
   */
  it('forgets the token by removing it, never by clearing the store', async () => {
    await keystoreSecureStore.clear()

    expect(plugin.removeItem).toHaveBeenCalledWith('garajul-meu.refresh-token')
    expect(plugin.clear).not.toHaveBeenCalled()
  })
})