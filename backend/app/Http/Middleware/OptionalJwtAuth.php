<?php

namespace App\Http\Middleware;

use App\Support\Jwt;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Throwable;

/**
 * Décode un Bearer JWT s'il est présent et valide, et remplit les mêmes
 * attributs que {@see JwtAuth} sans renvoyer de 401 si le token est absent.
 *
 * Utilisé sur les routes publiques qui ont besoin de connaître l'identité
 * lorsqu'elle est fournie (ex. ne pas incrémenter les vues pour le formateur
 * propriétaire).
 */
class OptionalJwtAuth
{
    public function handle(Request $request, Closure $next): Response
    {
        $authorization = (string) $request->header('Authorization', '');

        if (! str_starts_with($authorization, 'Bearer ')) {
            return $next($request);
        }

        $token = trim(substr($authorization, 7));
        if ($token === '') {
            return $next($request);
        }

        try {
            $claims = Jwt::decode($token);
        } catch (Throwable) {
            return $next($request);
        }

        $userId = isset($claims['sub']) ? (int) $claims['sub'] : null;
        $email = $claims['email'] ?? null;
        $role = $claims['role'] ?? null;

        if ($userId !== null && $email !== null && $role !== null) {
            $request->attributes->set('auth_user_id', $userId);
            $request->attributes->set('auth_email', (string) $email);
            $request->attributes->set('auth_role', (string) $role);
            $request->attributes->set('auth_claims', $claims);
        }

        return $next($request);
    }
}
