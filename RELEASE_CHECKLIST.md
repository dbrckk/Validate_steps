# Validate Steps v1.1 — Recette de stabilité Android

**Statut :** une base de code peut être validée par CI sans être certifiée sur un téléphone réel. Les quatre applications de récompenses conservent leur propre logique de crédit.

## 1. Compilation et livraison

- [ ] `main` est sur le commit v1.1 validé et tous les contrôles GitHub Actions sont verts : lint, tests unitaires, compilation APK debug et release.
- [ ] L'artefact `validate-steps-debug` contient l'APK de test et `SHA256SUMS.txt`. Le contrôle `apksigner verify` a réussi.
- [ ] Ne **pas** présenter le debug APK comme un APK Play Store ou une version signée pour la distribution.
- [ ] Pour produire un APK de publication réutilisable, créer une **clé de signature privée durable**, la conserver hors du dépôt, et enregistrer ces secrets GitHub : `VS_KEYSTORE_B64`, `VS_KEYSTORE_PASSWORD`, `VS_KEY_ALIAS`, `VS_KEY_PASSWORD`.
- [ ] Dans GitHub → Actions → Android APK → Run workflow (sur **main**), cocher `publish_signed`. Le workflow échoue si les secrets sont absents et publie `validate-steps-release-signed` uniquement après vérification de signature.
- [ ] Garder le même keystore pour les versions ultérieures ; Android refuse une mise à jour signée avec une clé différente. Ne jamais inclure la clé dans un ticket, commit ou message.
- [ ] Vérifier les exigences du Play Store séparément (fiche, captures réelles, règles d'Accessibilité et de données de santé, confidentialité). Une compilation APK seule ne suffit pas à la publication.

## 2. Essais sur Redmi 15 5G / Android 15

- [ ] **Ne désinstallez pas l'application précédente sans export des diagnostics utiles** : `android:allowBackup=false` et la désinstallation efface ses données privées.
- [ ] Depuis le tableau de bord, autoriser Santé Connect : lecture des pas et lecture en arrière-plan. Vérifier des nombres réels, y compris le zéro valide.
- [ ] Autoriser les alarmes exactes. Vérifier l'état visible « RELEVÉ DES PAS À 22 H · PROGRAMMÉ ». Sans ce droit Android peut retarder l'exécution.
- [ ] Activer la lecture quotidienne à 22 h. Vérifier sous HyperOS les restrictions de batterie, le démarrage automatique et les optimisations possibles.
- [ ] Pour le mode sans assistance : accorder l'accès aux notifications dans Android (permission sensible), puis toucher **Activer le mode 22 h · sans assistance**. L'Accessibilité n'est **pas nécessaire**.
- [ ] Tester le bouton **Essayer sans assistance** : aucune ouverture d'app fournisseur ni lancement du service Accessibilité. Vérifier les cartes et les actions directes réellement présentes.
- [ ] Confirmer que le bilan indique **« action envoyée »** ou **« clic tenté »** sans afficher « récompense confirmée ».
- [ ] Après consultation directe de WeWard, Macadam, Winwalk et Sweatcoin, déclarer le crédit reçu/non reçu et vérifier son statut distinct.
- [ ] Tester un relevé de 22 h **avec écran verrouillé**, au moins une nuit, et vérifier le journal d'audit après 22 h 20 et 23 h 15.
- [ ] Tester redémarrage/reconnexion et retour de permission Santé Connect ; vérifier l'absence de clic ou de crédit indu.
- [ ] Vérifier après minuit que les pas de la veille et les déclarations d'hier ne sont pas affichés comme des succès du jour.
- [ ] Vérifier « Rapports précédents » : présence de la journée enregistrée, sélection de date, copie du bilan, suppression uniquement après confirmation et horizon de 30 jours.
- [ ] Vérifier que la police Android agrandie n'empêche pas l'accès aux réglages et boutons.

## 3. Signaux de diagnostic d'incident

- `AUDIT_OK` : Santé Connect a rendu un total pour la date ; **aucun crédit partenaire prouvé**.
- `AUDIT_BLOCKED` : permission ou environnement Android indisponible.
- `SCHEDULED_ACTION_IGNORED` : un déclencheur périmé ou hors fenêtre n'a pas relancé de commandes.
- `CLAIM_ACTION_DISPATCHED` : Android a accepté le PendingIntent, **pas une preuve d'encaissement**.
- `GUIDED_CLICK_ATTEMPTED` : un clic Android a été tenté dans une app partenaire ; une vérification dans cette app reste nécessaire.
- `USER_CREDIT_REPORTED_RECEIVED` : **l'utilisateur** affirme avoir vu le crédit ; l'application n'a pas accès aux relevés serveur du fournisseur.

## 4. Limite de garantie

**Ce qui peut être certifié par la CI :** compilation, contrôles statiques, tests de politiques, signature de l'APK produit et présence de l'artefact.

**Ce qui exige un test réel :** exactitude/ponctualité du relevé sous HyperOS, services Android maintenus en arrière-plan et attribution de récompenses par les quatre fournisseurs.

La base v1.0 n'envoie aucune requête privée aux fournisseurs, n'invente pas de pas et n'exécute pas les parcours d'Accessibilité sans geste volontaire.


## 5. Recette spécifique v1.1 « Sans assistance »

- [ ] Activer le mode 22 h depuis l'application **sans accorder l'autorisation Accessibilité**. Vérifier que l'application montre « Mode programmé » puis « Service connecté » une fois Android raccordé.
- [ ] Sans permission de notification, l'activation doit expliquer le caractère sensible de cet accès et renvoyer vers les paramètres Android, sans déclarer l'automatisation directe activée.
- [ ] Vérifier que le bouton principal **Essayer sans assistance** et le raccourci de la notification ne démarrent jamais `GuidedClaimService`.
- [ ] Vérifier les fournisseurs installés : les états inconnus ou les libellés ambigus ne sont jamais présentés comme crédités.
- [ ] Confirmer sur le Redmi que la connexion du service de notifications persiste en arrière-plan avec les restrictions HyperOS configurées.
- [ ] Après une nuit, vérifier le rapport du jour et les soldes réels dans les applications ; noter les boutons directs effectivement proposés.
- [ ] Ne publier aucune garantie de crédit WeWard/Macadam/Winwalk sans preuve d'intégration officielle ou confirmation côté compte.
