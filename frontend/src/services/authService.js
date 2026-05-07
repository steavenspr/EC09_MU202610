import { apiRequest } from './apiClient'
import { authServerRequest } from './authServerClient'
import { computeLoginHmac } from './hmacService'
import { decodeJwtPayload } from './jwtPayload'

const TOKEN_KEY = 'skillhub_token'
const USER_KEY = 'skillhub_user'

// Rôles alignés sur auth-server (et Laravel) : apprenant | formateur
const ALLOWED_ROLES = ['formateur', 'apprenant']

const MAX_FIELD_LENGTH = 255

const JWT_REGEX = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/

const sanitizeString = (value, maxLength = MAX_FIELD_LENGTH) => {
  if (typeof value !== 'string') return ''
  // eslint-disable-next-line no-control-regex
  return value.replace(/[\u0000-\u001F\u007F]/g, '').trim().slice(0, maxLength)
}

const sanitizeRole = (value) => {
  const role = typeof value === 'string' ? value.toLowerCase() : ''
  return ALLOWED_ROLES.includes(role) ? role : 'apprenant'
}

const sanitizeToken = (value) => {
  if (typeof value !== 'string') return null
  const trimmed = value.trim()
  return JWT_REGEX.test(trimmed) ? trimmed : null
}

const sanitizeId = (value) => {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && /^[a-zA-Z0-9_-]{1,64}$/.test(value)) return value
  return null
}

const normalizeUser = (rawUser = {}, fallback = {}) => ({
  id: sanitizeId(rawUser.id ?? fallback.id),
  prenom: sanitizeString(rawUser.prenom || fallback.prenom),
  nom: sanitizeString(rawUser.nom || fallback.nom),
  contact: sanitizeString(rawUser.contact || fallback.contact),
  name: sanitizeString(
    rawUser.name
      || [rawUser.prenom, rawUser.nom].filter(Boolean).join(' ')
      || fallback.name
      || 'Utilisateur',
  ),
  email: sanitizeString(rawUser.email || fallback.email),
  role: sanitizeRole(rawUser.role || rawUser.role_name || fallback.role),
})

const buildSafeUserPayload = (user = {}) => ({
  id: sanitizeId(user.id),
  prenom: sanitizeString(user.prenom),
  nom: sanitizeString(user.nom),
  contact: sanitizeString(user.contact),
  name: sanitizeString(user.name),
  email: sanitizeString(user.email),
  role: sanitizeRole(user.role),
})

const saveSession = ({ token, user }) => {
  const safeToken = sanitizeToken(token)
  if (safeToken) {
    localStorage.setItem(TOKEN_KEY, safeToken)
  }

  const safeUserPayload = buildSafeUserPayload(user)
  localStorage.setItem(USER_KEY, JSON.stringify(safeUserPayload))
}

const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

const userFromAccessToken = (token, emailHint, extras = {}) => {
  const claims = decodeJwtPayload(token)
  const emailRaw = typeof claims.email === 'string' ? claims.email : emailHint

  return normalizeUser(
    {
      id: claims.sub,
      email: emailRaw,
      role: claims.role,
      name: emailRaw && emailRaw.includes('@') ? emailRaw.split('@')[0] : 'Utilisateur',
      ...extras,
    },
    { email: emailHint },
  )
}

const loginWithAuthServer = async ({ email, password }) => {
  const nonce = crypto.randomUUID()
  const timestamp = Math.floor(Date.now() / 1000)
  const hmac = await computeLoginHmac(password, email, nonce, timestamp)

  const payload = await authServerRequest('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, nonce, timestamp, hmac }),
  })

  const token = payload?.accessToken || payload?.access_token
  if (!token) {
    throw new Error('Token JWT manquant dans la reponse login.')
  }

  const safeToken = sanitizeToken(token)
  if (!safeToken) {
    throw new Error('Token JWT mal forme dans la reponse login.')
  }

  const user = userFromAccessToken(safeToken, email)
  const session = { token: safeToken, user, mode: 'auth-server' }
  saveSession(session)

  return session
}

const registerWithAuthServer = async ({ prenom, nom, contact, email, password, role }) => {
  const body = new URLSearchParams()
  body.set('email', email)
  body.set('password', password)
  body.set('role', role || 'apprenant')

  await authServerRequest('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })

  const session = await loginWithAuthServer({ email, password })
  const user = normalizeUser(
    {
      ...session.user,
      prenom,
      nom,
      contact,
      name: [prenom, nom].filter(Boolean).join(' ') || session.user.name,
    },
    session.user,
  )
  saveSession({ token: session.token, user })

  return { ...session, user }
}

export const getStoredUser = () => {
  const rawUser = localStorage.getItem(USER_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return normalizeUser(JSON.parse(rawUser))
  } catch {
    clearSession()
    return null
  }
}

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY)

export const getProfile = async () => {
  const token = getStoredToken()

  if (!token) {
    return null
  }

  const previous = getStoredUser()

  const payload = await apiRequest('/api/profile', {
    headers: { Authorization: `Bearer ${token}` },
  })

  const user = normalizeUser(payload?.user || payload, previous || {})
  saveSession({ token, user })

  return user
}

export const login = async (credentials) => loginWithAuthServer(credentials)

export const register = async (payload) => registerWithAuthServer(payload)

export const logout = () => {
  clearSession()
}
