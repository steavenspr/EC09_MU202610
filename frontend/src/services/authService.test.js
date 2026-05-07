import { describe, it, expect, beforeEach, vi } from 'vitest'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
}))

vi.mock('./authServerClient', () => ({
  authServerRequest: vi.fn(),
}))

vi.mock('./hmacService', () => ({
  computeLoginHmac: vi.fn(),
}))

import { apiRequest } from './apiClient'
import { authServerRequest } from './authServerClient'
import { computeLoginHmac } from './hmacService'
import {
  login,
  register,
  logout,
  getStoredUser,
  getStoredToken,
  getProfile,
} from './authService'

const makeJwt = (claims) => {
  const h = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
  const p = btoa(JSON.stringify(claims))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
  return `${h}.${p}.sig`
}

const VALID_JWT = makeJwt({
  sub: '42',
  email: 'jean@test.com',
  role: 'formateur',
})

const TOKEN_KEY = 'skillhub_token'
const USER_KEY = 'skillhub_user'

beforeEach(() => {
  apiRequest.mockReset()
  authServerRequest.mockReset()
  computeLoginHmac.mockReset()
  window.localStorage.clear()
  vi.spyOn(crypto, 'randomUUID').mockReturnValue('11111111-2222-4333-8444-555555555555')
})

