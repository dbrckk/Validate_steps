# Validate Steps v1.1.1 — Recette sans appui

**Portée :** relevé réel de pas à 22 h, commandes directes éventuellement émises par les notifications des fournisseurs, **aucune permission Accessibilité**. La compilation réussie ne prouve pas le crédit des récompenses.

## Livraison Android

- [ ] CI verte sur le dernier commit de `main` : lint, JUnit, build debug/release, `apksigner verify`, somme SHA-256.
- [ ] `AndroidManifest.xml` ne déclare aucun `BIND_ACCESSIBILITY_SERVICE`. La CI doit refuser tout retour de ce service.
- [ ] Artefact `validate-steps-debug` : APK de test uniquement, signature de debug susceptible de changer entre compilations.
- [ ] Pour mettre à jour la même application sans la désinstaller, conserver une clé privée de signature durable et configurer `VS_KEYSTORE_B64`, `VS_KEYSTORE_PASSWORD`, `VS_KEY_ALIAS` et `VS_KEY_PASSWORD` dans les secrets GitHub. Utiliser `publish_signed` sur `main` lorsque ces secrets sont prêts.

## Tests réels Redmi 15 5G / Android 15

- [ ] Activer le mode autonome une seule fois, puis accorder Santé Connect (pas et lecture en arrière-plan), alarme exacte, et accès Android aux notifications.
- [ ] Vérifier l'état du mode autonome et l'absence de service ou de parcours Accessibilité.
- [ ] Sous HyperOS, autoriser démarrage automatique et activité en arrière-plan ; tester écran verrouillé à 22 h et regarder les contrôles de secours à 22 h 20 et 23 h 15.
- [ ] Vérifier les dates après minuit, un redémarrage, un changement de fuseau et la récupération d'une permission perdue.
- [ ] Consulter séparément WeWard, Macadam, Winwalk et Sweatcoin : distinguer une action Android envoyée, un crédit déclaré par l'utilisateur, et un crédit réellement visible dans le compte.
- [ ] Si aucune action directe du fournisseur n'a été observée, **ne pas prétendre avoir validé automatiquement les récompenses**.
- [ ] Tester les bilans datés (30 jours), l'arrêt des automatismes et le rappel du soir facultatif sans bouton « Tout valider ».

## Limites

Les fournisseurs peuvent exiger l'ouverture de leur interface ou une validation humaine. Une lecture Santé Connect ou l'envoi d'un `PendingIntent` ne suffit pas à prouver des pièces créditées. Aucun contournement d'API privée, publicité, CAPTCHA ou identité n'est utilisé.
