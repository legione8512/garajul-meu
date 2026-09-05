/**
 * Stands in for `firebase/messaging`, which this project deliberately does not
 * install.
 *
 * <p><strong>Why it exists.</strong> `@capacitor-firebase/messaging` ships a web
 * implementation - `dist/esm/web.js` - that imports five bindings from
 * `firebase/messaging`. That package is an *optional* peer dependency, and we do
 * not have it. `index.js` reaches the web implementation through a dynamic
 * `import('./web')`, which Rollup follows whether or not the branch can ever
 * run, so on 2026-09-05 `npm run build` stopped working altogether: five
 * MISSING_EXPORT errors and no `dist` at all. The plugin swap broke the web
 * deployment, and nothing in the test suite could have said so.
 *
 * <p><strong>Why a stub rather than the real thing.</strong> `npm i firebase`
 * would fix the build by shipping the Firebase JavaScript SDK to every browser
 * that loads the site, to support a code path neither of our two targets uses.
 * Section 18 makes push native-only and V1 implements no Firebase Web Push. On
 * Android and iOS the plugin talks to the native bridge and never loads
 * `web.js`; on the web `push` is `null` and the plugin is never imported.
 *
 * <p><strong>Why these throw.</strong> The alternative - silent no-ops - would
 * turn "somebody wired up Web Push and it quietly does nothing" into a bug found
 * by a person who never got a reminder. If this code ever runs, the assumption
 * above has stopped being true and the message says exactly which one.
 *
 * <p>The five names below are the ones `web.js` imports, checked in its source
 * on 2026-09-05. A version of the plugin that imports a sixth would fail the
 * build the same way this one did, which is the right way for that to be found.
 */

function unavailable(name: string): never {
  throw new Error(
    `firebase/messaging is not installed: ${name} was called. Web Push is out of `
    + 'scope for V1 (section 18), so this module is a build-time stub. If Web Push '
    + 'is now in scope, install `firebase` and delete the alias in vite.config.ts.',
  )
}

export function deleteToken(): never {
  return unavailable('deleteToken')
}

export function getMessaging(): never {
  return unavailable('getMessaging')
}

export function getToken(): never {
  return unavailable('getToken')
}

export function isSupported(): Promise<boolean> {
  // The one that must not throw. It is the honest answer to the question asked -
  // no, this build does not support Web Push - and it is the call a caller is
  // most likely to make *before* deciding whether to use any of the others.
  return Promise.resolve(false)
}

export function onMessage(): never {
  return unavailable('onMessage')
}
