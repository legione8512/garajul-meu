import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * A plain object with four spies would not have caught the defect that reached
 * an iPhone on 2026-09-04, so this one carries the trap that did.
 *
 * <p>A real Capacitor plugin is a Proxy: every property access becomes a native
 * call. That makes it look *thenable* to the promise machinery, so an `async`
 * function which **returns** it makes the runtime ask for `.then` - and the
 * proxy forwards that to the platform as a method named `then`, which nothing
 * implements. Neither callback is ever invoked and the promise never settles.
 * Not a crash: a hang, presenting as an application that quietly does nothing.
 *
 * <p>`then` here throws instead of hanging, deliberately. The real failure is a
 * promise that never resolves, which in a test is a fifteen-second timeout and a
 * message about time rather than about cause. Throwing rejects the same promise
 * immediately and names the mistake. Any of the three seams handing the plugin
 * object onward fails on this line.
 */
const plugin = vi.hoisted(() => ({
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  then: () => {
    throw new Error(
      'The plugin object was used as the resolution value of a promise. A Capacitor '
      + 'proxy is thenable-looking, so this hangs for ever on a device: import it '
      + 'inside the method that uses it and never return it.',
    )
  },
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