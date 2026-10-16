# Validate Steps (v0.4) — 22 h autonome, sans interface quotidienne

Projet Android natif, **réellement compilé et testé en CI**, pour relever les pas à 22 h sans ouvrir d'écran, app compagnon ni service tiers.

## Fonctionnalités
- **22:00 heure locale** : `AlarmManager` lance un `BroadcastReceiver`, qui confie la lecture à **WorkManager** (`androidx.work:work-runtime:2.12.0`). L'écran ne s'ouvre pas.
- Lire uniquement les **pas réels agrégés** de Santé Connect : évite le double comptage entre sources, aucune écriture de pas.
- Après échec transitoire, **tentatives automatiques toutes les ~12 minutes avant minuit**, sans réveiller l'utilisateur. Une file WorkManager `APPEND_OR_REPLACE` conserve les nouvelles demandes pendant qu'une lecture précédente est en cours. Les échecs d'autorisation sont signalés, pas masqués.
- Après redémarrage / changement d'heure ou de fuseau : reprogramme les alarmes et rattrape une lecture manquée après 22 h si encore possible.
- Chaque alarme transporte une **date d'audit explicite** pour que les retards au-delà de minuit ne lisent jamais les pas du mauvais jour. Evite la lecture en double après un relevé réussi ; journal local : date, pas, nombre de tentatives et état.
- Fonctionnement local et privé : **aucune permission Internet**, pas de serveur, compte tiers, traçage, root ou injection de pas.

## Ce qui n'est PAS automatisé

Une lecture Health Connect **ne crédite pas les pièces** des applications de récompenses :
- **WeWard** impose d'ouvrir l'application et d'appuyer sur « Valider mes pas ». Leur FAQ explique que cela empêche la triche automatisée.
- **Macadam** impose de valider les pas manuellement avant minuit. Les pas manqués ne sont pas récupérables.
- **Winwalk** : selon les réponses de son éditeur sur l'App Store, une vidéo récompensée lancée une fois dans la journée permet ensuite de récolter automatiquement les pièces **jusqu'à minuit**. Cette activation nécessite toutefois une ouverture quotidienne ; ce n'est pas une API pour notre application.
- **Sweatcoin** : suivi et conversion prévus en arrière-plan par Sweatcoin lui-même. Sa propre FAQ Android recommande cependant une ouverture de l'app au moins une fois toutes les 12 heures en cas de difficultés de conversion ; ce n'est donc pas une garantie de zéro ouverture.

Aucun endpoint public de validation « headless » autorisée n'a été établi pour les trois premiers. Nous ne tentons **ni contournement d'authentification, ni invocation d'API privée, ni simulation de pas**, et l'application ne déclare jamais à tort avoir crédité les récompenses.

## Installer sur Redmi 15 5G, Android 15

1. **GitHub → Actions → Android APK → dernier build réussi → `validate-steps-debug`** ; extraire le ZIP et installer `app-debug.apk` (version de test signée automatiquement).
2. Ouvrir Validate Steps **une seule fois** pour accorder `READ_STEPS` et `READ_HEALTH_DATA_IN_BACKGROUND` à Santé Connect. Sans consentement système, Android ne permet pas de lire les données.
3. Autoriser les **alarmes exactes** (option système « Alarmes et rappels ») pour viser précisément 22 h ; sinon Android peut retarder l'exécution. Les notifications sont facultatives.
4. Appuyer sur « Activer la lecture quotidienne à 22 h ». Tester la lecture réelle avec le bouton de diagnostic sans toucher au planning.
5. Sur HyperOS, régler **Batterie → Applications → Validate Steps → Aucune restriction** et autoriser **Démarrage automatique** si proposé.

