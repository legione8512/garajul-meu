/**
 * What the person is running, sent with a message so a problem report says it
 * without anybody having to ask.
 *
 * <p>Chosen at build time, as the session channel is: `VITE_CLIENT` is a literal
 * by the time the bundler runs, so the web bundle carries neither plugin, and a
 * web client answers WEB with no version. The plugins are imported inside the
 * function and nothing but plain values leaves it - a Capacitor plugin is a
 * Proxy that looks thenable, and returning one from an async function never
 * settles (see nativePush.ts).
 */
export interface ClientInfo {
  readonly platform: string
  readonly appVersion: string | null
}

export async function clientInfo(): Promise<ClientInfo> {
  if (import.meta.env.VITE_CLIENT !== 'native') {
    return { platform: 'WEB', appVersion: null }
  }

  const { Capacitor } = await import('@capacitor/core')
  const platform = Capacitor.getPlatform().toUpperCase()

  try {
    const { App } = await import('@capacitor/app')
    const info = await App.getInfo()
    return { platform, appVersion: info.version }
  } catch {
    // The message matters more than its version; send it without one.
    return { platform, appVersion: null }
  }
}