describe('login', () => {
  it('appelle auth-server avec HMAC puis enregistre le JWT et l utilisateur', async () => {
    computeLoginHmac.mockResolvedValueOnce('abc_hmac_hex')
    authServerRequest.mockResolvedValueOnce({
      accessToken: VALID_JWT,
      tokenType: 'Bearer',
    })

    const session = await login({ email: 'jean@test.com', password: 'Password123!' })

    expect(computeLoginHmac).toHaveBeenCalledWith(
      'Password123!',
      'jean@test.com',
      '11111111-2222-4333-8444-555555555555',
      expect.any(Number),
    )

    expect(authServerRequest).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: expect.stringContaining('"email":"jean@test.com"'),
    })

    const parsedBody = JSON.parse(authServerRequest.mock.calls[0][1].body)
    expect(parsedBody).toMatchObject({
      email: 'jean@test.com',
      nonce: '11111111-2222-4333-8444-555555555555',
      hmac: 'abc_hmac_hex',
    })
    expect(typeof parsedBody.timestamp).toBe('number')

    expect(session.token).toBe(VALID_JWT)
    expect(session.user.role).toBe('formateur')
    expect(localStorage.getItem(TOKEN_KEY)).toBe(VALID_JWT)
    expect(JSON.parse(localStorage.getItem(USER_KEY))).toMatchObject({
      id: '42',
      email: 'jean@test.com',
      role: 'formateur',
    })
  })

  it('accepte access_token comme alias', async () => {
    computeLoginHmac.mockResolvedValueOnce('h')
    authServerRequest.mockResolvedValueOnce({
      access_token: VALID_JWT,
    })

    const session = await login({ email: 'a@b.com', password: 'x' })
    expect(session.token).toBe(VALID_JWT)
  })

  it('rejette la reponse si aucun token', async () => {
    computeLoginHmac.mockResolvedValueOnce('h')
    authServerRequest.mockResolvedValueOnce({})

    await expect(login({ email: 'a@b.com', password: 'x' })).rejects.toThrow(
      'Token JWT manquant dans la reponse login.',
    )
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('rejette et ne persiste pas un token mal forme', async () => {
    computeLoginHmac.mockResolvedValueOnce('h')
    authServerRequest.mockResolvedValueOnce({ accessToken: 'not-a-valid-jwt' })

    await expect(login({ email: 'a@b.com', password: 'x' })).rejects.toThrow(
      'Token JWT mal forme dans la reponse login.',
    )

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('rejette un role non autorise dans le JWT (fallback apprenant)', async () => {
    const jwtBadRole = makeJwt({ sub: '1', email: 'a@b.com', role: 'superuser' })
    computeLoginHmac.mockResolvedValueOnce('h')
    authServerRequest.mockResolvedValueOnce({ accessToken: jwtBadRole })

    const session = await login({ email: 'a@b.com', password: 'x' })
    expect(session.user.role).toBe('apprenant')
  })

  it('tronque les champs au-dela de 255 caracteres (claims email)', async () => {
    const longLocal = `${'a'.repeat(300)}@b.com`
    const jwtLong = makeJwt({ sub: '1', email: longLocal, role: 'apprenant' })
    computeLoginHmac.mockResolvedValueOnce('h')
    authServerRequest.mockResolvedValueOnce({ accessToken: jwtLong })

    const session = await login({ email: 'a@b.com', password: 'x' })
    expect(session.user.email.length).toBeLessThanOrEqual(255)
  })

  it('propage les erreurs auth-server', async () => {
    computeLoginHmac.mockResolvedValueOnce('h')
    const err = new Error('Authentication failed')
    err.status = 401
    authServerRequest.mockRejectedValueOnce(err)

    await expect(login({ email: 'a@b.com', password: 'x' })).rejects.toThrow('Authentication failed')
  })
})

describe('register', () => {
  it('inscrit sur auth-server puis login et fusionne prenom/nom', async () => {
    computeLoginHmac.mockResolvedValue('hmacval')
    authServerRequest
      .mockResolvedValueOnce({ raw: 'User registered' })
      .mockResolvedValueOnce({ accessToken: VALID_JWT })

    const session = await register({
      prenom: 'Alice',
      nom: 'Martin',
      contact: '+261340000000',
      email: 'alice@test.com',
      password: 'Password123!',
      role: 'apprenant',
    })

    const registerOpts = authServerRequest.mock.calls[0][1]
    expect(registerOpts.method).toBe('POST')
    expect(registerOpts.headers).toEqual({ 'Content-Type': 'application/x-www-form-urlencoded' })
    const regParams = new URLSearchParams(registerOpts.body)
    expect(regParams.get('email')).toBe('alice@test.com')
    expect(regParams.get('password')).toBe('Password123!')
    expect(regParams.get('role')).toBe('apprenant')

    expect(session.user.name).toBe('Alice Martin')
    expect(session.user.prenom).toBe('Alice')
    expect(localStorage.getItem(TOKEN_KEY)).toBe(VALID_JWT)
  })

  it('construit le name depuis le JWT si prenom nom vides', async () => {
    const jwtMinimal = makeJwt({ sub: '1', email: 'bob@test.com', role: 'apprenant' })
    computeLoginHmac.mockResolvedValue('h')
    authServerRequest.mockResolvedValueOnce({}).mockResolvedValueOnce({ accessToken: jwtMinimal })

    const session = await register({
      prenom: '',
      nom: '',
      contact: '0',
      email: 'bob@test.com',
      password: 'Password123!',
      role: 'apprenant',
    })

    expect(session.user.email).toBe('bob@test.com')
  })

  it('rejette si login apres register ne renvoie pas de token', async () => {
    computeLoginHmac.mockResolvedValue('h')
    authServerRequest.mockResolvedValueOnce({}).mockResolvedValueOnce({})

    await expect(
      register({
        prenom: 'A',
        nom: 'B',
        contact: 'c',
        email: 'e@test.com',
        password: 'Password123!',
        role: 'apprenant',
      }),
    ).rejects.toThrow('Token JWT manquant dans la reponse login.')
  })
})

describe('logout', () => {
  it('efface le stockage', () => {
    localStorage.setItem(TOKEN_KEY, VALID_JWT)
    localStorage.setItem(USER_KEY, JSON.stringify({ id: 1 }))

    logout()

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })
})

describe('getStoredToken', () => {
  it('retourne le token', () => {
    localStorage.setItem(TOKEN_KEY, VALID_JWT)
    expect(getStoredToken()).toBe(VALID_JWT)
  })

  it('retourne null si absent', () => {
    expect(getStoredToken()).toBeNull()
  })
})

describe('getStoredUser', () => {
  it('retourne null si absent', () => {
    expect(getStoredUser()).toBeNull()
  })

  it('normalise le JSON stocke', () => {
    localStorage.setItem(USER_KEY, JSON.stringify({ id: 7, prenom: 'Eve', email: 'eve@test.com', role: 'formateur' }))
    const user = getStoredUser()
    expect(user).toMatchObject({ id: 7, email: 'eve@test.com', role: 'formateur' })
  })

  it('nettoie si JSON corrompu', () => {
    localStorage.setItem(TOKEN_KEY, VALID_JWT)
    localStorage.setItem(USER_KEY, '{ invalid')

    expect(getStoredUser()).toBeNull()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('role inconnu -> apprenant', () => {
    localStorage.setItem(USER_KEY, JSON.stringify({ id: 1, role: 'hacker' }))
    expect(getStoredUser().role).toBe('apprenant')
  })
})

describe('getProfile', () => {
  it('retourne null sans token', async () => {
    const user = await getProfile()
    expect(user).toBeNull()
    expect(apiRequest).not.toHaveBeenCalled()
  })

  it('appelle Laravel /api/profile et conserve prenom/nom stockes', async () => {
    localStorage.setItem(TOKEN_KEY, VALID_JWT)
    localStorage.setItem(
      USER_KEY,
      JSON.stringify({
        id: '42',
        email: 'jean@test.com',
        role: 'formateur',
        prenom: 'Jean',
        nom: 'Dupont',
        name: 'Jean Dupont',
      }),
    )

    apiRequest.mockResolvedValueOnce({
      user: { id: 5, email: 'jean@test.com', role: 'formateur' },
    })

    const user = await getProfile()

    expect(apiRequest).toHaveBeenCalledWith('/api/profile', {
      headers: { Authorization: `Bearer ${VALID_JWT}` },
    })
    expect(user).toMatchObject({
      id: 5,
      email: 'jean@test.com',
      role: 'formateur',
      prenom: 'Jean',
      nom: 'Dupont',
    })
  })
})
