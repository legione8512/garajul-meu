import { apiFetch } from '../client.ts'

export interface UserProfile {
  id: string
  fullName: string
  email: string
  /** 'ro' or 'en' - the lower-case tag, matching what i18next uses. */
  preferredLanguage: string
  timezone: string
  emailVerified: boolean
}

/**
 * What screen 15 may change about the account itself.
 *
 * <p>Every field optional, because the backend reads an absent field as
 * "unchanged" - that is the whole difference between PATCH and PUT, and it is
 * why this cannot be modelled as a full profile object.
 */
export interface ProfileChanges {
  readonly fullName?: string
  readonly preferredLanguage?: string
  readonly timezone?: string
}

export function getProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/api/v1/users/me')
}

export function updateProfile(changes: ProfileChanges): Promise<UserProfile> {
  return apiFetch<UserProfile>('/api/v1/users/me', {
    method: 'PATCH',
    body: JSON.stringify(changes),
  })
}

/**
 * Screen 16. Answers 204 and <strong>revokes every refresh token, including the
 * caller's own</strong> - section 14 requires it after a reset and the same
 * reasoning applies here. The screen signs out afterwards rather than leaving
 * somebody on a session that will die silently at the next refresh.
 */
export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>('/api/v1/users/me/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}

/**
 * Screen 17, first half. Answers 204 and <strong>sends the code to the address
 * currently on file, never to the new one</strong> - proving control of the
 * inbox that already owns the account is what makes a stolen access token
 * useless here.
 */
export function requestEmailChange(newEmail: string, currentPassword: string): Promise<void> {
  return apiFetch<void>('/api/v1/users/me/change-email', {
    method: 'POST',
    body: JSON.stringify({ newEmail, currentPassword }),
  })
}

/**
 * Screen 17, second half. Answers with the profile, whose address has moved and
 * whose `emailVerified` is now false - the honest consequence of a code that
 * proved control of the *old* inbox only.
 */
export function confirmEmailChange(code: string): Promise<UserProfile> {
  return apiFetch<UserProfile>('/api/v1/users/me/confirm-email-change', {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
}

/**
 * Screen 22. A password in a body on a DELETE, which RFC 9110 permits and some
 * proxies strip - recorded in the project state as something to verify against
 * the real deployment before release.
 */
export function deleteAccount(currentPassword: string): Promise<void> {
  return apiFetch<void>('/api/v1/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ currentPassword }),
  })
}