# Validate Steps v1.1.0 — actions directes sans assistance à 22 h

Projet Android natif pour relever les pas réels à 22 h sans ouvrir l'écran et proposer une assistance facultative pour les applications de récompenses. Le code est compilé et contrôlé dans GitHub Actions. **Aucun mécanisme autorisé ne garantit le crédit automatique de tous les fournisseurs.**

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


## v0.5 — Secours ciblés, moins de réveils

- L'ancien `PeriodicWorkRequest` (une vérification environ toutes les 30 minutes, y compris hors de la plage utile) est remplacé par **deux `OneTimeWorkRequest` datés** à 22 h 20 et 23 h 15, en complément de l'alarme de 22 h.
- Les contrôles de secours sont associés à une **date locale précise**. Ils ne font aucune lecture s'ils sont exécutés en retard un autre jour, ou si la lecture a déjà réussi.
- Chaque secours utilise le calcul des instants réels, y compris en cas de passage à l'heure d'hiver. Quand le planning est réarmé, les deux demandes sont mises à jour sans créer de doublons.
- La désactivation annule les tâches de secours ; aucune interface n'est ouverte lors de leurs exécutions. Lors d'une mise à jour depuis la v0.4, l'ancien travail périodique nommé `recovery-watchdog` est explicitement annulé (migration des tâches persistantes).
- Les secours réarment eux-mêmes le planning du lendemain : une alarme principale manquée ne désactive pas définitivement les secours du jour suivant.
- **Limite Android** : WorkManager exécute ces deux tâches dès que le système le permet ; les heures exactes ne sont pas garanties. Un téléphone éteint ou un processus arrêté de force ne peut pas être réveillé par magie.

### Provenance et conformité des pas
La lecture locale de Santé Connect ne confirme **aucun** crédit de récompense. En octobre 2026, WeWard documente explicitement un bouton manuel et Macadam une validation quotidienne avant minuit. Ces services n'exposent pas d'API publique d'encaissement sans interface vérifiée par ce projet. Toute future intégration devra être officiellement autorisée et prouver un crédit réel avant de marquer une récompense comme validée.

Sources techniques :
- https://developer.android.com/reference/androidx/work/OneTimeWorkRequest
- https://developer.android.com/reference/androidx/work/ExistingWorkPolicy
- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work


## v0.6 — Essai sans interface via actions des notifications (optionnel)

Nouvelle fonction **expérimentale et désactivée par défaut**. Sans contacter aucun fournisseur, Android offre une voie potentiellement sans écran : certains éditeurs publient dans leurs notifications des **actions directes** (objet `Notification.Action` lié à un `PendingIntent`). Une fois l'autorisation Android accordée, notre `NotificationListenerService` peut inspecter ces actions puis envoyer celles qui satisfont une politique restrictive.

**Conditions strictes avant tout envoi :**
- L'utilisateur doit activer volontairement le module dans Validate Steps et autoriser l'accès aux notifications dans les réglages Android. La permission est sensible car Android la présente comme un accès potentiellement global aux notifications. Notre code filtre les seuls packages `com.weward`, `com.macadamapp`, `com.winwalk.android` avant d'accéder aux actions ; aucun texte de notification n'est enregistré.
- Entre **22:00 et 23:40 heure locale** uniquement : lors de la réception d'une notification, à la connexion du service, à l'alarme de 22 h et aux secours.
- Le bouton doit avoir un **intitulé explicite** relatif à la récupération / validation de pas ou pièces. Pas de bouton « ouvrir », « regarder une publicité », « acheter », ni d'action réclamant une saisie.
- L'objet `PendingIntent` ne doit pas être déclaré comme ouvrant une `Activity`. Cette vérification réduit le risque d'ouverture d'écran, sans garantir qu'une application tierce ne lancera pas un écran depuis son propre code.
- **Un seul essai par fournisseur et par date**. L'application distingue `action_envoyee_non_confirmee`, `action_annulee` et `erreur_envoi`. Aucun solde ni crédit de récompenses n'est inventé.

**Limitation fondamentale :** nous n'avons pas constaté de notification contenant une action de validation directe sur un téléphone réel. WeWard documente notamment des *rappels* invitant à ouvrir l'app. Macadam et Winwalk ne documentent pas non plus un tel `PendingIntent` de validation. L'option pourrait donc ne trouver **aucun bouton compatible** : le fonctionnement 100 % autonome des récompenses **n'est pas démontré**. Santé Connect ne crédite pas les pièces externes.

### Mise en route sur Android 15
1. Installer la v0.6 et garder le relevé autonome 22 h activé.
2. Dans Validate Steps : « Autoriser l'accès aux notifications dans Android », puis autoriser **Validate Steps** dans la liste système.
3. Activer « Essai d'actions directes (sans écran) ». Les essais sont ensuite autonomes dans la fenêtre nocturne.
4. Observer le résultat dans Validate Steps le lendemain. Le statut « action envoyée » **ne signifie pas** « récompense créditée » : seul le solde confirmé dans la vraie application peut le démontrer.

**Ce que ce module ne fait pas :** contourner publicité / CAPTCHA / authentification, utiliser des API privées, ouvrir automatiquement les interfaces, fabriquer des pas ou garantir l'encaissement contre les règles du fournisseur. Pour Winwalk, une éventuelle vidéo quotidienne n'est ni lancée ni simulée. Sweatcoin continue à convertir ses pas par son propre mécanisme de fond.

### Références
- Android NotificationListenerService : https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android PendingIntent : https://developer.android.com/reference/android/app/PendingIntent
- WeWard validation manuelle : https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses
- WeWard rappels : https://faq.weward.fr/hc/fr/articles/28123570360338-Activer-le-rappel-de-validation-des-pas


## v0.7 — Diagnostics sûrs des validations par notification

**Sans contacter d'éditeur, sans API privée et sans ouvrir leurs interfaces.**

