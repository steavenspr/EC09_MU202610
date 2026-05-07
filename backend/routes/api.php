<?php

use App\Http\Controllers\Api\EnrollmentController;
use App\Http\Controllers\Api\FormationController;
use App\Http\Controllers\Api\ModuleController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

// Route de vérification — utilisée par le healthcheck Docker
Route::get('/health', function () {
    return response()->json(['status' => 'ok']);
});

// Catalogue des formations (public)
Route::get('/formations', [FormationController::class, 'index']);
Route::get('/formations/{formation}', [FormationController::class, 'show'])
    ->middleware('jwt.optional');
Route::get('/formations/{formation}/modules', [ModuleController::class, 'index']);

// Authentification : gérée par le micro-service auth-server (Spring Boot).
// Laravel ne fait que valider le JWT (middleware jwt.auth).

Route::middleware('jwt.auth')->group(function () {
    Route::get('/profile', function (Request $request) {
        return response()->json([
            'user' => [
                'id' => $request->attributes->get('auth_user_id'),
                'email' => $request->attributes->get('auth_email'),
                'role' => $request->attributes->get('auth_role'),
            ],
        ]);
    });
});

// Routes formateur uniquement
Route::middleware(['jwt.auth', 'check.role:formateur'])->group(function () {
    Route::post('/formations', [FormationController::class, 'store']);
    Route::put('/formations/{formation}', [FormationController::class, 'update']);
    Route::delete('/formations/{formation}', [FormationController::class, 'destroy']);

    Route::post('/formations/{formation}/modules', [ModuleController::class, 'store']);
    Route::put('/modules/{module}', [ModuleController::class, 'update']);
    Route::delete('/modules/{module}', [ModuleController::class, 'destroy']);
});

// Routes apprenant uniquement
Route::middleware(['jwt.auth', 'check.role:apprenant'])->group(function () {
    Route::post('/formations/{formation}/inscription', [EnrollmentController::class, 'store']);
    Route::delete('/formations/{formation}/inscription', [EnrollmentController::class, 'destroy']);
    Route::get('/apprenant/formations', [EnrollmentController::class, 'mesFormations']);
});
