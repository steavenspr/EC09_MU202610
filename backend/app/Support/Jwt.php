<?php

namespace App\Support;

use Firebase\JWT\JWT as FirebaseJwt;
use Firebase\JWT\Key;
use Illuminate\Support\Facades\Config;

/**
 * Helper statique pour encoder/decoder les JWT emis par le micro-service
 * auth-server (Spring Boot).
 *
 * Laravel ne signe jamais de JWT dans l'architecture EC06 : seul l'auth-server
 * est emetteur. Ce helper expose une methode decode() utilisee par le middleware
 * JwtAuth, et une methode encode() utile uniquement aux tests pour forger
 * un token valide (meme secret + meme algo HS256).
 */
class Jwt
{
    private const ALGO = 'HS256';

    /**
     * Verifie la signature et les claims (iss, exp, iat) d'un JWT.
     *
     * @return array<string, mixed> les claims decodes
     */
    public static function decode(string $token): array
    {
        $secret = self::secret();
        FirebaseJwt::$leeway = (int) Config::get('app.jwt_leeway', env('JWT_LEEWAY', 5));

        $claims = (array) FirebaseJwt::decode($token, new Key($secret, self::ALGO));

        $expectedIssuer = env('JWT_ISSUER', 'ec06-auth-server');
        if (! isset($claims['iss']) || $claims['iss'] !== $expectedIssuer) {
            throw new \UnexpectedValueException('Invalid JWT issuer');
        }

        return $claims;
    }

    /**
     * Encode un JWT HS256 avec les claims standard (sub, email, role, iss,
     * iat, exp). Utilise en test pour simuler l'emission cote auth-server.
     *
     * @param  array<string, mixed>  $extra  claims additionnels
     */
    public static function encode(int $userId, string $email, string $role, int $ttlMinutes = 15, array $extra = []): string
    {
        $now = time();
        $payload = array_merge($extra, [
            'iss' => env('JWT_ISSUER', 'ec06-auth-server'),
            'sub' => (string) $userId,
            'email' => $email,
            'role' => $role,
            'iat' => $now,
            'exp' => $now + ($ttlMinutes * 60),
        ]);

        return FirebaseJwt::encode($payload, self::secret(), self::ALGO);
    }

    /**
     * Recupere le secret HS256 partage avec l'auth-server.
     */
    private static function secret(): string
    {
        $secret = (string) env('JWT_SECRET', '');
        if (strlen($secret) < 32) {
            throw new \RuntimeException('JWT_SECRET must be defined and at least 32 characters long.');
        }

        return $secret;
    }
}
