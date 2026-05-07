<?php

/*
 * EC06 — L'authentification est déléguée au micro-service auth-server
 * (Spring Boot). Laravel ne gère plus aucun utilisateur localement : il se
 * contente de vérifier les JWT HS256 émis par auth-server via le middleware
 * App\Http\Middleware\JwtAuth.
 *
 * Ce fichier reste présent (squelette minimal) uniquement pour satisfaire
 * les dépendances internes du framework. Aucun guard "api" n'est exposé.
 */

return [

    'defaults' => [
        'guard' => env('AUTH_GUARD', 'web'),
        'passwords' => env('AUTH_PASSWORD_BROKER', 'users'),
    ],

    'guards' => [
        'web' => [
            'driver' => 'session',
            'provider' => null,
        ],
    ],

    'providers' => [],

    'passwords' => [],

    'password_timeout' => env('AUTH_PASSWORD_TIMEOUT', 10800),

];
