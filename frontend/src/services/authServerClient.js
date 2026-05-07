const AUTH_API_BASE_URL = (import.meta.env.VITE_AUTH_API_BASE_URL || '').replace(/\/$/, '')
const SHOULD_LOG = import.meta.env.DEV || import.meta.env.VITE_DEBUG_API === 'true'

const buildUrl = (path) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${AUTH_API_BASE_URL}${normalizedPath}`
}

const parseBody = async (response) => {
  const text = await response.text()
  if (!text) {
    return {}
  }

  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    try {
      return JSON.parse(text)
    } catch {
      return {}
    }
  }

  try {
    return JSON.parse(text)
  } catch {
    return { raw: text }
  }
}

const logError = (path, method, status, message, payload) => {
  if (!SHOULD_LOG) {
    return
  }
  console.error('[SkillHub Auth Server]', {
    method,
    path,
    url: buildUrl(path),
    status,
    message,
    payload,
    timestamp: new Date().toISOString(),
  })
}

/**
 * Appel HTTP vers le micro-service auth-server (Spring Boot).
 *
 * @param {string} path ex. /api/auth/login
 * @param {RequestInit} options
 * @returns {Promise<unknown>}
 */
export const authServerRequest = async (path, options = {}) => {
  if (!AUTH_API_BASE_URL) {
    throw new Error('VITE_AUTH_API_BASE_URL manquant.')
  }

  const { headers: customHeaders = {}, ...rest } = options
  const method = typeof rest.method === 'string' ? rest.method.toUpperCase() : 'GET'

  let response

  try {
    response = await fetch(buildUrl(path), {
      headers: {
        Accept: 'application/json',
        ...customHeaders,
      },
      ...rest,
    })
  } catch {
    const networkError = new Error('Failed to fetch')
    networkError.status = 0
    logError(path, method, 0, networkError.message, null)
    throw networkError
  }

  const payload = await parseBody(response)

  if (!response.ok) {
    const message =
      typeof payload?.message === 'string'
        ? payload.message
        : typeof payload?.raw === 'string'
          ? payload.raw
          : 'Une erreur est survenue.'

    const error = new Error(message)
    error.status = response.status

    if (payload?.errors && typeof payload.errors === 'object') {
      error.details = payload.errors
    }

    logError(path, method, error.status, error.message, payload)
    throw error
  }

  return payload
}

export { AUTH_API_BASE_URL }
