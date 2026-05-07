# Auth Server - TP1 à TP5

[![CI/CD TP5](https://github.com/steavenspr/auth-server/actions/workflows/ci.yml/badge.svg)](https://github.com/steavenspr/auth-server/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=steavenspr_auth-server-tp2&metric=alert_status)](https://sonarcloud.io/project/overview?id=steavenspr_auth-server-tp2)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=steavenspr_auth-server-tp2&metric=coverage)](https://sonarcloud.io/project/overview?id=steavenspr_auth-server-tp2)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Docker](https://img.shields.io/badge/Docker-ready-blue)

Serveur d'authentification développé dans le cadre du cours CDWFS.
Le projet évolue du TP1 au TP5, chaque étape corrigeant les failles de la précédente.
L'objectif final est qu'un utilisateur puisse prouver qu'il connaît son mot de passe
sans jamais l'envoyer sur le réseau.

---

## Table des matières
- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Lancer avec Docker](#lancer-avec-docker)
- [Endpoints](#endpoints)
- [Exemples d'utilisation](#exemples-dutilisation)
- [Protocole HMAC](#protocole-hmac)
- [Evolution du projet](#evolution-du-projet)
- [Tests](#tests)
- [Limite pédagogique](#limite-pédagogique)

---

## Stack technique

- Java 17
- Spring Boot 3.1.1
- MySQL (production) / H2 en mémoire (tests)
- Maven
- Docker
- GitHub Actions (CI/CD)
- SonarCloud (qualité de code)

---

## Prérequis

- Java 17
- Maven 3.9+
- MySQL
- Docker Desktop

---

## Installation

1. Cloner le projet :
```bash
git clone https://github.com/steavenspr/auth-server.git
cd auth-server
```

2. Créer la base de données MySQL :
```sql
CREATE DATABASE auth_db_tp3;
```

3. Configurer `src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db_tp3
spring.datasource.username=root
spring.datasource.password=
```

4. Définir la variable d'environnement Master Key (minimum 32 caractères) :
```bash
# Windows PowerShell
$env:APP_MASTER_KEY="UneCleDe32CaracteresExactement!!"
```

5. Lancer l'application :
```bash
mvn spring-boot:run
```

---

## Lancer avec Docker
```bash
# Construire le jar
mvn clean package -DskipTests

# Construire l'image
docker build -t cdwfs-auth-app .

# Lancer le conteneur
docker run -p 8080:8080 -e APP_MASTER_KEY=UneCleDe32CaracteresExactement!! cdwfs-auth-app
```

L'application est ensuite accessible sur `http://localhost:8080`

---

## Endpoints

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Inscription d'un nouvel utilisateur |
| POST | `/api/auth/login` | Connexion via protocole HMAC, retourne un token |
| GET | `/api/me` | Acces a la route protegee par token |
| PUT | `/api/auth/change-password` | Changement de mot de passe |

---

## Exemples d'utilisation

Inscription :
```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -d "email=user@example.com&password=Motdepasse1!"
```

Connexion :
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "nonce": "uuid-aleatoire",
    "timestamp": 1711300000,
    "hmac": "signature-hmac-calculee"
  }'
```

Changement de mot de passe :
```bash
curl -X PUT "http://localhost:8080/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "oldPassword": "Motdepasse1!",
    "newPassword": "NouveauMdp1!",
    "confirmPassword": "NouveauMdp1!"
  }'
```

---

## Protocole HMAC

Le mot de passe ne circule jamais sur le reseau. Le client prouve qu'il le connait
en calculant une signature mathematique.

Cote client :
```
1. Genere un nonce (UUID aleatoire)
2. Recupere le timestamp actuel en secondes
3. Construit le message : email + ":" + nonce + ":" + timestamp
4. Calcule : hmac = HMAC_SHA256(key=password, data=message)
5. Envoie : email, nonce, timestamp, hmac (le mot de passe ne part pas)
```

Cote serveur :
```
1. Verifie que l'email existe
2. Verifie que le timestamp est dans +/- 60 secondes
3. Verifie que le nonce n'a pas deja ete utilise
4. Dechiffre le mot de passe stocke avec la Master Key
5. Recalcule le hmac attendu
6. Compare les deux hmac en temps constant
7. Marque le nonce comme consomme
8. Retourne accessToken + expiresAt
```

---

## Evolution du projet

| Fonctionnalite | TP1 | TP2 | TP3 | TP4 | TP5 |
|----------------|:---:|:---:|:---:|:---:|:---:|
| Stockage mot de passe | En clair | BCrypt | AES | AES-GCM | AES-GCM |
| Politique mot de passe | Non | Oui | Oui | Oui | Oui |
| Protection brute force | Non | Oui | Oui | Oui | Oui |
| Protocole HMAC | Non | Non | Oui | Oui | Oui |
| Anti-rejeu | Non | Non | Oui | Oui | Oui |
| Master Key externe | Non | Non | Non | Oui | Oui |
| Changement mot de passe | Non | Non | Non | Non | Oui |
| Docker | Non | Non | Non | Non | Oui |
| CI/CD GitHub Actions | Non | Non | Non | Oui | Oui |
| SonarCloud | Non | Oui | Oui | Oui | Oui |

---

## Tests
```bash
mvn test
```

45 tests JUnit repartis sur 6 classes :

- **AuthServiceTest** — inscription, connexion HMAC, anti-rejeu, lockout apres 5 echecs, expiration token, changement de mot de passe
- **AesEncryptionServiceTest** — chiffrement/dechiffrement AES-GCM, IV aleatoire, detection de modification, refus demarrage si Master Key trop courte
- **GlobalExceptionHandlerTest** — verification des codes HTTP retournes (400, 401, 409, 423) via MockMvc
- **HmacServiceTest** — calcul HMAC, comparaison en temps constant, signatures distinctes
- **PasswordPolicyValidatorTest** — politique de mot de passe (longueur, majuscule, minuscule, chiffre, caractere special, null)
- **AuthApplicationTests** — chargement du contexte Spring
---

## Limite pedagogique

Ce projet utilise un chiffrement reversible (AES-GCM) pour stocker les mots de passe.
C'est un choix impose par le protocole HMAC qui necessite de recuperer le mot de passe
en clair pour recalculer la signature cote serveur.
En production, on utiliserait un protocole comme SRP ou OPAQUE qui evitent
de stocker le mot de passe de facon recuperable.