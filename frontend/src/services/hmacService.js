const textEncoder = new TextEncoder()

const bufferToLowerHex = (buffer) =>
  [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, '0')).join('')

/**
 * HMAC-SHA256 (clé = mot de passe UTF-8, message = email:nonce:timestamp)
 * — aligné sur {@code com.example.auth.security.HmacService} (auth-server).
 *
 * @param {string} password
 * @param {string} email
 * @param {string} nonce UUID
 * @param {number} timestampSec epoch secondes
 * @returns {Promise<string>} signature hex minuscule
 */
export const computeLoginHmac = async (password, email, nonce, timestampSec) => {
  const message = `${email}:${nonce}:${timestampSec}`

  const key = await crypto.subtle.importKey(
    'raw',
    textEncoder.encode(password),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  )

  const signature = await crypto.subtle.sign('HMAC', key, textEncoder.encode(message))

  return bufferToLowerHex(signature)
}
