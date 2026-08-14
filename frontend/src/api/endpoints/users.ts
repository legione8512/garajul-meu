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

export function getProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/api/v1/users/me')
}