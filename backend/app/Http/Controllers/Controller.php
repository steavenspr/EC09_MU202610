<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

abstract class Controller
{
    protected function jwtId(Request $request): int
    {
        return (int) $request->attributes->get('auth_user_id');
    }

    protected function jwtEmail(Request $request): string
    {
        return (string) $request->attributes->get('auth_email');
    }

    protected function jwtRole(Request $request): string
    {
        return (string) $request->attributes->get('auth_role');
    }
}
