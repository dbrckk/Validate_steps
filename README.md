# Validate Steps (v0.12) — 22 h autonome, sans interface quotidienne

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