Après cela, le relevé ne nécessite **aucun écran**, mais Android peut différer/annuler une tâche (téléphone arrêté, application désactivée de force, mode économie d'énergie agressif, absence de permission). Une validation garantie à 22:00:00 est impossible sur tous les téléphones Android.

### Développeurs
```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```
SDK Android 35, JDK 17, Gradle 8.10.2. Le job GitHub Actions publie un APK de test téléchargeable. Pas de compte payant.

### Références officielles
- https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses
- https://help.macadam.app/fr/articles/264329-qu-est-ce-que-l-application-macadam
- https://help.macadam.app/fr/articles/291735-j-ai-oublie-de-valider-mes-pas
- https://apps.apple.com/gb/app/it-pays-to-walk-winwalk/id6740551887
- https://help.sweatco.in/hc/en-us/articles/360012911431-Does-Sweatcoin-count-my-steps-when-it-s-in-the-background
- https://developer.android.com/health-and-fitness/health-connect/read-data
- https://developer.android.com/jetpack/androidx/releases/work


## Modifications v0.3 (16 octobre 2026)
- Relances conservées dans une queue séquentielle `APPEND_OR_REPLACE`; une nouvelle lecture ne supprime ni n'interrompt une lecture en attente.
- Dates des alarmes transmises à WorkManager : si le système diffère l'exécution après minuit, le relevé reste attribué au bon jour (jusqu'à un jour de retard).
- Travaux trop anciens, malformés ou futurs ignorés au lieu de lire silencieusement les pas du jour courant.
- Blocage des écritures retardées qui remplaceraient une lecture plus récente.
- Vérification de la disponibilité du droit Android Santé Connect pour lecture en arrière-plan (Android 15 ou Android 14 avec extension suffisante).
- Tests unitaires : transition à minuit, demandes anciennes/futures, ordonnancement.
- Non-objectif délibéré : les pas lus ne deviennent pas automatiquement des pièces créditées sur WeWard, Macadam ou Winwalk.

### Documentation officielle
- https://developer.android.com/reference/androidx/work/ExistingWorkPolicy
- https://developer.android.com/reference/android/health/connect/HealthPermissions
- https://play.google.com/store/apps/details?id=com.winwalk.android
- https://help.sweatco.in/hc/en-us/articles/360012520012-How-can-I-improve-my-step-conversion-All-Other-Android-Devices


## v0.4 — Filet de sécurité sans interface
- Un `PeriodicWorkRequest` de **30 minutes**, avec fenêtre flexible de 15 minutes, complète l'alarme quotidienne. Le système peut le décaler ; ce n'est **pas** un minuteur exact.
- Seulement dans la plage locale **22:00–23:40**, le contrôle vérifie si le relevé Santé Connect du jour a réussi ; sinon il met une lecture en attente dans la file WorkManager sans ouvrir l'interface.
- Après succès, refus de permission, indisponibilité du système ou désactivation, les tâches de lecture inutiles sont évitées.
- Le contrôle périodique est reprogrammé de façon idempotente sur démarrage/redémarrage et annulé à la désactivation.
- Pas d'accès réseau, de root, de login tiers ni de pas fabriqués. Une lecture réussie **n'est pas** un crédit WeWard, Macadam ou Winwalk.
- Tests supplémentaires de la fenêtre de secours, des erreurs permanentes et du passage à l'heure d'hiver.

### Pourquoi ne pas faire de requêtes HTTP privées pour créditer les pièces ?
Les fournisseurs gèrent leurs comptes et leurs vérifications côté serveur. Les intégrations non documentées sont fragiles et peuvent contrevenir à leurs conditions. Les règles publiques de WeWard exigent une validation manuelle, et Macadam fixe une validation dans l'application avant minuit. Seul un mécanisme officiellement autorisé et vérifié pourra être ajouté avec une preuve de crédit réelle.

### Références techniques
- [Android WorkManager périodique](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Android exact alarm / permission](https://developer.android.com/develop/background-work/services/alarms)
- [WeWard validation manuelle](https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses)
- [Macadam validation avant minuit](https://help.macadam.app/fr/articles/469049-comment-fonctionnent-la-validation-des-pas-et-les-recompenses)
