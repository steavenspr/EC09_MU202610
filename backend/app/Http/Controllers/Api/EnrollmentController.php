<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Enrollment;
use App\Models\Formation;
use App\Services\ActivityLogService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Gère l'inscription et la désinscription des apprenants.
 */
class EnrollmentController extends Controller
{
    private const MAX_ACTIVE_ENROLLMENTS_PER_LEARNER = 5;

    /**
     * Inscrit l'apprenant connecté à une formation.
     */
    public function store(Request $request, Formation $formation): JsonResponse
    {
        $userId = $this->ensureLearner($request);

        $alreadyEnrolled = Enrollment::query()
            ->where('utilisateur_id', $userId)
            ->where('formation_id', $formation->id)
            ->exists();

        if ($alreadyEnrolled) {
            return response()->json([
                'message' => 'Vous suivez déjà cette formation.',
            ], 409);
        }

        $activeCount = Enrollment::query()
            ->where('utilisateur_id', $userId)
            ->count();

        if ($activeCount >= self::MAX_ACTIVE_ENROLLMENTS_PER_LEARNER) {
            return response()->json([
                'message' => sprintf(
                    'Limite atteinte : un apprenant ne peut pas être inscrit à plus de %d formations en même temps.',
                    self::MAX_ACTIVE_ENROLLMENTS_PER_LEARNER
                ),
            ], 400);
        }

        $enrollment = Enrollment::create([
            'utilisateur_id' => $userId,
            'formation_id' => $formation->id,
            'progression' => 0,
        ]);

        app(ActivityLogService::class)->log('enrollment.created', [
            'user_id' => $userId,
            'formation_id' => $formation->id,
            'enrollment_id' => $enrollment->id,
        ]);

        return response()->json([
            'message' => 'Enrollment created successfully',
            'enrollment' => [
                'id' => $enrollment->id,
                'utilisateur_id' => $enrollment->utilisateur_id,
                'formation_id' => $enrollment->formation_id,
                'progression' => $enrollment->progression,
                'date_inscription' => $enrollment->date_inscription,
            ],
        ], 201);
    }

    /**
     * Désinscrit l'apprenant connecté d'une formation suivie.
     */
    public function destroy(Request $request, Formation $formation): JsonResponse
    {
        $userId = $this->ensureLearner($request);

        $enrollment = Enrollment::query()
            ->where('utilisateur_id', $userId)
            ->where('formation_id', $formation->id)
            ->first();

        if (! $enrollment) {
            return response()->json([
                'message' => 'Aucune inscription trouvée pour cette formation.',
            ], 404);
        }

        app(ActivityLogService::class)->log('enrollment.deleted', [
            'user_id' => $userId,
            'formation_id' => $formation->id,
            'enrollment_id' => $enrollment->id,
        ]);

        $enrollment->delete();

        return response()->json([
            'message' => 'Enrollment deleted successfully',
        ]);
    }

    /**
     * Retourne les formations suivies par l'apprenant connecté.
     */
    public function mesFormations(Request $request): JsonResponse
    {
        $userId = $this->ensureLearner($request);

        $enrollments = Enrollment::query()
            ->with(['formation' => fn ($query) => $query->withCount('inscriptions')])
            ->where('utilisateur_id', $userId)
            ->orderByDesc('date_inscription')
            ->get();

        return response()->json([
            'formations' => $enrollments->map(function (Enrollment $enrollment): array {
                $formation = $enrollment->formation;

                return [
                    'enrollment_id' => $enrollment->id,
                    'progression' => $enrollment->progression,
                    'date_inscription' => $enrollment->date_inscription,
                    'formation' => [
                        'id' => $formation?->id,
                        'titre' => $formation?->titre,
                        'description' => $formation?->description,
                        'niveau' => $formation?->niveau,
                        'categorie' => $formation?->categorie,
                        'vues' => $formation?->nombre_de_vues,
                        'apprenants' => (int) ($formation?->inscriptions_count ?? 0),
                        'formateur' => [
                            'id' => $formation?->formateur_id,
                            'nom' => $formation?->formateur_public_label,
                        ],
                    ],
                ];
            })->values(),
        ]);
    }

    /**
     * Vérifie que l'utilisateur JWT est un apprenant.
     */
    private function ensureLearner(Request $request): int
    {
        abort_unless($this->jwtRole($request) === 'apprenant', 403, 'Seul un apprenant peut gérer ses inscriptions.');

        return $this->jwtId($request);
    }
}
