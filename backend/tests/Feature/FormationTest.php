<?php

namespace Tests\Feature;

use App\Models\Formation;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests du FormationController avec authentification JWT (auth-server).
 */
class FormationTest extends TestCase
{
    use RefreshDatabase;

    public function test_index_returns_public_list_of_formations(): void
    {
        Formation::factory()->count(3)->create();

        $response = $this->getJson('/api/formations');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'data' => [
                    ['id', 'titre', 'niveau', 'categorie', 'vues', 'apprenants', 'formateur'],
                ],
            ])
            ->assertJsonCount(3, 'data');
    }

    public function test_index_filters_by_search(): void
    {
        Formation::factory()->create(['titre' => 'Cours React avance']);
        Formation::factory()->create(['titre' => 'Cours Python debutant']);

        $response = $this->getJson('/api/formations?search=React');

        $response->assertStatus(200)->assertJsonCount(1, 'data');
    }

    public function test_index_filters_by_categorie(): void
    {
        Formation::factory()->create(['categorie' => 'Data']);
        Formation::factory()->create(['categorie' => 'Design']);

        $response = $this->getJson('/api/formations?categorie=Data');

        $response->assertStatus(200)->assertJsonCount(1, 'data');
    }

    public function test_show_returns_a_formation(): void
    {
        $formation = Formation::factory()->create();

        $response = $this->getJson("/api/formations/{$formation->id}");

        $response->assertStatus(200)
            ->assertJsonPath('formation.id', $formation->id)
            ->assertJsonPath('formation.titre', $formation->titre);
    }

    public function test_show_increments_view_counter_for_anonymous_visitor(): void
    {
        $formation = Formation::factory()->create(['nombre_de_vues' => 0]);

        $this->getJson("/api/formations/{$formation->id}");

        $this->assertSame(1, (int) $formation->refresh()->nombre_de_vues);
    }

    public function test_show_does_not_increment_view_for_owner_formateur(): void
    {
        $formateurId = 9101;
        $formation = Formation::factory()->create([
            'formateur_id' => $formateurId,
            'nombre_de_vues' => 0,
        ]);
        $token = $this->jwtBearer($formateurId, 'owner@example.com', 'formateur');

        $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson("/api/formations/{$formation->id}");

        $this->assertSame(0, (int) $formation->refresh()->nombre_de_vues);
    }

    public function test_show_returns_404_for_unknown_formation(): void
    {
        $response = $this->getJson('/api/formations/999999');

        $response->assertStatus(404);
    }

    public function test_store_creates_a_formation_for_authenticated_formateur(): void
    {
        $formateurId = 9201;
        $token = $this->jwtBearer($formateurId, 'trainer@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/formations', [
                'titre' => 'PHP avance',
                'description' => 'Une formation complete sur PHP.',
                'categorie' => 'Développement web',
                'niveau' => 'Avancé',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('formation.titre', 'PHP avance');

        $this->assertDatabaseHas('formations', [
            'titre' => 'PHP avance',
            'formateur_id' => $formateurId,
            'formateur_public_label' => 'trainer@example.com',
        ]);
    }

    public function test_store_validates_input(): void
    {
        $token = $this->jwtBearer(9202, 't2@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/formations', [
                'titre' => '',
                'description' => '',
                'categorie' => 'CategorieInconnue',
                'niveau' => 'Expert',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['titre', 'description', 'categorie', 'niveau']);
    }

    public function test_update_modifies_formation_owned_by_formateur(): void
    {
        $formateurId = 9301;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $token = $this->jwtBearer($formateurId, 'own@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->putJson("/api/formations/{$formation->id}", [
                'titre' => 'Nouveau titre',
                'description' => 'Nouvelle description suffisamment longue.',
                'categorie' => 'Data',
                'niveau' => 'Intermédiaire',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('formation.titre', 'Nouveau titre');

        $this->assertDatabaseHas('formations', [
            'id' => $formation->id,
            'titre' => 'Nouveau titre',
        ]);
    }

    public function test_update_rejects_non_owner_formateur(): void
    {
        $ownerId = 9401;
        $otherId = 9402;
        $formation = Formation::factory()->create(['formateur_id' => $ownerId]);
        $token = $this->jwtBearer($otherId, 'other@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->putJson("/api/formations/{$formation->id}", [
                'titre' => 'Tentative de hijack',
                'description' => 'Description',
                'categorie' => 'Data',
                'niveau' => 'Débutant',
            ]);

        $response->assertStatus(403);
    }

    public function test_destroy_deletes_formation_owned_by_formateur(): void
    {
        $formateurId = 9501;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $token = $this->jwtBearer($formateurId, 'del@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/formations/{$formation->id}");

        $response->assertStatus(200);
        $this->assertDatabaseMissing('formations', ['id' => $formation->id]);
    }

    public function test_destroy_rejects_non_owner_formateur(): void
    {
        $ownerId = 9601;
        $otherId = 9602;
        $formation = Formation::factory()->create(['formateur_id' => $ownerId]);
        $token = $this->jwtBearer($otherId, 'nope@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/formations/{$formation->id}");

        $response->assertStatus(403);
        $this->assertDatabaseHas('formations', ['id' => $formation->id]);
    }
}
