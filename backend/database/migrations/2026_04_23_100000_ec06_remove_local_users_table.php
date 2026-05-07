<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * EC06 : SkillHub ne persiste plus d'utilisateurs localement.
 * Les identités viennent du micro-service auth-server ; seuls des IDs
 * numériques (claim JWT "sub") sont stockés côté Laravel.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('enrollments', function (Blueprint $table): void {
            $table->dropForeign(['utilisateur_id']);
        });

        Schema::table('formations', function (Blueprint $table): void {
            $table->dropForeign(['formateur_id']);
        });

        Schema::table('formations', function (Blueprint $table): void {
            $table->string('formateur_public_label')->nullable()->after('formateur_id');
        });

        Schema::dropIfExists('users');
        Schema::dropIfExists('password_reset_tokens');
    }

    public function down(): void
    {
        // Migration structurelle non rejouée en sens inverse (rendu exam EC06).
    }
};
