<?php

namespace Tests\Feature;

use App\Http\Middleware\JwtAuth;
use App\Support\Jwt;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests du profil API basé sur le JWT validé par {@see JwtAuth}.
 *
 * L'inscription et le login sont gérés par auth-server ; Laravel n'expose plus
 * /api/register ni /api/login.
 */
class AuthTest extends TestCase
{
    use RefreshDatabase;

    private const PROFILE_ENDPOINT = '/api/profile';

    public function test_profile_returns_claims_from_valid_jwt(): void
    {
        $token = Jwt::encode(42, 'formateur@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson(self::PROFILE_ENDPOINT);

        $response->assertStatus(200)
            ->assertJsonPath('user.id', 42)
            ->assertJsonPath('user.email', 'formateur@example.com')
            ->assertJsonPath('user.role', 'formateur');
    }

    public function test_profile_requires_authentication(): void
    {
        $response = $this->getJson(self::PROFILE_ENDPOINT);

        $response->assertStatus(401)->assertJsonPath('message', 'Unauthenticated.');
    }

    public function test_profile_rejects_invalid_token(): void
    {
        $response = $this->withHeader('Authorization', 'Bearer not-a-valid-jwt')
            ->getJson(self::PROFILE_ENDPOINT);

        $response->assertStatus(401)->assertJsonPath('message', 'Invalid or expired token.');
    }
}
