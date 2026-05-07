import { describe, it, expect } from 'vitest'
import { computeLoginHmac } from './hmacService'

describe('computeLoginHmac', () => {
  it('produit une signature hex de 64 caracteres (SHA-256)', async () => {
    const hex = await computeLoginHmac('Secret1!', 'user@example.com', '550e8400-e29b-41d4-a716-446655440000', 1700000000)
    expect(hex).toMatch(/^[0-9a-f]{64}$/)
  })

  it('est deterministe pour les memes entrees', async () => {
    const a = await computeLoginHmac('x', 'a@b.c', 'n1', 1)
    const b = await computeLoginHmac('x', 'a@b.c', 'n1', 1)
    expect(a).toBe(b)
  })
})
