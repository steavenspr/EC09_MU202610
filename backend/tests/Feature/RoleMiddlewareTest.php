<?php

namespace Tests\Feature;

use App\Models\Formation;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests du middleware CheckRole avec la chaîne jwt.auth + check.role.
 */
class RoleMiddlewareTest extends TestCase
{
    use RefreshDatabase;

    public function test_formateur_endpoint_allows_formateur(): void
    {
        $token = $this->jwtBearer(8101, 'f@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/formations', [
                'titre' => 'Test formation',
                'description' => 'Une description valide pour la formation.',
                'categorie' => 'Data',
                'niveau' => 'Débutant',
            ]);

        $response->assertStatus(201);
    }

    public function test_formateur_endpoint_rejects_apprenant(): void
    {
        $token = $this->jwtBearer(8102, 'a@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/formations', [
                'titre' => 'Test',
                'description' => 'Description',
                'categorie' => 'Data',
                'niveau' => 'Débutant',
            ]);

        $response->assertStatus(403)
            ->assertJsonPath('message', 'Accès refusé pour ce rôle.');
    }

    public function test_formateur_endpoint_rejects_unauthenticated_user(): void
    {
        $response = $this->postJson('/api/formations', [
            'titre' => 'Test',
            'description' => 'Description',
            'categorie' => 'Data',
            'niveau' => 'Débutant',
        ]);

        $response->assertStatus(401);
    }

    public function test_apprenant_endpoint_allows_apprenant(): void
    {
        $formation = Formation::factory()->create();
        $token = $this->jwtBearer(8201, 'app@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(201);
    }

    public function test_apprenant_endpoint_rejects_formateur(): void
    {
        $formation = Formation::factory()->create();
        $token = $this->jwtBearer(8202, 'frm@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(403);
    }

    public function test_apprenant_endpoint_rejects_unauthenticated_user(): void
    {
        $formation = Formation::factory()->create();

        $response = $this->postJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(401);
    }
}
