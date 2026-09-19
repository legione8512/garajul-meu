import { beforeEach, describe, expect, it, vi } from 'vitest'

interface Status { connected: boolean, connectionType: string }

type StatusListener = (status: Status) => void

/**
 * The `then` trap every native seam's test carries - see
 * `keystoreSecureStore.test.ts` for the hang it turns into a red test.
 */
function trap(): never {
  throw new Error(
    'The plugin object was used as the resolution value of a promise. A Capacitor '
    + 'proxy is thenable-looking, so this hangs for ever on a device: import it '
    + 'inside the function that uses it and never return it.',
  )
}

const network = vi.hoisted(() => ({
  getStatus: vi.fn<() => Promise<Status>>(),
  addListener: vi.fn<(event: string, listener: StatusListener) => Promise<{ remove: () => Promise<void> }>>(),
  then: () => { trap() },
}))

const platform = vi.hoisted(() => ({ current: 'android' }))

vi.mock('@capacitor/network', () => ({ Network: network }))
vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => platform.current } }))

/** What the WebView claims. On the tablet it claimed `false` throughout. */
let webViewOnline = true

/** The module holds its state at module level, so each test starts from a fresh copy. */
async function fresh() {
  vi.resetModules()
  return import('./connectivity.ts')
}

/** The listener the module registered, so a test can change the network under it. */
function registered(): StatusListener {
  const listener = network.addListener.mock.calls.at(0)?.[1]

  if (listener === undefined) {
    throw new Error('watchConnectivity registered no listener')
  }

  return listener
}

const WIFI: Status = { connected: true, connectionType: 'wifi' }
const NONE: Status = { connected: false, connectionType: 'none' }

describe('whether there is a network', () => {
  beforeEach(() => {
    webViewOnline = true
    vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => webViewOnline)
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`.
    network.getStatus.mockReset()
    network.addListener.mockReset()
    network.addListener.mockResolvedValue({ remove: () => Promise.resolve() })
    platform.current = 'android'
  })

  it('follows the browser while nothing native has started, as in the web build', async () => {
    const { getSnapshot } = await fresh()

    webViewOnline = false
    expect(getSnapshot()).toBe(false)
    webViewOnline = true
    expect(getSnapshot()).toBe(true)
  })

  /** The MediaPad T3: a validated network, and a WebView that said offline throughout. */
  it('believes Android over a WebView that says it is offline', async () => {
    webViewOnline = false
    network.getStatus.mockResolvedValue(WIFI)
    const { getSnapshot, watchConnectivity } = await fresh()

    await watchConnectivity()

    expect(getSnapshot()).toBe(true)
  })

  /**
   * The other direction, and the gap `useOnline.ts` admitted: a Wi-Fi with no
   * internet behind it, which the browser calls online and Android does not
   * validate.
   */
  it('reports offline when Android does, whatever the WebView says', async () => {
    network.getStatus.mockResolvedValue(NONE)
    const { getSnapshot, watchConnectivity } = await fresh()

    await watchConnectivity()

    expect(getSnapshot()).toBe(false)
  })

  it('follows Android as the network goes and comes back, and says so each time', async () => {
    network.getStatus.mockResolvedValue(WIFI)
    const { getSnapshot, subscribe, watchConnectivity } = await fresh()
    const changed = vi.fn()
    subscribe(changed)
    await watchConnectivity()
    changed.mockClear()

    registered()(NONE)
    expect(getSnapshot()).toBe(false)
    expect(changed).toHaveBeenCalled()

    changed.mockClear()
    registered()(WIFI)
    expect(getSnapshot()).toBe(true)
    expect(changed).toHaveBeenCalled()
  })

  /**
   * Between launch and Android's first answer. The tablet's WebView says
   * offline from its first frame, so reading it here would flash the banner at
   * every launch before Android corrected it.
   */
  it('shows no banner while Android has not answered yet', async () => {
    webViewOnline = false
    let answer: (status: Status) => void = () => undefined
    network.getStatus.mockReturnValue(new Promise((resolve) => { answer = resolve }))
    const { getSnapshot, watchConnectivity } = await fresh()

    const started = watchConnectivity()
    expect(getSnapshot()).toBe(true)

    answer(WIFI)
    await started
    expect(getSnapshot()).toBe(true)
  })

  /** iOS never showed the defect, and no iOS build carries the plugin yet. */
  it('leaves iOS to the browser, and never asks the plugin there', async () => {
    platform.current = 'ios'
    webViewOnline = false
    const { getSnapshot, watchConnectivity } = await fresh()

    await watchConnectivity()

    expect(getSnapshot()).toBe(false)
    expect(network.getStatus).not.toHaveBeenCalled()
    expect(network.addListener).not.toHaveBeenCalled()
  })

  /** A failure here must leave things no worse than they were before this file existed. */
  it('falls back to the browser if the plugin fails', async () => {
    webViewOnline = false
    network.getStatus.mockRejectedValue(new Error('"Network" plugin is not implemented on android'))
    const { getSnapshot, watchConnectivity } = await fresh()

    await watchConnectivity()

    expect(getSnapshot()).toBe(false)
  })

  /**
   * The network is lost after the question was asked and before its answer
   * arrived. The change is newer than the answer, so the answer must not
   * overwrite it and put the banner away while the network is gone.
   */
  it('lets a change heard first stand over an older answer', async () => {
    let answer: (status: Status) => void = () => undefined
    network.getStatus.mockReturnValue(new Promise((resolve) => { answer = resolve }))
    const { getSnapshot, watchConnectivity } = await fresh()

    const started = watchConnectivity()
    await vi.waitFor(() => { expect(network.getStatus).toHaveBeenCalled() })
    registered()(NONE)
    answer(WIFI)
    await started

    expect(getSnapshot()).toBe(false)
  })
})
