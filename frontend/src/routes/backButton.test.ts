import { beforeEach, describe, expect, it, vi } from 'vitest'

type BackButtonListener = (event: { canGoBack: boolean }) => void

/**
 * The plugin, with the `then` trap every native seam's test carries - see
 * `keystoreSecureStore.test.ts` for the hang it turns into a red test. This file
 * returns nothing it imports, so the trap only matters to a future refactor that
 * does.
 */
const plugin = vi.hoisted(() => ({
  addListener: vi.fn<(event: string, listener: BackButtonListener) => Promise<{ remove: () => Promise<void> }>>(),
  minimizeApp: vi.fn<() => Promise<void>>(),
  exitApp: vi.fn<() => Promise<void>>(),
  then: () => {
    throw new Error(
      'The plugin object was used as the resolution value of a promise. A Capacitor '
      + 'proxy is thenable-looking, so this hangs for ever on a device: import it '
      + 'inside the function that uses it and never return it.',
    )
  },
}))

const platform = vi.hoisted(() => ({ current: 'android' }))

vi.mock('@capacitor/app', () => ({ App: plugin }))
vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => platform.current } }))

const { handleBackButton } = await import('./backButton.ts')

/** Registers the handler and hands back what it registered, so a test can press Back. */
async function pressBack(): Promise<BackButtonListener> {
  await handleBackButton()

  const call = plugin.addListener.mock.calls.at(0)

  if (call === undefined) {
    throw new Error('handleBackButton registered no listener')
  }

  const [event, listener] = call
  expect(event).toBe('backButton')
  return listener
}

describe('the Android Back button', () => {
  beforeEach(() => {
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`, so
    // their call history is cleared here or it carries into the next test.
    plugin.addListener.mockReset()
    plugin.addListener.mockResolvedValue({ remove: () => Promise.resolve() })
    plugin.minimizeApp.mockReset()
    plugin.minimizeApp.mockResolvedValue(undefined)
    plugin.exitApp.mockReset()
    platform.current = 'android'
  })

  it('goes back a screen while there is one to go back to', async () => {
    const back = vi.spyOn(window.history, 'back').mockImplementation(() => undefined)
    const press = await pressBack()

    press({ canGoBack: true })

    expect(back).toHaveBeenCalledOnce()
    expect(plugin.minimizeApp).not.toHaveBeenCalled()
  })

  /**
   * The press `@capacitor/app` would have swallowed. Without a listener its
   * callback does nothing when the WebView cannot go back, so on the first
   * screen Back would have stopped working entirely - the opposite of the defect
   * this file exists for, and just as wrong.
   *
   * <p>Minimised, never finished: `exitApp` is `finish()`, which Android stopped
   * doing to a root activity in version 12, and which would cost the next visit a
   * cold start.
   */
  it('leaves the application from the first screen, by minimising it', async () => {
    const back = vi.spyOn(window.history, 'back').mockImplementation(() => undefined)
    const press = await pressBack()

    press({ canGoBack: false })

    expect(plugin.minimizeApp).toHaveBeenCalledOnce()
    expect(plugin.exitApp).not.toHaveBeenCalled()
    expect(back).not.toHaveBeenCalled()
  })

  /**
   * iOS never fires the event, and a build made before `cap sync ios` would not
   * have the plugin at all - registering there could only fail at startup.
   */
  it('registers nothing on iOS, which has no Back button', async () => {
    platform.current = 'ios'

    await handleBackButton()

    expect(plugin.addListener).not.toHaveBeenCalled()
  })
})
