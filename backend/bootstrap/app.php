<?php

use App\Http\Middleware\CheckRole;
use App\Http\Middleware\JwtAuth;
use App\Http\Middleware\OptionalJwtAuth;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        api: __DIR__.'/../routes/api.php',
        web: __DIR__.'/../routes/web.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias([
            'jwt.auth' => JwtAuth::class,
            'jwt.optional' => OptionalJwtAuth::class,
            'check.role' => CheckRole::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        // L'authentification API repose sur jwt.auth (pas sur le guard session).
        unset($exceptions);
    })->create();
