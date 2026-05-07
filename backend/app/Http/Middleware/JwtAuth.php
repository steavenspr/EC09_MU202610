<?php

namespace App\Http\Middleware;

use App\Support\Jwt;
use Closure;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Throwable;

/**
 * Valide un JWT HS256 emis par le micro-service auth-server et attache le
 * contexte utilisateur aux attributs de la requete.
 *
 * Les attributs suivants sont disponibles dans les controllers via
 * $request->attributes->get('auth_user_id') etc :
 *  - auth_user_id (int)
 *  - auth_email   (string)
 *  - auth_role    (string: "apprenant" | "formateur")
 *  - auth_claims  (array<string, mixed>)
 */
class JwtAuth
{
    public function handle(Request $request, Closure $next): Response
    {
        $authorization = (string) $request->header('Authorization', '');

        if (! str_starts_with($authorization, 'Bearer ')) {
            return $this->unauthorized('Unauthenticated.');
        }

        $token = trim(substr($authorization, 7));
        if ($token === '') {
            return $this->unauthorized('Unauthenticated.');
        }

        try {
            $claims = Jwt::decode($token);
        } catch (Throwable) {
            return $this->unauthorized('Invalid or expired token.');
        }

        $userId = isset($claims['sub']) ? (int) $claims['sub'] : null;
        $email = $claims['email'] ?? null;
        $role = $claims['role'] ?? null;

        if ($userId === null || $email === null || $role === null) {
            return $this->unauthorized('Invalid token payload.');
        }

        $request->attributes->set('auth_user_id', $userId);
        $request->attributes->set('auth_email', (string) $email);
        $request->attributes->set('auth_role', (string) $role);
        $request->attributes->set('auth_claims', $claims);

        return $next($request);
    }

    private function unauthorized(string $message): JsonResponse
    {
        return response()->json(['message' => $message], 401);
    }
}
