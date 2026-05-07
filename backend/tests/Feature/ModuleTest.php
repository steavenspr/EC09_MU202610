<?php

namespace Tests\Feature;

use App\Models\Formation;
use App\Models\Module;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests du ModuleController avec JWT formateur.
 */
class ModuleTest extends TestCase
{
    use RefreshDatabase;

    public function test_index_returns_modules_of_a_formation_ordered(): void
    {
        $formation = Formation::factory()->create();
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 2]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 3]);

        $response = $this->getJson("/api/formations/{$formation->id}/modules");

        $response->assertStatus(200)->assertJsonCount(3, 'modules');

        $ordres = collect($response->json('modules'))->pluck('ordre')->all();
        $this->assertSame([1, 2, 3], $ordres);
    }

    public function test_store_creates_a_module_for_owner_formateur(): void
    {
        $formateurId = 6101;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $token = $this->jwtBearer($formateurId, 'm1@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/modules", [
                'titre' => 'Module 1',
                'contenu' => 'Contenu pedagogique du module 1.',
                'ordre' => 1,
            ]);

        $response->assertStatus(201)->assertJsonPath('module.titre', 'Module 1');

        $this->assertDatabaseHas('modules', [
            'titre' => 'Module 1',
            'formation_id' => $formation->id,
            'ordre' => 1,
        ]);
    }

    public function test_store_validates_payload(): void
    {
        $formateurId = 6102;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $token = $this->jwtBearer($formateurId, 'm2@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/modules", [
                'titre' => '',
                'contenu' => '',
                'ordre' => 0,
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['titre', 'contenu', 'ordre']);
    }

    public function test_store_rejects_duplicate_ordre_in_same_formation(): void
    {
        $formateurId = 6103;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        $token = $this->jwtBearer($formateurId, 'm3@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/modules", [
                'titre' => 'Doublon',
                'contenu' => 'Contenu',
                'ordre' => 1,
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['ordre']);
    }

    public function test_store_rejects_non_owner_formateur(): void
    {
        $ownerId = 6201;
        $otherId = 6202;
        $formation = Formation::factory()->create(['formateur_id' => $ownerId]);
        $token = $this->jwtBearer($otherId, 'intrus@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/modules", [
                'titre' => 'Module',
                'contenu' => 'Contenu',
                'ordre' => 1,
            ]);

        $response->assertStatus(403);
    }

    public function test_update_modifies_module_for_owner(): void
    {
        $formateurId = 6301;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $module = Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        $token = $this->jwtBearer($formateurId, 'm4@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->putJson("/api/modules/{$module->id}", [
                'titre' => 'Titre modifie',
                'contenu' => 'Contenu revu.',
                'ordre' => 1,
            ]);

        $response->assertStatus(200)->assertJsonPath('module.titre', 'Titre modifie');
    }

    public function test_update_rejects_non_owner(): void
    {
        $ownerId = 6401;
        $otherId = 6402;
        $formation = Formation::factory()->create(['formateur_id' => $ownerId]);
        $module = Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        $token = $this->jwtBearer($otherId, 'hack@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->putJson("/api/modules/{$module->id}", [
                'titre' => 'Hack',
                'contenu' => 'Contenu',
                'ordre' => 1,
            ]);

        $response->assertStatus(403);
    }

    public function test_destroy_allows_deletion_when_more_than_three_modules(): void
    {
        $formateurId = 6501;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 2]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 3]);
        $moduleToDelete = Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 4]);
        $token = $this->jwtBearer($formateurId, 'm5@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/modules/{$moduleToDelete->id}");

        $response->assertStatus(200);
        $this->assertDatabaseMissing('modules', ['id' => $moduleToDelete->id]);
    }

    public function test_destroy_rejects_deletion_when_only_three_modules(): void
    {
        $formateurId = 6601;
        $formation = Formation::factory()->create(['formateur_id' => $formateurId]);
        $module1 = Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 1]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 2]);
        Module::factory()->create(['formation_id' => $formation->id, 'ordre' => 3]);
        $token = $this->jwtBearer($formateurId, 'm6@example.com', 'formateur');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/modules/{$module1->id}");

        $response->assertStatus(422)
            ->assertJsonPath('message', 'Une formation doit contenir au minimum 3 modules.');

        $this->assertDatabaseHas('modules', ['id' => $module1->id]);
    }
}