- **Diagnostic en lecture seule** via « Diagnostiquer les actions maintenant (sans envoyer) ». Il n'envoie aucun `PendingIntent` et fonctionne même avant d'activer les actions.
- Affiche si WeWard, Macadam et Winwalk sont **installés** (vérification ciblée via `<queries>` Android 11+), si le service de notifications est **connecté**, la date de la dernière inspection et la **catégorie** d'action disponible pour chaque service.
- Catégories : `notification non observée`, `notification_sans_bouton`, `action_ouvrant_interface`, `action_avec_saisie`, `libelle_action_non_reconnu`, `action_directe_compatible`. Une ancienne notification est identifiée séparément.
- **Confidentialité :** aucune copie du contenu des notifications, aucun texte de bouton ni identifiant utilisateur enregistré. Les réglages système donnent toutefois au listener un accès sensible : l'utilisateur doit l'autoriser explicitement.
- Le bouton d'activation refuse de démarrer les actions sans autorisation Android préalable. Désactiver la planification à 22 h désactive aussi les tentatives directes.
- Seules les actions directes `PendingIntent` de type service/diffusion déjà fournies par un éditeur et explicitement associées à des pas peuvent être envoyées, à 22 h–23 h 40 et après consentement. Rien n'est lancé en lisant le diagnostic.

**État de preuve :** les tests CI et les diagnostics ne démontrent pas que WeWard, Macadam ou Winwalk publient un bouton direct compatible ni qu'une action envoyée crédite réellement des récompenses. Il faut observer la catégorie `action_directe_compatible` et vérifier le solde chez l'éditeur avant de conclure à une validation réussie.

### Vérification sans effet de bord
1. Installer le nouvel APK et accorder une fois **Accès aux notifications** dans Android.
2. Ouvrir Validate Steps puis « Diagnostiquer les actions maintenant (sans envoyer) ».
3. Lire les résultats de chaque fournisseur sans déclencher de validation.
4. Pour l'essai automatique, activer séparément les actions directes et la tâche 22 h.


## v0.8 — Récupération du service de notifications + relances sûres

- Si le système Android déconnecte le `NotificationListenerService`, l'application tente une **reconnexion avec `requestRebind()`**, uniquement quand l'utilisateur a autorisé l'accès système et activé le module de validation directe.
- Reconnexion également demandée au redémarrage du téléphone, au déclenchement de 22 h et lors des contrôles de secours. Le service inspecte ensuite les notifications présentes, sans lancer une interface.
- **Confidentialité** : quand les actions directes sont désactivées, aucune inspection automatique des notifications n'est effectuée. Le diagnostic explicitement demandé reste autorisé.
- **Envois limités** : si Android renvoie `PendingIntent.CanceledException`, l'action n'a pas été expédiée et peut être retentée, au maximum **trois essais par fournisseur et par jour**. Un envoi réussi, un envoi en cours ou une erreur ambiguë ne sont jamais retentés.
- Tests unitaires de la politique de relance ; signature `PendingIntent` strictement limitée aux services et diffusions côté politique et appelant.

**Limite** : la reconnexion dépend des restrictions du système. Les notifications des applications tierces peuvent ne comporter aucun bouton direct de conversion de pas. Un statut « action envoyée » ne signifie pas « récompense créditée ». Ni le code Android ni Santé Connect ne peuvent attester d'un solde tiers sans preuve du fournisseur.


## v0.9 — Origine des actions vérifiée et installation mobile simplifiée

- **Sécurité :** avant de toucher à un `PendingIntent` contenu dans une notification WeWard, Macadam ou Winwalk, le pont exige que `getCreatorPackage()` soit exactement le package de la notification et que `getCreatorUid()` corresponde à `StatusBarNotification.getUid()`. Les informations sont fournies par Android, pas par le texte de la notification. L'action est ignorée si son origine est absente ou différente.
- **Diagnostic privé :** le code local `origine_action_non_verifiee` indique un refus ; aucune notification complète, identité de compte ou donnée de santé n'est transmise.
- **Ergonomie :** les boutons et diagnostics peuvent désormais défiler dans un `ScrollView` sur un petit écran Android. Correction de l'affichage du message d'accès Santé Connect.
- **Tests unitaires :** origine correcte, paquet falsifié/délégué, UID différent, profil Android secondaire et origine inconnue ; priorité du diagnostic en cas de refus.
- **Limite majeure :** cette sécurisation ne démontre pas l'existence de boutons de validation directs dans les applications tierces et **ne prouve aucun crédit**. Le module expérimental reste désactivé tant que l'utilisateur ne l'a pas autorisé dans Android et dans Validate Steps.

