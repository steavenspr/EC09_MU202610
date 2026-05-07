<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Verifie que l'utilisateur authentifie (JWT auth-server) possede un role
 * autorise. Doit etre applique APRES le middleware "jwt.auth" qui remplit
 * l'attribut "auth_role".
 *
 * @param  array<int, string>  $roles
 */
class CheckRole
{
    public function handle(Request $request, Closure $next, string ...$roles): Response
    {
        $role = $request->attributes->get('auth_role');

        if ($role === null) {
            return new JsonResponse(['message' => 'Unauthenticated.'], 401);
        }

        if (! in_array($role, $roles, true)) {
            return new JsonResponse(['message' => 'Accès refusé pour ce rôle.'], 403);
        }

        return $next($request);
    }
}
