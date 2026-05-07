-- Base dédiée au micro-service auth-server (tables users / nonces).
-- S'exécute au premier démarrage du volume MySQL uniquement.
-- Le nom d'utilisateur doit correspondre à MYSQL_USER dans .env (ex. skillhub_user).

CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON auth_db.* TO 'skillhub_user'@'%';

FLUSH PRIVILEGES;
