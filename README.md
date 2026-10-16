# Validate Steps (v0.2) — 22 h autonome, sans interface quotidienne

Projet Android natif, **réellement compilé et testé en CI**, pour relever les pas à 22 h sans ouvrir d'écran, app compagnon ni service tiers.

## Fonctionnalités
- **22:00 heure locale** : `AlarmManager` lance un `BroadcastReceiver`, qui confie la lecture à **WorkManager** (`androidx.work:work-runtime:2.12.0`). L'écran ne s'ouvre pas.
- Lire uniquement les **pas réels agrégés** de Santé Connect : évite le double comptage entre sources, aucune écriture de pas.
- Après échec transitoire, **tentatives automatiques toutes les ~12 minutes avant minuit**, sans réveiller l'utilisateur. Les échecs d'autorisation sont signalés, pas masqués.
- Après redémarrage / changement d'heure ou de fuseau : reprogramme les alarmes et rattrape une lecture manquée après 22 h si encore possible.
- Evite la lecture en double après un relevé réussi ; journal local : date, pas, nombre de tentatives et état.
- Fonctionnement local et privé : **aucune permission Internet**, pas de serveur, compte tiers, traçage, root ou injection de pas.

## Ce qui n'est PAS automatisé

Une lecture Health Connect **ne crédite pas les pièces** des applications de récompenses :
- **WeWard** impose d'ouvrir l'application et d'appuyer sur « Valider mes pas ». Leur FAQ explique que cela empêche la triche automatisée.
- **Macadam** impose de valider les pas manuellement avant minuit. Les pas manqués ne sont pas récupérables.
- **Winwalk** : selon les réponses de son éditeur sur l'App Store, une vidéo récompensée lancée une fois dans la journée permet ensuite de récolter automatiquement les pièces **jusqu'à minuit**. Cette activation nécessite toutefois une ouverture quotidienne ; ce n'est pas une API pour notre application.
- **Sweatcoin** : suivi et conversion prévus en arrière-plan par Sweatcoin lui-même, sous réserve qu'Android le laisse fonctionner.

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
