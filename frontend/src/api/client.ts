import { ApiError, type FieldError } from './ApiError.ts'
import { API_BASE_URL, AUTH_PATH_PREFIX } from './config.ts'
import { refreshSession } from './refresh.ts'
import { getAccessToken } from './tokenStore.ts'

interface ErrorBody {
  code?: string
  requestId?: string
  fieldErrors?: FieldError[]
}

const REQUEST_ID_HEADER = 'X-Request-Id'

function send(path: string, init: RequestInit, token: string | null): Promise<Response> {
  const headers = new Headers(init.headers)

  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token !== null) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    // Always, because the refresh cookie is path-scoped to /api/v1/auth and the
    // browser will only attach it there anyway. The flag is what makes it
    // travel at all across the origin boundary section 21 describes.
    credentials: 'include',
  })
}

async function toApiError(response: Response): Promise<ApiError> {
  const headerRequestId = response.headers.get(REQUEST_ID_HEADER)

  try {
    const body = (await response.json()) as ErrorBody

    return new ApiError(
      body.code ?? 'UNKNOWN',
      response.status,
      body.requestId ?? headerRequestId,
      body.fieldErrors ?? [],
    )
  } catch {
    // No JSON at all - a proxy error page, a connection cut mid-response. There
    // is no code to translate, so say so rather than inventing one.
    return new ApiError('UNKNOWN', response.status, headerRequestId, [])
  }
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204 || response.status === 205) {
    return undefined as T
  }

  const text = await response.text()

  // 201 Created answers with an empty body - the account exists and there is
  // nothing useful to say about it until it is verified. Checking the status
  // alone missed that and tried to parse nothing as JSON.
  if (text.length === 0) {
    return undefined as T
  }

  return JSON.parse(text) as T
}

/**
 * Every call to the backend goes through here.
 *
 * <p>On a 401 it refreshes once and retries once. Exactly once, in both cases:
 * a second refresh would be answering a server that has already told us the new
 * token is unacceptable, and looping on it would spend the rate limit and hang
 * the page.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await send(path, init, getAccessToken())

  if (response.status === 401 && !path.startsWith(AUTH_PATH_PREFIX)) {
    if (await refreshSession()) {
      response = await send(path, init, getAccessToken())
    }
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  return parse<T>(response)
}