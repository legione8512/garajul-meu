/**
 * Holds the page at its own size inside the native application.
 *
 * <p><strong>Found on 2026-09-14 on an iPad.</strong> With the certificate
 * zoomed to 125% in portrait, turning the iPad to landscape left the whole
 * application enlarged - text, buttons, everything - with no way back short of
 * closing it. Two behaviours meet there. WebKit may change a page's scale by
 * itself: when the device turns, it can keep the same width of content in view,
 * which in a wider window means drawing it larger; and it zooms towards an
 * input whose text is small. Capacitor, for its part, switches off pinching the
 * moment any zoom begins (`scrollViewWillBeginZooming` in
 * WebViewDelegationHandler.swift, read on 2026-09-14), so a zoom the person did
 * not make is one they cannot undo.
 *
 * <p>The rotation itself could not be driven on the simulator from here, and
 * the page was measured instead: at 125% in portrait it was exactly as wide as
 * the window, with a scale of 1. So nothing on the page asks to be enlarged,
 * and the fix is not to allow it: a viewport with its minimum and maximum scale
 * at 1, which WKWebView honours, clamps whatever scale WebKit arrives at back to
 * 1. TRIGGER for revisiting: the same report after this shipped, which would
 * mean the scale is not what changed.
 *
 * <p><strong>Native only, and that is the accessibility decision.</strong> In a
 * browser, pinch zoom is how somebody who needs larger text gets it, and a
 * locked viewport takes that away on Android; the web build keeps its viewport
 * as index.html writes it. The native application loses nothing it had, because
 * Capacitor had already taken pinching away there. The certificate keeps its
 * own zoom controls, which are the zoom that screen needs.
 */

const LOCKS = ['minimum-scale=1', 'maximum-scale=1', 'user-scalable=no']

const LOCKED_KEYS = /^(minimum-scale|maximum-scale|user-scalable)\s*=/i

/**
 * Adds the three locks to the page's viewport, keeping everything index.html
 * already says there - `viewport-fit=cover` above all, without which the safe
 * areas read as zero. Repeating it changes nothing. A document with no viewport
 * is left alone: there is nothing to add to, and inventing one would drop
 * `viewport-fit`.
 */
export function lockPageScale(doc: Document): void {
  const viewport = doc.querySelector<HTMLMetaElement>('meta[name="viewport"]')

  if (viewport === null) {
    return
  }

  const kept = viewport.content
    .split(',')
    .map(part => part.trim())
    .filter(part => part !== '' && !LOCKED_KEYS.test(part))

  viewport.content = [...kept, ...LOCKS].join(', ')
}