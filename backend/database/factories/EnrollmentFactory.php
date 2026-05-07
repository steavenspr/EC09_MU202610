<?php

namespace Database\Factories;

use App\Models\Enrollment;
use App\Models\Formation;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * Factory pour le modèle Enrollment.
 *
 * @extends Factory<Enrollment>
 */
class EnrollmentFactory extends Factory
{
    /**
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'utilisateur_id' => fake()->numberBetween(1000, 999_999),
            'formation_id' => Formation::factory(),
            'progression' => 0,
        ];
    }
}