[Documentation Android sur l'identité d'un `PendingIntent`](https://developer.android.com/reference/android/app/PendingIntent#getCreatorPackage()) : le package et l'UID sont vérifiés par le système et identifient **l'émetteur du PendingIntent**, pas nécessairement l'application qui l'a relayé.


## v0.10 — Journal de preuve locale des déclenchements (Redmi / HyperOS)
- Journal borné à **80 événements** d'exécution, horodatés en heure locale avec décalage UTC. Pas de texte de notifications, pas de compte tiers, pas de secret, pas de localisation ni de nombre de pas enregistré dans ce journal.
- L'interface défilante affiche les **18 événements récents**, avec possibilité explicite de copier ce diagnostic dans le presse-papiers ou de le supprimer. Le presse-papiers Android n'est utilisé que sur action du propriétaire.
- Événements : alarmes principales et de reprise, watchdog en cours/ignoré/ayant mis une lecture en file, réarmements système, lectures démarrées/réussies/bloquées/à relancer et commandes de notification effectivement *envoyées*. **Une commande envoyée n'est pas une récompense créditée.**
- Grâce aux heures effectivement observées, il devient possible de diagnostiquer pourquoi une alarme n'a pas eu lieu à 22 h, si WorkManager a été retardé ou si Santé Connect a refusé la lecture.
- Tests unitaires purs : capacité maximale, historique récent, absence de données et refus des événements multi-lignes.
- Tout est local. Aucune autorisation réseau supplémentaire. Android peut retarder ou bloquer une exécution ; un événement qui n'a jamais été reçu ne peut pas être journalisé, et l'historique n'est pas une preuve de crédit chez les partenaires.

Utilisation : après une nuit de test sur le Redmi, ouvrir Validate Steps puis lire **HISTORIQUE D'EXÉCUTION**. Copier le journal technique pour analyser les heures si nécessaire.


## v0.11 — relances strictement bornées et signature de mise à jour stable

### Respect de l'échéance nocturne
La récupération des pas et les éventuelles actions de notification sont limitées à **22:00–23:40 (fin exclusive)**. Le planificateur n'inscrit plus d'alarme prévue hors de cette plage (par exemple 23:51 après un échec à 23:39). Les tests couvrent les dernières minutes avant la clôture. Cela évite un réveil inutile et supprime une incohérence entre programmation des relances et politique de validation.

### Accès aux notifications sur Redmi / Android 13+
Après installation manuelle d'un APK depuis GitHub, Android peut bloquer la permission d'accès aux notifications avec le message **« Paramètre restreint »**. Cela peut empêcher totalement notre service de recevoir les notifications des applications tierces.

1. Essayer d'accorder l'accès dans **Validate Steps → Autoriser l'accès aux notifications dans Android**.
2. Si Android refuse avec « Paramètre restreint », utiliser le nouveau bouton **Ouvrir les paramètres système de Validate Steps** ; dans la fiche de l'application, menu **⋮ → Autoriser les paramètres restreints** si Android le propose.
3. Retourner aux réglages d'accès aux notifications et accorder explicitement l'autorisation à Validate Steps, puis activer les actions directes dans l'application.

Cette permission est sensible : elle donne au service un accès large aux notifications du téléphone. L'application filtre les seuls fournisseurs autorisés sans conserver leur contenu, mais l'activation doit rester un **choix explicite** de l'utilisateur. Aucun contournement automatique de la protection d'Android.

Source : https://support.google.com/android/answer/12623953?hl=fr

### Mises à jour APK et certificat de signature : correction structurelle

Le `app-debug.apk` GitHub Actions est signé par une **clé de débogage créée par le runner**, qui peut changer d'une compilation à l'autre. Android **refuse d'installer une mise à jour si les certificats diffèrent**. Les APK debug restent disponibles pour les tests mais **ne constituent pas un canal de mises à jour fiables**.

La CI prend maintenant en charge **en option** un `validate-steps-release-signed` avec un certificat privé stable. La clé et les mots de passe sont conservés dans **GitHub Actions Secrets**, jamais dans Git, ni dans un artefact téléchargeable.

Configuration unique par le propriétaire du dépôt :

1. Générer et **sauvegarder hors du dépôt** une clé de signature Android avec Android Studio ou `keytool` (JDK 17) :
   ```bash
   keytool -genkeypair -v -keystore validate-steps-release.jks -alias validate-steps \
     -keyalg RSA -keysize 3072 -validity 10000
   ```
2. Dans GitHub → dépôt **Settings → Secrets and variables → Actions → New repository secret**, enregistrer :
   - `VS_KEYSTORE_B64` : contenu base64 du fichier .jks (Linux : `base64 -w0 validate-steps-release.jks`).
   - `VS_KEYSTORE_PASSWORD` : mot de passe du keystore.
   - `VS_KEY_ALIAS` : `validate-steps`.
   - `VS_KEY_PASSWORD` : mot de passe de la clé.
3. Après cette configuration, chaque `push` sur `main` produit, en plus du debug, **`validate-steps-release-signed`** sous GitHub Actions. La CI vérifie la signature avec `apksigner verify --print-certs`.
4. Installer **toujours cette variante stable** pour les prochaines mises à jour. Garder la clé originale dans un lieu sûr ; la perte de cette clé empêchera de mettre à jour l'installation avec la même identité.

**Migration depuis debug :** si le certificat de l'application actuellement installée diffère de celui de la version release, Android refusera le remplacement. Une **désinstallation puis réinstallation** peut être nécessaire **une fois**, ce qui efface les données locales et oblige à redonner les autorisations ; ne pas désinstaller avant d'avoir consulté ou copié le journal. Une fois la release stable installée, les futures mises à jour signées avec la même clé conservent les données.

Sans les quatre secrets, la CI continue de réussir et ne publie que le debug, sans prétendre garantir les mises à jour.

Sources :
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/build/building-cmdline


## v0.12 — Ne jamais confondre des points quelconques avec des récompenses de pas

Le pont de notifications demeure **expérimental** et facultatif. Une action telle que « récupérer mes pièces », « récupérer mes points », « collect coins » ou « collect rewards » **n'établit pas** qu'il s'agit de pas. La v0.12 refuse désormais ces libellés génériques, même s'ils proviennent d'une notification authentique et même si le système autorise leur exécution sans écran.

- Seules des formules qui mentionnent explicitement les **pas** (`pas`, `steps`, `step coins`) et qui figurent dans la liste blanche locale sont admissibles.
- Diagnostic `libelle_de_recompense_ambigu` pour un bouton générique, sans enregistrer son texte ou aucune donnée personnelle.
- Quand une notification contient plusieurs boutons, la classification la plus informative est conservée (plutôt que simplement celle du dernier bouton).
- Tests unitaires supplémentaires : refus d'actions génériques, normalisation des accents, classification et conservation du diagnostic.
- Ni simulation de pas, ni accès à des API privées, ni contournement de validation publicitaire ou de l'interface du fournisseur.

**État du but initial :** une commande envoyée en arrière-plan ne permet toujours pas de confirmer le crédit WeWard/Macadam/Winwalk. Aucun bouton officiellement documenté par les fournisseurs n'a été démontré compatible sur le téléphone. Les libellés génériques sont sciemment rejetés plutôt que de risquer des actions qui ne concernent pas les pas.


## v0.13 — Refonte premium et « Tout valider » à un appui (mode assisté)

### Interface graphique Android refondue
- Tableau de bord **sombre** natif, cartes arrondies, accents turquoise, hiérarchie lisible sur Redmi.
- Anneau de progression animé (objectif indicatif de 10 000 pas), **uniquement alimenté par la lecture Santé Connect**. L'objectif n'est pas un seuil universel des partenaires.
- Une carte pour chacun des quatre services : **WeWard, Macadam, Winwalk et Sweatcoin**. Détection d'installation et accès direct à l'application ou à sa fiche Google Play.
- Bouton central « **Tout valider** » et diagnostic du parcours assisté, distinct de l'option de notification en arrière-plan.
- Interrupteurs de relevé nocturne et d'actions de notifications, réglages Android regroupés, confirmation avant désactivation, historique local et copie volontaire.
- Source Java Android native, sans SDK web ni ressources distantes.

### « Tout valider » : assistance déterministe et volontaire

Cette option **ouvre les applications à l'écran** : elle n'est PAS une validation invisible. Elle est disponible seulement après **consentement explicite** et autorisation spéciale d'**Accessibilité** accordée dans Android.

Au démarrage demandé par l'utilisateur, un petit script local :
1. Ouvre WeWard, puis cherche **exactement** un bouton visible « Valider mes pas » ou « Valider les pas ». Tente le clic une seule fois.
2. Ouvre Macadam et cherche les libellés explicites de validation des pas. Aucun clic générique « Récupérer mes pièces ».
3. Ouvre Winwalk et recherche uniquement un libellé explicitement lié aux pièces issues des pas. Les annonces et offres publicitaires sont ignorées.
4. Ouvre Sweatcoin brièvement pour favoriser sa propre synchronisation. Il n'y a **aucun clic supposé créditer les Sweatcoins**.

Le service écoute les changements de contenu uniquement pour les **trois packages autorisés** (les interfaces des autres applications ne sont pas analysées). Les étiquettes vues ne sont ni sauvegardées ni envoyées ; seule une **tentative de clic** ou l'absence du bouton est enregistrée localement. L'utilisateur peut désactiver l'assistance dans Android à tout moment. Le parcours est limité dans le temps et n'utilise ni mot de passe, ni OCR, ni coordonnées, ni API privée, ni automatisation d'annonces.

**Résultat honnête :** le fait de cliquer ou même de trouver un bouton ne prouve pas que le serveur d'un fournisseur a crédité une récompense. Selon les versions installées, les boutons réels peuvent être différents ou absents. La validation totale ne peut être garantie sans observation des interfaces réelles et du solde effectivement obtenu. Certains fournisseurs imposent une validation manuelle et une automatisation peut enfreindre leurs conditions ; ne pas présenter cette fonction comme officiellement supportée par leurs éditeurs.

### 22 h : le mode discret existant est conservé
L'alarme de 22 h continue de **relever les vrais pas** et de tenter uniquement les actions de notifications directes, strictement filtrées et activées par consentement. Elle **ne lance pas** le service d'assistance ni les applications sur un écran verrouillé. Android impose des restrictions aux ouvertures d'activité en arrière-plan.

### Autorisation et publication
L'API Accessibilité est sensible et n'est **pas** déclarée à tort comme « outil d'accessibilité » (`isAccessibilityTool=false`). Avant diffusion Google Play, le développeur doit effectuer la déclaration AccessibilityService, fournir une divulgation claire et recueillir le consentement, en plus de respecter les règles de Google Play et des applications partenaires.

Sources :
- https://developer.android.com/guide/topics/ui/accessibility/service
- https://support.google.com/googleplay/android-developer/answer/10964491?hl=fr
- https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses
- https://help.macadam.app/fr/articles/469049-comment-fonctionnent-la-validation-des-pas-et-les-recompenses


### Raccourci de la notification de 22 h

Si les notifications Android sont autorisées et que la lecture de pas du jour aboutit pendant la fenêtre 22:00–23:40, la notification affiche **« Tout valider · assisté »**.

- Le **clic sur cette action** est un geste explicite de l'utilisateur ; elle peut démarrer le parcours assisté **en un appui** si l'autorisation Accessibilité a déjà été accordée et que le consentement applicatif a été accepté.
- Le clic sur le **texte de la notification** ouvre seulement le tableau de bord, sans cliquer sur un fournisseur.
- L'action contient un **jeton aléatoire à usage unique**, vérifié par l'application avec contrôle du jour local, de la fenêtre de récupération et d'une validité maximale de deux heures ; une simple ouverture externe de l'activité ne peut pas lancer la séquence.
- Si l'autorisation manque, l'application affiche la configuration nécessaire. Elle ne contourne jamais les restrictions du système.
- Une ouverture ou un clic assisté n'est **pas** une preuve de crédit. Les notifications tierces peuvent proposer zéro action directe compatible.

Le moteur assisté refuse un second parcours simultané et limite la fréquence des inspections de l'arbre d'accessibilité pour réduire la charge CPU.

 
## v0.14 — Bouton « Tout valider » hybride et tableau de bord d'autorisations

### Un seul appui, deux moyens techniques réellement distincts

1. Depuis l'écran de Validate Steps, appuyer sur **« Tout valider »**. Si l'accès Android aux notifications est accordé et que le listener est connecté, le système recherche et envoie **immédiatement** les actions directes publiées par WeWard/Macadam/Winwalk, **même en dehors de 22 h**. Il n'est pas nécessaire d'activer la surveillance nocturne pour cette tentative ponctuelle.
2. Les vérifications restent strictes : le fournisseur doit être sur la liste blanche, son UID doit correspondre au propriétaire du `PendingIntent`, le bouton doit mentionner explicitement les **pas**, ne doit pas ouvrir directement une activité, ni demander une saisie ni déclencher une publicité.
3. Si l'utilisateur a également autorisé l'assistance Accessibilité et en a accepté le fonctionnement, **le même appui** peut démarrer le parcours guidé dans les applications restantes. Une action de notification déjà envoyée aujourd'hui ne déclenche **pas** un second clic assisté.
4. Si Android ne fournit pas de service ou de bouton compatible, l'écran explique quelle permission manque et permet d'ouvrir ses réglages. La méthode guidée requiert un écran déverrouillé et ouvre les applications ; elle **n'est pas invisible**.
5. Chaque service expose son propre statut local. **« Action envoyée » signifie seulement que le système Android a accepté la commande, pas que le fournisseur a crédité ses récompenses**. Les services peuvent modifier leurs interfaces ou ne proposer aucune action éligible.

### Exécution automatique à 22 h

La programmation existante reste distincte : lecture Santé Connect par alarme à **22:00**, secours WorkManager à **22:20** et **23:15**, et actions directes de notifications uniquement dans la fenêtre **22:00–23:40** après activation volontaire. Aucun parcours Accessibilité n'est ouvert par une tâche de fond ; Android ne garantit ni écran déverrouillé ni possibilité de lancer les interfaces de services depuis le fond.

### Interface Android

- Nouveau groupe **« Prêt à valider ? »**, quatre tuiles d'état cliquables (Santé Connect, alarme à 22 h, accès aux notifications et assistance à un appui).
- Mise en route ciblée par permission manquante, sans recherches manuelles dans les paramètres.
- Retour explicite du nombre de commandes directes envoyées depuis le bouton principal. **Ce nombre ne représente pas des gains.**
- Toujours : tableau de bord sombre natif, anneau de pas réels, fiches individuelles des quatre services, historique privé et diagnostics.
- Tests unitaires couvrant les règles du lancement ponctuel, les libellés ambigus et le maintien strict des limites nocturnes.

### Validation encore nécessaire

Les actions réellement exposées sur ton Redmi par chaque application doivent être vérifiées en situation. Sans API officielle de crédit, **il n'existe pas de preuve universelle de validation automatique des récompenses** ; un clic Accessibilité peut échouer ou être refusé et l'envoi d'un `PendingIntent` ne prouve pas un gain.


## v0.15 — Assistance Android plus robuste et commande d'arrêt

La procédure « Tout valider » reste **assistée**, uniquement sur un appui explicite de l'utilisateur. L'assistance Accessibilité peut chercher et appuyer sur les **boutons visibles** et reconnus de WeWard, Macadam et Winwalk ; elle ne peut pas confirmer un crédit côté fournisseur. Sweatcoin dispose de sa propre conversion des pas.

### Corrections

- La recherche des boutons visibles est désormais limitée à **400 nœuds par événement**, profondeur maximale 20 et jusqu'à 60 enfants par nœud. La version précédente limitait seulement la profondeur et pouvait parcourir un nombre très élevé d'éléments sur une interface complexe.
- Une vue qui disparaît, un arbre Android momentanément indisponible ou un clic rejeté par le système ne doit plus provoquer la fermeture brutale du service d'assistance.
- Si le service Accessibilité est **déconnecté**, les références au service sont réinitialisées et les relances en attente sont annulées.
- Un bouton **« Arrêter le parcours en cours »** s'affiche dans le tableau de bord pendant l'assistance. L'arrêt annule les actions futures et laisse un statut non confirmé, jamais un crédit inventé.
- **Avant 22 h**, « Tout valider » affiche un avertissement : les applications qui ne permettent qu'une validation quotidienne risquent de ne pas comptabiliser les pas encore effectués. L'utilisateur choisit de valider immédiatement ou de programmer le relevé à 22 h. Programmer le relevé n'active pas automatiquement les droits de notification manquants.
- Tests unitaires de limite globale de parcours et de la règle horaire.

**Limites inchangées :** Android peut limiter l'accès aux notifications ou à l'Accessibilité ; l'utilisation des interfaces tierces n'est pas une validation en arrière-plan. Les actions de notifications à 22 h ne fonctionnent que si les fournisseurs les exposent ; elles ne sont pas une preuve de gain réel. Aucun CAPTCHA, publicité ou contrôle d'identité n'est contourné.


## v0.16 — Reprise dès que Santé Connect est de nouveau autorisé

Lorsqu'une lecture automatique nocturne échouait pour `permission_missing`, le système arrêtait les secours de cette nuit, ce qui était approprié **tant que l'accès restait refusé**. Le redonner depuis les réglages Android ne relançait toutefois pas nécessairement le relevé du jour.

Cette version détecte le retour des autorisations `READ_STEPS` et `READ_HEALTH_DATA_IN_BACKGROUND` à l'ouverture du tableau de bord ou au retour du dialogue Android :

- Seulement si le relevé à 22 h a été activé, que le dernier échec du **jour courant** était `permission_missing`, et qu'il est encore entre **22 h et 23 h 40**.
- Insère **un seul** audit Santé Connect du jour dans la file WorkManager. Plusieurs passages par le tableau de bord n'engendrent pas de doublons.
- Ajoute `AUDIT_PERMISSION_RECOVERED` au journal privé, pour expliquer pourquoi une lecture complémentaire a été lancée.
- Ne déclenche **aucun** bouton ou parcours de validation dans les applications tierces à cette occasion. Un relevé réussi ne confirme pas un crédit WeWard/Macadam/Winwalk.
- Tests unitaires pour les autorisations absentes, le service désactivé, la fenêtre expirée, l'état déjà récupéré, et les erreurs n'étant pas des refus de permission.

Les travaux Android restent soumis à la planification système et peuvent être retardés par HyperOS ; aucun accès ni crédit n'est fabriqué.


## v0.17 — Compteur fiable après minuit et interface sans états trompeurs

Un problème de fraîcheur pouvait faire apparaître des pas de la veille sous l'en-tête de la nouvelle journée : les lectures de Santé Connect sont asynchrones, les permissions peuvent être retirées et plusieurs actualisations peuvent se terminer dans un ordre différent de leur démarrage.

La version 0.17 applique des règles strictes de **fraîcheur d'affichage**, sans modifier ni fabriquer les données de Santé Connect :

- Un résultat n'est affiché que s'il correspond **au jour local courant**, indique `steps_read` et contient un nombre de pas valide (zéro accepté).
- Une réponse tardive d'une ancienne requête ne peut pas écraser celle d'une actualisation plus récente. Une réponse d'hier arrivant après minuit est ignorée.
- Si Santé Connect devient indisponible, ou si une lecture échoue, le compteur et l'anneau reviennent à **« — »**, et non à un ancien total éventuellement erroné.
- À **minuit local**, lorsque l'écran est ouvert, la date, les indicateurs et les pas sont actualisés sans scrutation permanente. La planification visuelle est arrêtée lorsque l'interface passe à l'arrière-plan.
- Les anciennes animations de l'anneau sont annulées lors d'un nouveau résultat ; le mode logiciel forcé est supprimé pour permettre le rendu Android normal.
- L'état positif Santé Connect est maintenant libellé **« Accès aux pas autorisé »** (ancien libellé erroné corrigé).
- Les quatre tuiles d'autorisations peuvent s'agrandir avec les réglages de police Android. Les diagnostics des notifications ne déclarent plus qu'aucune action n'existe lorsque cette information n'est pas connue.
- Le bandeau nocturne décrit précisément le **relevé des pas**, avec avertissement si l'accès nécessaire manque. Il n'annonce pas de crédit partenaire automatique.

La règle de fraîcheur fait l'objet de tests unitaires (jour courant, changement de date, lecture échouée, valeur manquante, réponse de requête dépassée). Elle n'interfère pas avec l'alarme de 22 h ni avec le parcours volontaire « Tout valider ».

**Important :** l'interface ne lit pas les soldes serveurs des quatre fournisseurs. Une lecture de pas fiable et une commande Android envoyée restent distinctes d'une récompense réellement validée.


---

## v0.18 — Audit professionnel des 15 concepts fondamentaux

Cette matrice lie **chaque concept à son implémentation réelle dans le dépôt**. « Présent » signifie que le code et des protections sont en place ; **cela ne signifie pas que les serveurs des applications partenaires ont crédité des récompenses**.

| # | Concept | Implémentation / garantie | Limite de vérification |
|---|---|---|---|
| 1 | Programmation fiable 22 h | `DailyScheduler`, `BootActionPolicy`, `BootReceiver` ; signal `ALARM_SCHEDULE_FAILED` si le système refuse une alarme | HyperOS peut différer des tâches |
| 2 | Secours et reprise | `WatchdogWorker` à 22 h 20 / 23 h 15 ; `AuditWorker` avec données datées ; récupération après accord Santé Connect | L'heure exacte n'est pas garantie par WorkManager |
| 3 | Un appui utilisateur | `MainActivity.onClaimAll` ; commande directe si disponible, puis parcours assisté facultatif | Les fournisseurs peuvent ne pas exposer de bouton |
| 4 | Anti-doublons | `ActionBridgeState`, `ClaimRetryPolicy`, nouveau `ProviderCreditStore` : aucune deuxième tentative après réception déclarée | Ne constitue pas une preuve serveur |
| 5 | Origine des actions | `ActionOriginPolicy` compare package et UID du `PendingIntent` et de la notification | Actions tierces soumises aux politiques des fournisseurs |
| 6 | Assistance contrôlée | `GuidedClaimService`, `GuidedClickSafetyPolicy` : propriétaires des vues vérifiés, parents proches seulement | Nécessite utilisateur, écran et permission Accessibilité |
| 7 | Permissions explicites | Tuiles et réglages Android + `HealthPermissionRecoveryPolicy` ; jamais de contournement | L'utilisateur peut refuser l'accès |
| 8 | Validité des pas | `HealthReader`, `StepValuePolicy` : aucune valeur négative ou absente validée, pas de simulation | Les pas dépendent des données Santé Connect |
| 9 | Dates et changement de jour | `TimePlanner`, `AuditDay`, `StepDisplayPolicy`, tests DST ; retour à zéro du compteur au changement de jour | Dépend de l'horloge locale du téléphone |
| 10 | Cycle de vie Android | **Nouveau :** parcours Accessibilité désarmé en cas de recréation/connexion du service ; anciens clics non rejoués | Nécessite des tests sur appareil |
| 11 | Performance énergétique/UI | `AccessibilityScanBudget` (400 nœuds maximum par événement), limitation des labels, animations `StepRingView` annulables | Autonomie variable selon appareil |
| 12 | Tableau de bord | Indicateurs de permissions, anneau, quatre cartes, nouvel état de crédits déclarés 0/4 | Les états reflètent des faits locaux |
| 13 | Preuves et état réel | **Nouveau :** `CreditEvidencePolicy` + `ProviderCreditStore` : reçu / non reçu / non vérifié, déclarés manuellement pour la journée | **Aucune confirmation automatique des soldes** |
| 14 | Confidentialité et diagnostics | Aucune permission INTERNET, historique borné et local, libellés filtrés, `NotificationNoncePolicy` (32 caractères), `ProviderEventCode` fixe | Clipboard/journal sous le contrôle du téléphone |
| 15 | Livraison, tests et signature | CI `lintDebug` + `testDebugUnitTest` + `assembleDebug` + `assembleRelease`; signature stable optionnelle par secrets GitHub | Signature stable indisponible tant que les secrets ne sont pas configurés |

### Fonctions nouvelles ou renforcées en v0.18

- **Preuve déclarative quotidienne par application** : sur la carte WeWard, Macadam, Winwalk ou Sweatcoin, « Vérifier / déclarer le crédit » permet de choisir *récompenses reçues*, *non reçues* ou *effacer ma déclaration*. Une double confirmation explicite est exigée avant d'enregistrer « reçues ». Aucun solde, compte ou montant n'est interrogé.
- **Résumé `0/4`** : l'accueil compte uniquement les confirmations humaines du **jour actuel** et les présente comme « déclarées reçues », jamais comme des crédits serveur automatiquement vérifiés.
- **Anti-revalidation** : les actions de notifications et les clics assistés ignorent aujourd'hui les partenaires déjà déclarés reçus par l'utilisateur. Démarrer un nouveau parcours réinitialise les statuts temporaires d'anciennes tentatives, sans effacer les déclarations de crédit.
- **Relance Accessibilité sécurisée** : après redémarrage du processus ou reconnexion du service, aucune ancienne session persistée ne réarme les clics. Une action explicite est à nouveau nécessaire.
- **Contrôles de l'interface tierce** : limitation à deux niveaux de parents cliquables et vérification stricte du package du bouton. Les textes inattendus de plus de 100 caractères sont ignorés sans normalisation.
- **Données Santé Connect** : les agrégats négatifs/absents sont rejetés avant de pouvoir être marqués `steps_read` ; stockage et affichage partagent la même règle.
- **Résilience horaire Android** : réarmement limité aux broadcasts attendus ; les alarmes rejetées et les notifications devenues inaccessibles sont consignées sans invalider la lecture Santé Connect achevée.
- **Journal par fournisseur** : pour les événements pertinents, seuls les codes fixes `WEWARD`, `MACADAM`, `WINWALK`, `SWEATCOIN` accompagnent l'événement. Pas de texte de notification, de numéro de compte ou de données de pas dans l'historique.
- **Sécurité des notifications** : un jeton d'action expiré, mal formé ou trop long est refusé ; les Intent mal formés ne doivent pas faire planter le tableau de bord Android.

### Politiques Google Play et limites de validation

L'assistance Accessibilité n'est jamais destinée à contourner des publicités, CAPTCHA ou protections des fournisseurs. Elle exige une divulgation claire et un consentement explicite, et se limite à un enchaînement déterministe initié par l'utilisateur. Une déclaration d'utilisation de l'API Accessibilité est nécessaire pour une publication sur Google Play. Voir : https://support.google.com/googleplay/android-developer/answer/10964491?hl=fr

**Objectif encore ouvert** : constater des **crédits effectifs** sur les quatre comptes. Une action envoyée, un clic tenté et une lecture des pas ne garantissent pas un crédit. Aucun partenaire n'est contacté, aucune interface privée ni API non documentée n'est employée.


## v0.19 — Durcissement de la lecture asynchrone et des notifications

Cette version complète les **15 piliers qualité** de la v0.18 avec deux correctifs transversaux :

1. **Résultat de Santé Connect immuable par tentative.** La lecture asynchrone passe par `AuditReadGate` : un seul résultat est accepté, une fois le délai de 25 secondes écoulé le résultat est figé, et un callback tardif ou dupliqué ne peut plus modifier les valeurs persistées. Un code de succès contenant un nombre de pas négatif ou absent est converti en `invalid_steps_data`. Une interruption ferme aussi la tentative proprement. Les tests couvrent succès, zéro pas, délai expiré, doublon, valeur invalide et exception.
2. **Inspection bornée des notifications.** `RewardActionListener` filtre désormais explicitement les fournisseurs autorisés avant de traiter une action. Il n'inspecte pas plus de **256 notifications issues de ces fournisseurs par passage**, même si Android en renvoie davantage. Les notifications d'autres applications ne sont pas ouvertes pour classification ; aucun contenu tiers n'est enregistré. La limite dispose de tests dédiés.

Ces corrections ciblent les piliers fiabilité, précision, gestion des erreurs, anti-doublons, performances, batterie, sécurité, confidentialité et tests. Les autres piliers déjà traités en v0.18 sont conservés.

**Limite :** les tests de logique et le build Android ne prouvent pas que les fournisseurs créditent des récompenses. Une action Android envoyée reste un essai, et les déclarations de crédits sont uniquement celles de l'utilisateur après consultation de chaque application.


## v0.20 — Blocage des notifications causées par une alarme Android périmée

Android peut retarder les alarmes ou WorkManager à cause de Doze et des politiques d'économie d'énergie du fabricant. Un secours daté d'hier peut donc être livré à une heure où **les actions de récompense d'aujourd'hui sont admissibles** (22 h à 23 h 40). Auparavant, les anciens récepteurs rétablissaient/inspectaient l'accès aux notifications **avant** de contrôler la date réellement programmée.

La v0.20 corrige cette distinction :

- Nouvelle règle testée `ScheduledActionPolicy` : une inspection **déclenchée par un travail planifié** exige que la date du déclencheur soit exactement celle du jour local actuel, que la planification soit activée et que l'heure soit dans la fenêtre 22:00–23:40.
- `NightlyReceiver` et `WatchdogWorker` appliquent cette règle **avant de reconnecter le service Android ou d'inspecter les notifications**.
- Si un travail ancien se réveille en retard, le journal enregistre `SCHEDULED_ACTION_IGNORED` et le planning du lendemain est quand même réarmé si l'automatisation reste activée.
- Une ancienne **lecture des pas** peut toujours garder sa propre date dans la file WorkManager selon les règles existantes, sans déclencher pour autant des commandes de récompense pour le mauvais jour.
- Le mécanisme `ActionBridgeState.reserve()` protège maintenant la lecture/vérification/écriture de réservation par un verrou commun. Deux appels Android presque simultanés ne peuvent plus tous deux réserver la même action dans le processus.
- Tests unitaires : 22 h exactement, 23 h 39, fin exclusive à 23 h 40, alarmes d'hier/de demain, rediffusions après minuit, autorisation désactivée, données nulles.

**À distinguer :** les actions directes lancées volontairement par le bouton « Tout valider » conservent leur logique indépendante de l'heure ; les notifications nouvelles réellement émises par un fournisseur conservent le contrôle de consentement et la fenêtre nocturne. Aucun mécanisme d'Accessibilité n'est exécuté sans geste utilisateur, aucune pièce n'est déclarée créditée sur la seule base d'un `PendingIntent`.

Documentation Android sur les retards : https://developer.android.com/training/monitoring-device-state/doze-standby et https://developer.android.com/develop/background-work/services/alarms.


## v0.21 — Rapport quotidien par application, sans fausse confirmation

Le tableau de bord comporte désormais un **« Bilan du jour »**, calculé à la demande à partir de données déjà disponibles en local, pour **WeWard, Macadam, Winwalk et Sweatcoin**. Un bouton permet de copier ce bilan dans le presse-papiers du smartphone ; un second permet de l'actualiser.

### Quatre états distincts, jamais confondus

1. **Lecture Santé Connect :** réussite, lecture en cours, absence d'autorisation, ou relevé indisponible.
2. **Commande de notification :** aucune, réservée, annulée, en échec, ou `PendingIntent` envoyé à Android (ce dernier état n'est **pas** une récompense créditée).
3. **Parcours assisté :** aucun, recherche en cours, clic de bouton de pas tenté, clic refusé, bouton absent, application absente, ouverture bloquée, application ouverte pour synchronisation, parcours interrompu ou saut anti-doublon.
4. **Déclaration de l'utilisateur :** réception déclarée après consultation de l'application, non-réception déclarée ou non-vérifié. Ces déclarations **ne constituent pas** une attestation du fournisseur.

Les données du rapport sont **scindées par date locale**. Un événement d'hier ne peut jamais être présenté comme un clic ou un crédit d'aujourd'hui. Les anciens états textuels restent consultables mais ne sont pas promus artificiellement au rang de clic constaté ; seul un nouveau code de résultat structuré est pris en compte après cette mise à jour.

### Protection des données et comportement

- Aucun accès Internet, aucun identifiant de compte, aucune lecture des soldes ou des messages privés.
- Les statuts de parcours sont des codes fixes de type `GuidedOutcome`, non le texte arbitraire d'une notification.
- Le rapport affiche aussi la raison observée pour une notification non utilisable : aucun bouton, bouton ouvrant une interface, origine non vérifiée, libellé ambigu, saisie requise ou autre type non reconnu. Seuls des **codes fixes prédéfinis** sont interprétés ; le contenu des notifications n'est pas copié.
- Un clic assisté réellement tenté est retenu **pour le jour courant**, même si une relance ou une interruption remplace ensuite le dernier état du parcours. Cela empêche de sous-déclarer des tentatives sans jamais prétendre qu'elles ont été créditées.
- Le rapport copié contient les types d'actions et les déclarations, **pas le nombre individuel de pas**.
- Les compteurs affichent séparément le nombre de commandes envoyées, de clics assistés tentés, de réceptions déclarées, de non-réceptions déclarées et de cas non vérifiés.
- Aucun déclenchement automatique, clic ou déclaration ne peut être présenté comme une preuve serveur.
- Tests unitaires de la classification des tentatives et de la distinction entre crédit déclaré et crédit jamais vérifié par l'application.

**Limite importante :** le bilan résume ce que le téléphone a réellement observé et ce que l'utilisateur a déclaré. Sans preuve côté fournisseur, il est impossible de certifier automatiquement que toutes les récompenses ont été encaissées.


## v1.0.0 — Base stable, historique de 30 jours et contrôles de publication

Cette version consolide l'architecture Android, les 15 piliers de qualité et les corrections des v0.3 à v0.21. L'objectif **stable** signifie ici : code compilé, tests JUnit et lint réussis, politiques de confidentialité et de récupération contrôlées, et traçabilité des opérations locales. Cela ne constitue **pas** une certification d'encaissement des applications tierces sur un Redmi réel.

### Historique réel sur 30 jours

Les précédentes versions ne gardaient que les dernières valeurs de chaque application. La v1.0 ajoute un archiveur local qui enregistre chaque jour, au moment de son événement :

- le résultat technique du relevé Santé Connect (`steps_read`, blocage, indisponibilité ou erreur générique) **sans stocker le nombre de pas dans l'archive** ;
- le code de diagnostic d'une action de notification (signal compatible, non reconnu, ancien, ouvrant une interface, libellé ambigu, origine non vérifiée) ;
- les actions directes effectivement envoyées, annulées, refusées ou dont le résultat reste inconnu ;
- l'état de l'assistance Accessibilité et le fait qu'un clic sur un bouton de pas ait **été tenté** au moins une fois dans la journée ;
- les déclarations personnelles « reçu » et « non reçu », sans jamais les présenter comme preuves certifiées du fournisseur.

Les entrées sont enregistrées immédiatement sous la **date locale de l'événement**, même si les états actuels sont réinitialisés le lendemain. Le stockage se limite aux **30 dernières dates locales** avec suppression automatique des événements périmés. Seuls des codes internes sur liste blanche sont archivés : pas de mot de passe, solde, identifiant de compte, texte des notifications ni connexion Internet.

Dans le tableau de bord, **Rapports précédents → Choisir un jour** ouvre un sélecteur de dates, puis un bilan détaillé et copiable. **Effacer archives** efface seulement ces nouveaux bilans après confirmation, sans désactiver les alarmes ou les autorisations. Les bilans avant l'installation de la v1.0 ne peuvent pas être reconstitués rétroactivement si les anciennes données avaient déjà été remplacées.

### Publication et limites de la version stable

- CI : `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, vérification cryptographique de la signature de l'APK debug avec `apksigner`, empreinte SHA-256.
- Artefact `validate-steps-debug` : **APK de test installable**, signé avec une clé de debug qui peut changer d'une exécution à l'autre. Les mises à jour d'APK exigent le **même certificat de signature**.
- Artefact `validate-steps-release-signed` : créé **uniquement** si une clé privée de publication stable a été fournie dans les secrets protégés GitHub ; elle n'est jamais inscrite dans le code ou la documentation.
- Une exécution manuelle du workflow peut exiger une version signée de production : elle **échoue volontairement** si les quatre secrets ne sont pas configurés, au lieu d'annoncer une fausse version prête à publier.
- La version stable ne remplace pas une **recette réelle** sur le Redmi Android 15 : il faut vérifier le déclenchement nocturne, le fonctionnement après verrouillage/redémarrage et l'état de chacun des quatre partenaires.

Voir [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) avant toute diffusion comme APK de production.

### Contrat fonctionnel définitif de la v1.0

| Fonction | Engagement |
|---|---|
| Pas réels Santé Connect | Lecture locale, jamais simulation ou doublon volontaire |
| Relevé à 22 h | Alarme et secours, selon les autorisations et les contraintes d'Android |
| Bouton direct manuel | Tente les actions de notifications reconnues, **sans jamais lancer Accessibilité** |
| Validation à 22 h sans écran | Seulement quand un fournisseur fournit une action directe de notification admissible |
| Encaissement WeWard/Macadam/Winwalk | **Non garanti** sans confirmation dans les applications |
| Sweatcoin | Synchronisation et crédit gérés par Sweatcoin |
| Historique | 30 jours de codes locaux, et non de soldes ou de données sensibles |
| Confidentialité | Pas de permission Internet, accès Android demandé explicitement |

Aucun logiciel ne peut promettre une version « définitive » au sens d'impossible à corriger : de futurs changements d'Android, d'HyperOS ou des fournisseurs peuvent nécessiter des mises à jour.


## v1.1 — Mode « Sans assistance » à 22 h

L'écran principal et le raccourci de la notification Android **n'ouvrent plus le parcours Accessibilité**. Le nouveau bouton **« Activer le mode 22 h · sans assistance »** active, après autorisation explicite de l'accès Android aux notifications, l'alarme quotidienne, les deux secours WorkManager et l'envoi des **actions directes** que WeWard, Macadam ou Winwalk publient réellement sous forme de `Notification.Action` admissible.

- **Préparation initiale :** accorder l'accès Santé Connect pour le relevé des pas, les alarmes exactes pour la ponctualité et **l'accès aux notifications** pour les boutons directs. Android accorde un accès sensible aux notifications ; Validate Steps ne conserve que des codes locaux et filtre strictement les trois packages concernés.
- **Chaque jour :** tentative à 22 h, nouvelle inspection des notifications connectées, secours à 22 h 20 et 23 h 15. Pas besoin d'ouvrir Validate Steps ni d'activer Accessibilité, tant qu'Android autorise les services à fonctionner.
- **Manuellement :** « Essayer sans assistance » tente uniquement les boutons de notification exposés et clairement liés aux pas, même en dehors de 22 h après avertissement avant l'heure. Il ne lance **aucun clic de service Accessibilité**, vidéo récompensée, connexion de compte ou action sur écran caché.
- **Suivi honnête :** les cartes WeWard, Macadam, Winwalk et Sweatcoin montrent séparément les commandes envoyées, les actions de notification non compatibles, les absences d'observation, les déclarations de crédit et les restrictions connues. Aucun `PendingIntent` envoyé n'est considéré comme preuve de gain.
- L'ancien parcours **assisté** reste volontairement accessible uniquement depuis les paramètres avancés pour ceux qui souhaitent l'utiliser ; il n'est requis ni pour le mode nocturne ni pour le bouton direct.

### Limitation incontournable des fournisseurs

**Cette version ne peut pas garantir de crédit automatique chez WeWard, Macadam et Winwalk.** Leurs mécanismes appartiennent à leurs applications : WeWard requiert de l'ouvrir et d'appuyer sur « Valider mes pas » ; Macadam requiert une validation manuelle avant minuit ; Winwalk offre une collecte automatique après activation d'une vidéo quotidienne dans sa propre application. Aucun `PendingIntent` utilisable pour créditer leurs comptes n'a encore été confirmé sur l'appareil réel. Sweatcoin gère sa conversion séparément.

**Sources :**
- https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses
- https://help.macadam.app/fr/articles/264329-qu-est-ce-que-l-application-macadam
- https://play.google.com/store/apps/details?id=com.winwalk.android

Les validations qui ne sont pas exposées comme actions Android directes restent donc à effectuer **dans les applications elles-mêmes**. Ce n'est pas une limitation qu'une programmation d'alarme ou une simple lecture Santé Connect peut supprimer.
