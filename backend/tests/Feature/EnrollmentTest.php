<?php

namespace Tests\Feature;

use App\Models\Enrollment;
use App\Models\Formation;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests du EnrollmentController avec JWT apprenant.
 */
class EnrollmentTest extends TestCase
{
    use RefreshDatabase;

    public function test_apprenant_can_enroll_in_a_formation(): void
    {
        $apprenantId = 7101;
        $formation = Formation::factory()->create();
        $token = $this->jwtBearer($apprenantId, 'learn@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(201)
            ->assertJsonStructure(['message', 'enrollment' => ['id', 'utilisateur_id', 'formation_id', 'progression']])
            ->assertJsonPath('enrollment.progression', 0);

        $this->assertDatabaseHas('enrollments', [
            'utilisateur_id' => $apprenantId,
            'formation_id' => $formation->id,
        ]);
    }

    public function test_apprenant_cannot_enroll_twice_in_same_formation(): void
    {
        $apprenantId = 7102;
        $formation = Formation::factory()->create();
        Enrollment::factory()->create([
            'utilisateur_id' => $apprenantId,
            'formation_id' => $formation->id,
        ]);
        $token = $this->jwtBearer($apprenantId, 'learn2@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(409)
            ->assertJsonPath('message', 'Vous suivez déjà cette formation.');
    }

    public function test_enrollment_requires_existing_formation(): void
    {
        $token = $this->jwtBearer(7103, 'learn3@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/formations/999999/inscription');

        $response->assertStatus(404);
    }

    public function test_apprenant_can_unsubscribe_from_a_formation(): void
    {
        $apprenantId = 7104;
        $formation = Formation::factory()->create();
        $enrollment = Enrollment::factory()->create([
            'utilisateur_id' => $apprenantId,
            'formation_id' => $formation->id,
        ]);
        $token = $this->jwtBearer($apprenantId, 'learn4@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(200);
        $this->assertDatabaseMissing('enrollments', ['id' => $enrollment->id]);
    }

    public function test_unsubscribe_returns_404_when_not_enrolled(): void
    {
        $apprenantId = 7105;
        $formation = Formation::factory()->create();
        $token = $this->jwtBearer($apprenantId, 'learn5@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->deleteJson("/api/formations/{$formation->id}/inscription");

        $response->assertStatus(404);
    }

    public function test_mes_formations_returns_only_own_enrollments(): void
    {
        $aliceId = 7201;
        $bobId = 7202;

        $formationA = Formation::factory()->create();
        $formationB = Formation::factory()->create();

        Enrollment::factory()->create([
            'utilisateur_id' => $aliceId,
            'formation_id' => $formationA->id,
        ]);
        Enrollment::factory()->create([
            'utilisateur_id' => $bobId,
            'formation_id' => $formationB->id,
        ]);

        $token = $this->jwtBearer($aliceId, 'alice@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/apprenant/formations');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'formations')
            ->assertJsonPath('formations.0.formation.id', $formationA->id);
    }

    public function test_mes_formations_empty_for_new_apprenant(): void
    {
        $token = $this->jwtBearer(7301, 'new@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/apprenant/formations');

        $response->assertStatus(200)->assertJsonCount(0, 'formations');
    }

    public function test_apprenant_cannot_enroll_when_five_active_formations(): void
    {
        $apprenantId = 7401;
        $formations = Formation::factory()->count(6)->create();

        foreach ($formations->take(5) as $formation) {
            Enrollment::factory()->create([
                'utilisateur_id' => $apprenantId,
                'formation_id' => $formation->id,
            ]);
        }

        $sixthFormation = $formations->last();
        $token = $this->jwtBearer($apprenantId, 'limit5@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$sixthFormation->id}/inscription");

        $response->assertStatus(400)
            ->assertJsonPath(
                'message',
                'Limite atteinte : un apprenant ne peut pas être inscrit à plus de 5 formations en même temps.'
            );

        $this->assertDatabaseMissing('enrollments', [
            'utilisateur_id' => $apprenantId,
            'formation_id' => $sixthFormation->id,
        ]);
    }

    public function test_apprenant_can_enroll_fifth_formation_when_four_active(): void
    {
        $apprenantId = 7402;
        $formations = Formation::factory()->count(5)->create();

        foreach ($formations->take(4) as $formation) {
            Enrollment::factory()->create([
                'utilisateur_id' => $apprenantId,
                'formation_id' => $formation->id,
            ]);
        }

        $fifthFormation = $formations[4];
        $token = $this->jwtBearer($apprenantId, 'fifthok@example.com', 'apprenant');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson("/api/formations/{$fifthFormation->id}/inscription");

        $response->assertStatus(201);
        $this->assertDatabaseHas('enrollments', [
            'utilisateur_id' => $apprenantId,
            'formation_id' => $fifthFormation->id,
        ]);
    }
}
