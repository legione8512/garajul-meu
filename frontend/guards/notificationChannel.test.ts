import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

import { REMINDER_CHANNEL_ID } from '../src/notifications/reminderChannel.ts'

/**
 * Holds the manifest and the code to one notification channel.
 *
 * <p>Since 1.0.2 a reminder reaches the same channel by two roads: with the
 * application open, `foregroundNotifications.ts` posts it there by id; with the
 * application closed, Firebase posts it into whatever the manifest names as its
 * default. The two are written in different languages in different files, and
 * nothing but this would notice them drifting apart. If they did, a reminder
 * arriving with the application closed would go to a channel that does not
 * exist, and Firebase would quietly make its own fallback channel again -
 * exactly the split 1.0.2 removed, and invisible on any test device where the
 * application happened to be open.
 *
 * <p>In `guards/` for the reason the others are: it reads the filesystem.
 */
const MANIFEST = 'android/app/src/main/AndroidManifest.xml'

describe('the reminder channel', () => {
  it('is the one Firebase uses for a reminder that arrives with the application closed', () => {
    const manifest = readFileSync(MANIFEST, 'utf8')
    const declared = /android:name="com\.google\.firebase\.messaging\.default_notification_channel_id"\s+android:value="([^"]+)"/
      .exec(manifest)

    expect(declared?.[1]).toBe(REMINDER_CHANNEL_ID)
  })
})
