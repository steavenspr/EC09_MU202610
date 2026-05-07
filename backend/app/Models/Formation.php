<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * Représente une formation SkillHub gérée par un formateur.
 *
 * Le formateur est identifié par {@see $formateur_id} : il s'agit de l'ID
 * utilisateur émis par le micro-service auth-server (claim JWT "sub"), sans
 * ligne correspondante dans la base Laravel.
 */
class Formation extends Model
{
    use HasFactory;

    protected $fillable = [
        'titre',
        'description',
        'categorie',
        'niveau',
        'formateur_id',
        'formateur_public_label',
        'nombre_de_vues',
    ];

    public const CREATED_AT = 'date_creation';

    public const UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'nombre_de_vues' => 'integer',
            'date_creation' => 'datetime',
        ];
    }

    /**
     * Retourne les modules associés à la formation.
     */
    public function modules(): HasMany
    {
        return $this->hasMany(Module::class);
    }

    /**
     * Retourne les inscriptions liées à cette formation.
     */
    public function inscriptions(): HasMany
    {
        return $this->hasMany(Enrollment::class, 'formation_id');
    }
}
