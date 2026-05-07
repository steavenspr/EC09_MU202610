/**
 * Décode le segment payload d'un JWT (sans vérifier la signature).
 * Réservé à l'UI après réception du token depuis auth-server (la confiance
 * vient du canal HTTPS + origine du serveur).
 *
 * @param {string} token
 * @returns {Record<string, unknown>}
 */
export const decodeJwtPayload = (token) => {
  const parts = String(token).split('.')
  if (parts.length !== 3) {
    throw new Error('JWT invalide.')
  }

  const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
  const pad = b64.length % 4
  const padded = pad === 0 ? b64 : b64 + '='.repeat(4 - pad)
  const json = atob(padded)

  return JSON.parse(json)
}
