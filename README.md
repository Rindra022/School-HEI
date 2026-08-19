# School-HEI - Gestion des Diplomes

Projet POJA pour la gestion des diplomes de l'HEI (Haute Ecole d'Ingeniariat).  
Gestion complete du parcours academique sur 3 ans : notes, releves, groupes, promotions et determination des diplomes.

## Stack technique

- **Java 21** + **Spring Boot 3.2.2**
- **Spring Data JPA** + **PostgreSQL** + **Flyway** (migrations)
- **Spring Security** (JWT bearer, roles STUDENT/TEACHER/ADMIN)
- **Thymeleaf** (interface web)
- **AWS S3** (stockage PDF/XLSX)
- **AWS SES** (envoi d'emails)
- **AWS EventBridge + SQS** (traitement asynchrone)
- **Apache POI** (generation XLSX)
- **OpenPDF** (generation PDF)
- **Springdoc OpenAPI** (documentation Swagger UI)
- **JaCoCo** (couverture de tests >= 80%)

## Prerequis

- **Java 21** ou superieur
- **Docker** (requis pour Testcontainers lors des tests)
- **Gradle** (inclus via wrapper : `./gradlew`)
- Compte AWS avec acces a **S3**, **SES**, **EventBridge** et **SQS** (pour le mode production)

## Architecture

```
src/main/java/mg/school/hei/
  config/              -- Seeding des comptes de test
  concurrency/         -- Virtual threads (Java 21)
  exception/           -- Gestion globale des erreurs
  file/bucket/         -- Configuration et upload S3
  handler/             -- Lambda handlers (SQS, HTTP)
  mail/                -- Envoi d'emails via SES
  mapper/              -- Mapping domain <-> JPA
  model/               -- Modele metier (records Java)
  repository/          -- Spring Data JPA + modeles JPA
  security/            -- JWT, filtres, configuration Spring Security
  endpoint/
    rest/controller/   -- API REST + DTOs
    web/               -- Controlleurs Thymeleaf
    event/             -- Evenements EventBridge/SQS
  service/             -- Logique metier
    event/             -- Services de traitement asynchrone
```

## Regles de gestion

| Regle | Implementation |
|---|---|
| Un cours peut etre enseigne par plusieurs enseignants | `course_assignment` lie course + teacher + group + annee |
| Un cours est donne a certains groupes (pas forcement tous) | Chaque assignment est lie a un groupe specifique |
| Un etudiant peut changer de groupe a tout moment | `group_membership` avec `start_date` / `end_date` |
| Un etudiant voit SES notes et SES releves | Securite par `@PreAuthorize` sur l'userId |
| Un enseignant voit et modifie les notes de SES cours | Verification `teacher.id` dans `GradeService` |
| Un admin peut tout faire | `@PreAuthorize("hasRole('ADMIN')")` |
| Les notes sont historisees avec motif | Chainage `previous_grade_id` + `reason` + `is_current` |
| Un diplome = 10 a tous les cours du parcours | `GraduateService` verifie chaque cours sur 3 ans |

## Concept de Track (parcours)

Chaque etudiant suit un parcours parmi :

| Track | Description |
|---|---|
| **EL** | Ecosysteme Logiciel |
| **TN** | Transformation Numerique |
| **COMMUN** | Tronc commun (debut de parcours, avant specialisation) |

Un etudiant peut changer de track en changeant de groupe. Par exemple, en L1 il est en **COMMUN** (groupe K1), puis en L2 il specialise en **EL** (groupe K3) ou **TN** (groupe K4). Les matieres specifiques a chaque track n'apparaissent que dans les bulletins des etudiants concernes.

## Determination des diplomes

Un etudiant est **diplome** s'il a obtenu **la note minimale de 10/20 a tous les cours** de son parcours sur 3 ans. L'algorithme :

1. Pour chaque etudiant de la promotion, on recupere le releve complet (3 ans)
2. On verifie que le releve est **complet** (tous les exams de tous les cours sont notes)
3. On verifie que la **moyenne de chaque cours** est **>= 10** sur l'ensemble du parcours
4. Si toutes les conditions sont remplies, l'etudiant est diplome
5. Les diplomes sont classes par **moyenne generale decroissante** avec un rang

Les diplomes sont separes par track : une feuille **EL** et une feuille **TN** dans le fichier Excel.

## Historisation des notes

Les notes peuvent changer au fil du temps (reclamation, erreur de transcription). Le systeme preserve l'historique complet :

```
Grade v3 (current=true)  -->  Grade v2  -->  Grade v1
   note: 15                  note: 12         note: 10
   reason: "Correction       reason: "Erreur  reason: "Note
   apres reclamation"        de saisie"       initiale"
```

- Chaque note a un lien vers sa version precedente (`previous_grade_id`)
- Seule la version **courante** (`is_current=true`) est utilisee pour les calculs
- La modification exige obligatoirement un **motif** (`reason`)
- L'endpoint `/grades/{id}/history` reconstruit la chaine complete

## Flux asynchrone (PDF par email)

L'envoi du releve de notes en PDF suit un flux asynchrone via AWS :

```
Client                    EventBridge              SQS                 Lambda                S3 + SES
  |                           |                      |                    |                     |
  |-- POST /transcript/pdf -->|                      |                    |                     |
  |                           |-- TranscriptPdf -->  |                    |                     |
  |                           |   Requested          |                    |                     |
  |                           |                      |-- consumable ---->|                     |
  |                           |                      |   event           |                     |
  |                           |                      |                    |-- generate PDF ---->|
  |                           |                      |                    |-- upload PDF ------>|
  |                           |                      |                    |-- presign URL       |
  |                           |                      |                    |-- send email ------>|
  |<-- 202 Accepted ---------|                      |                    |                     |
```

1. Le client appelle `POST /students/{id}/transcript/pdf`
2. L'API publie un evenement `TranscriptPdfRequested` sur **EventBridge**
3. L'evenement est route vers une file **SQS**
4. Un **Lambda** (via `MailboxEventHandler`) consomme l'evenement
5. Le service `TranscriptPdfRequestedService` genere le PDF, l'upload sur **S3**, genere une URL presignee et envoie l'email via **SES**
6. Le client recoit immediatement un `202 Accepted` sans attendre le traitement

## Roles et droits

| Role | Droits |
|---|---|
| **STUDENT** | Voir ses notes, ses releves, demander un PDF par email |
| **TEACHER** | Voir/modifier les notes de ses cours, creer des exams |
| **ADMIN** | Tout faire : promotions, groupes, cours, assignments, diplomes |

## Comptes de test

Le `TestAccountsSeeder` cree automatiquement 3 comptes au demarrage :

| Role | Email | Mot de passe |
|---|---|---|
| ADMIN | `admin@hei.school` | `password123` |
| TEACHER | `teacher@hei.school` | `password123` |
| STUDENT | `student@hei.school` | `password123` |

## Endpoints principaux

### Authentification
| Methode | Endpoint | Description |
|---|---|---|
| POST | `/register` | Inscription (creer un compte STUDENT) |
| POST | `/login` | Connexion, retourne un JWT |
| GET | `/me` | Informations de l'utilisateur connecte |

### Promotions
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/promotions` | Public | Lister les promotions |
| POST | `/promotions` | ADMIN | Creer une promotion |
| GET | `/promotions/{id}` | Public | Details d'une promotion |
| PATCH | `/promotions/{id}` | ADMIN | Modifier une promotion |
| DELETE | `/promotions/{id}` | ADMIN | Supprimer une promotion |

### Cours
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/courses` | Public | Lister les cours |
| POST | `/courses` | ADMIN | Creer un cours |
| GET | `/courses/{id}` | Public | Details d'un cours |
| PATCH | `/courses/{id}` | ADMIN | Modifier un cours |
| DELETE | `/courses/{id}` | ADMIN | Supprimer un cours |

### Assignations de cours
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/course-assignments` | Authentifie | Lister les assignations (filtre par teacher si TEACHER) |
| POST | `/course-assignments` | ADMIN | Assigner un cours a un enseignant/groupe |
| GET | `/course-assignments/{id}` | Authentifie | Details d'une assignation |
| DELETE | `/course-assignments/{id}` | ADMIN | Supprimer une assignation |

### Groupes
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/groups` | ADMIN/TEACHER | Lister les groupes |
| POST | `/groups` | ADMIN | Creer un groupe |
| PATCH | `/groups/{id}` | ADMIN | Modifier un groupe |
| DELETE | `/groups/{id}` | ADMIN | Supprimer un groupe |

### Appartenances de groupe
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/group-memberships?studentId=` | ADMIN ou proprietaire | Appartenances d'un etudiant |
| POST | `/group-memberships` | ADMIN | Ajouter un etudiant a un groupe |

### Exams
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/exams?assignmentId=` | Authentifie | Lister les exams d'une assignation |
| POST | `/exams` | ADMIN/TEACHER | Creer un exam (teacher : ses cours seulement) |
| DELETE | `/exams/{id}` | ADMIN/TEACHER | Supprimer un exam |

### Notes
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/grades?studentId=&examId=` | Authentifie | Lister les notes |
| POST | `/grades` | ADMIN/TEACHER | Noter (teacher : ses cours seulement) |
| GET | `/grades/{id}` | Authentifie | Detail d'une note |
| GET | `/grades/{id}/history` | Authentifie | Historique des modifications |

### Releves de notes
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/students/{id}/transcript?academicYear=` | ADMIN ou proprietaire | Releve d'une annee |
| GET | `/students/{id}/transcript/full` | ADMIN ou proprietaire | Releve complet (3 ans) |
| POST | `/students/{id}/transcript/pdf?academicYear=` | ADMIN ou proprietaire | Demander un PDF par email (async) |

### Diplomes
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/promotions/{id}/graduates?track=` | ADMIN | Liste des diplomes (EL/TN) |
| GET | `/promotions/{id}/graduates/export` | ADMIN | Telecharger XLSX des diplomes (S3) |

### Interface web
| Methode | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/promotions-view` | Public | Liste des promotions + bouton telechargement |

## Structure de la base de donnees

```
app_user (id, first_name, last_name, birthdate, email, password, phone, role, created_at)
promotion (id, year)
student_counter (year, count)  -- compteur atomique pour generation STD
student (id -> app_user, std, promotion_id -> promotion)
app_group (id, ref, track)  -- track: EL, TN, COMMUN
group_membership (id, student_id, group_id, start_date, end_date)
course (id, ref, title, credits)
course_assignment (id, course_id, teacher_id, group_id, academic_year)
exam (id, assignment_id, date_exam, coefficient)
grade (id, student_id, exam_id, value, graded_at, reason, previous_grade_id, is_current)
```

## Generation du STD

Le matricule `STD` est genere automatiquement lors de l'inscription :
- Format : `STD{AA}{NNN}` ou `AA` = derniers chiffres de l'annee, `NNN` = compteur atomique
- Exemple : `STD24001` = 1er etudiant inscrit en 2024

## Export Excel (XLSX)

Le fichier des diplomes contient 2 feuilles :
- **EL** : diplomates du parcours EL
- **TN** : diplomates du parcours TN

Colonnes : Rang, STD, Nom, Prenom, Moyenne generale

## Lancement

Variables d'environnement requises :

| Variable | Description |
|---|---|
| `JWT_SECRET` | Secret pour la signature JWT |
| `aws.s3.bucket` | Nom du bucket S3 |
| `aws.eventBridge.bus` | Nom du bus EventBridge |

```bash
./gradlew bootRun
```

L'application demarre sur le port 8080. La documentation Swagger est disponible sur `/swagger-ui.html`.

## Tests

```bash
./gradlew clean test
./gradlew jacocoTestReport 
```

La commande declenche automatiquement la verification JaCoCo (seuil 80%) puis genere le rapport de couverture. Les tests utilisent **Testcontainers** pour PostgreSQL et des mocks pour S3/SES/SQS.
