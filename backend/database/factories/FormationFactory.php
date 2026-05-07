<?php

namespace Database\Factories;

use App\Models\Formation;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * Factory pour le modèle Formation.
 *
 * {@see $formateur_id} est un identifiant externe (auth-server), sans FK MySQL.
 *
 * @extends Factory<Formation>
 */
class FormationFactory extends Factory
{
    /**
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'titre' => fake()->sentence(4),
            'description' => fake()->paragraph(3),
            'categorie' => fake()->randomElement([
                'Développement web',
                'Data',
                'Design',
                'Marketing',
                'DevOps',
            ]),
            'niveau' => fake()->randomElement([
                'Débutant',
                'Intermédiaire',
                'Avancé',
            ]),
            'formateur_id' => fake()->numberBetween(1000, 999_999),
            'formateur_public_label' => fake()->safeEmail(),
            'nombre_de_vues' => 0,
        ];
    }
}
