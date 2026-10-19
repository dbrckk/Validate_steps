package fr.validatesteps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * High-contrast, responsive native Android dashboard.
 * A real step read is NEVER presented as a confirmed external reward credit.
 */
public final class MainActivity extends Activity {
    private LinearLayout content;
    private LinearLayout settingsContent;
    private LinearLayout providersSection;
    private StepRingView progress;
    private TextView scheduleStatus;
    private TextView stepSource;
    private TextView subtitle;
    private TextView timelineText;
    private Switch scheduleSwitch;
    private Switch notificationSwitch;
    private boolean updating;

    private int dp(float value) { return DashboardStyles.dp(this, value); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(DashboardStyles.BG);
        getWindow().setNavigationBarColor(DashboardStyles.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        render();
        handleNotificationIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(GuidedNotificationToken.EXTRA)) return;
        String token = intent.getStringExtra(GuidedNotificationToken.EXTRA);
        // Remove extras even from this Activity's in-memory Intent to block replay.
        intent.removeExtra(GuidedNotificationToken.EXTRA);
        if (!GuidedNotificationToken.consume(this, token)) {
            toast("Ce bouton a expiré. Lance le parcours depuis le tableau de bord.");
            return;
        }
        // User tapped the notification; consent and system permission are still mandatory.
        onClaimAll();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(DashboardStyles.BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(44));
        scroll.addView(content, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        header();
        space(content, 22);
        hero();
        space(content, 18);
        actions();
        space(content, 28);
        title(content, "VOS APPLICATIONS", "4 services · états distincts");
        providersSection = column();
        content.addView(providersSection);
        space(content, 22);
        automation();
        space(content, 24);
        settings();
        space(content, 26);
        timeline();
        space(content, 22);
        TextView foot = label("Aucun crédit n'est déclaré confirmé sans preuve du fournisseur. " +
            "L'assistance à un appui ouvre les applications et peut nécessiter une validation manuelle.", 12,
            DashboardStyles.MUTED, false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
        refresh();
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        return DashboardStyles.text(this, text, size, color, bold);
    }

    private void space(LinearLayout parent, int dp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    private void header() {
        LinearLayout row = row();
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher_steps);
        mark.setContentDescription("Validate Steps");
        row.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout titles = column();
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(label("VALIDATE STEPS", 21, DashboardStyles.TEXT, true));
        titles.addView(label("Vos pas. Vos récompenses.", 12, DashboardStyles.MUTED, false));
        row.addView(titles, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView version = label("v0.13", 11, DashboardStyles.ACCENT, true);
        version.setPadding(dp(12), dp(7), dp(12), dp(7));
        version.setBackground(DashboardStyles.round(this, 0xff183a40, 99));
        row.addView(version);
        content.addView(row);
    }

    private void hero() {
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(16), dp(22), dp(16), dp(23));
        card.setBackground(DashboardStyles.gradient(this, 0xff1a3b4b, 0xff17243e, 30));

        String date = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE));
        TextView heading = label("VOTRE ACTIVITÉ · " + date.toUpperCase(Locale.FRANCE),
            12, 0xffc5d9e4, true);
        heading.setGravity(Gravity.CENTER);
        card.addView(heading);

        progress = new StepRingView(this);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(218), dp(218));
        rp.topMargin = dp(12);
        card.addView(progress, rp);

        stepSource = label("Santé Connect · synchronisation locale", 12, 0xffc6d8e5, false);
        stepSource.setGravity(Gravity.CENTER);
        card.addView(stepSource);
        space(card, 18);

        scheduleStatus = label("22:00 · Automatisation désactivée",
            14, DashboardStyles.TEXT, true);
        scheduleStatus.setPadding(dp(16), dp(13), dp(16), dp(13));
        scheduleStatus.setBackground(DashboardStyles.round(this, 0xff274759, 13));
        scheduleStatus.setGravity(Gravity.CENTER);
        card.addView(scheduleStatus, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(card);
    }

    private void actions() {
        TextView main = label("✓   TOUT VALIDER", 18, 0xff082223, true);
        main.setGravity(Gravity.CENTER);
        main.setPadding(dp(20), dp(21), dp(20), dp(21));
        main.setBackground(DashboardStyles.gradient(this, 0xff82f8d6,
            DashboardStyles.ACCENT, 20));
        main.setElevation(dp(5));
        main.setContentDescription("Lancer le parcours à un appui de validation des pas");
        main.setOnClickListener(v -> onClaimAll());
        content.addView(main, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        subtitle = label("Parcours assisté · ouvre les applications · crédit à vérifier",
            12, DashboardStyles.MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(dp(6), dp(13), dp(6), dp(5));
        content.addView(subtitle);

        LinearLayout row = row();
        TextView refresh = action("↻  Actualiser les pas", DashboardStyles.SURFACE_RAISED,
            DashboardStyles.TEXT, () -> fetchSteps(true));
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(53), 1f);
        first.rightMargin = dp(7);
        row.addView(refresh, first);
        TextView diagnose = action("⌕  Diagnostiquer", DashboardStyles.SURFACE_RAISED,
            DashboardStyles.TEXT, () -> {
                boolean connected = RewardActionListener.diagnoseConnectedNow(this);
                refresh();
                toast(connected ? "Notifications inspectées sans aucune action" :
                    "Service de notifications déconnecté : voir les autorisations");
            });
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(53), 1f);
        second.leftMargin = dp(7);
        row.addView(diagnose, second);
        content.addView(row);
    }

    private void title(LinearLayout parent, String heading, String desc) {
        parent.addView(label(heading, 14, DashboardStyles.TEXT, true));
        TextView caption = label(desc, 12, DashboardStyles.MUTED, false);
        caption.setPadding(0, dp(6), 0, dp(14));
        parent.addView(caption);
    }

    private TextView action(String text, int bg, int fg, Runnable onClick) {
        TextView b = label(text, 14, fg, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        b.setBackground(DashboardStyles.round(this, bg, 15));
        b.setOnClickListener(v -> onClick.run());
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void rebuildProviders() {
        if (providersSection == null) return;
        providersSection.removeAllViews();
        provider("WeWard", ProviderPackages.WEWARD, "W", 0xfff7b86f);
        provider("Macadam", ProviderPackages.MACADAM, "M", 0xffe8d188);
        provider("Winwalk", ProviderPackages.WINWALK, "w", 0xff96bdff);
        provider("Sweatcoin", ProviderPackages.SWEATCOIN, "S", 0xffc4a9ef);
    }

    private void provider(String name, String pkg, String monogram, int accent) {
        boolean installed = ProviderPackages.installed(this, pkg);
        LinearLayout card = column();
        card.setBackground(DashboardStyles.stroke(this,
            DashboardStyles.SURFACE, 0xff263850, 20, 1));
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        LinearLayout row = row();
        TextView icon = label(monogram, 22, DashboardStyles.BG, true);
        icon.setBackground(DashboardStyles.round(this, accent, 15));
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(51), dp(51)));

        LinearLayout texts = column();
        texts.setPadding(dp(13), 0, 0, 0);
        texts.addView(label(name, 17, DashboardStyles.TEXT, true));
        TextView installedText = label(installed ? "● Installée" : "○ Non installée",
            12, installed ? DashboardStyles.ACCENT : DashboardStyles.WARNING, false);
        installedText.setPadding(0, dp(5), 0, 0);
        texts.addView(installedText);
        row.addView(texts, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView open = action(installed ? "Ouvrir ↗" : "Installer ↗",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT,
            () -> openProvider(pkg));
        row.addView(open, new LinearLayout.LayoutParams(
            dp(93), dp(44)));
        card.addView(row);

        String status;
        if (ProviderPackages.SWEATCOIN.equals(pkg)) {
            status = "Conversion propre à Sweatcoin · crédit non vérifié";
        } else {
            status = new ClaimFlowState(this).result(pkg);
            // Distinct signals, with no false credit confirmation.
            String notification = new ActionBridgeState(this).brief(pkg);
            if (!"Aucune action observée".equals(notification))
                status += "\n" + notification;
        }
        TextView detail = label(status, 12, DashboardStyles.MUTED, false);
        detail.setPadding(0, dp(14), 0, 0);
        detail.setLineSpacing(dp(3), 1f);
        card.addView(detail);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        providersSection.addView(card, cp);
    }

    private void automation() {
        title(content, "AUTOMATISATION", "Deux méthodes complémentaires");
        LinearLayout card = column();
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(DashboardStyles.round(this, DashboardStyles.SURFACE, 20));

        LinearLayout auto = row();
        LinearLayout titles = column();
        titles.addView(label("Relevé quotidien à 22 h", 15, DashboardStyles.TEXT, true));
        titles.addView(label("Alarme et secours à 22 h 20 / 23 h 15",
            12, DashboardStyles.MUTED, false));
        auto.addView(titles, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        scheduleSwitch = new Switch(this);
        scheduleSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
            DashboardStyles.ACCENT));
        scheduleSwitch.setContentDescription("Activer le relevé automatique chaque jour à 22 heures");
        scheduleSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            new RunStore(this).enable(checked);
            if (!checked) new ActionBridgeState(this).enable(false);
            DailyScheduler.schedule(this);
            if (checked) DailyScheduler.scheduleRecovery(this, true);
            refresh();
        });
        auto.addView(scheduleSwitch);
        card.addView(auto);
        space(card, 16);

        View separator = new View(this);
        separator.setBackgroundColor(0xff324155);
        card.addView(separator, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        space(card, 16);

        LinearLayout notification = row();
        LinearLayout ntexts = column();
        ntexts.addView(label("Actions de notifications", 15, DashboardStyles.TEXT, true));
        ntexts.addView(label("En arrière-plan si le service publie un bouton direct",
            12, DashboardStyles.MUTED, false));
        notification.addView(ntexts, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        notificationSwitch = new Switch(this);
        notificationSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
            DashboardStyles.ACCENT));
        notificationSwitch.setContentDescription("Activer les actions directes de notifications en arrière-plan");
        notificationSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            if (checked && !RewardActionListener.hasPermission(this)) {
                notificationSwitch.setChecked(false);
                new AlertDialog.Builder(this)
                    .setTitle("Accès aux notifications")
                    .setMessage("Android doit autoriser explicitement l'accès aux notifications. " +
                        "Validate Steps ne stocke ni leur contenu ni les données de compte.")
                    .setPositiveButton("Configurer", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                    .setNegativeButton("Plus tard", null).show();
                return;
            }
            if (checked && !new RunStore(this).enabled()) {
                new RunStore(this).enable(true);
                DailyScheduler.schedule(this);
                DailyScheduler.scheduleRecovery(this, true);
                toast("Programmation quotidienne activée avec les actions directes");
            }
            new ActionBridgeState(this).enable(checked);
            if (checked) {
                RewardActionListener.ensureConnected(this);
                RewardActionListener.inspectConnectedNow();
            }
            refresh();
        });
        notification.addView(notificationSwitch);
        card.addView(notification);
        content.addView(card);

        TextView caution = label("L'automatisation nocturne ne peut pas forcer l'ouverture " +
            "des applications à écran verrouillé. Elle utilise uniquement les " +
            "actions de notification réellement disponibles.",
            12, DashboardStyles.MUTED, false);
        caution.setPadding(dp(8), dp(12), dp(8), 0);
        content.addView(caution);
    }

    private void settings() {
        TextView open = action("⚙   AUTORISATIONS & PARAMÈTRES   ▾",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT, () -> {
                boolean show = settingsContent.getVisibility() != View.VISIBLE;
                settingsContent.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        content.addView(open);
        settingsContent = column();
        settingsContent.setPadding(dp(8), dp(14), dp(8), dp(3));
        settingsContent.setVisibility(View.GONE);

        setting("Santé Connect", "Accès aux pas réels et lecture en arrière-plan",
            () -> {
                if (!HealthReader.backgroundSupported()) {
                    toast("Lecture Santé Connect en arrière-plan non prise en charge");
                    return;
                }
                requestPermissions(new String[] {
                    HealthReader.READ_STEPS, HealthReader.READ_BACKGROUND }, 10);
            });
        setting("Alarme exacte", "Déclenchement au plus près de 22 h", () -> {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        setting("Accès aux notifications", "Boutons directs proposés par les services",
            () -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        setting("Assistance à un appui", "Autorisation Android d'interagir avec les boutons visibles",
            this::discloseAccessibility);
        setting("Notifications système", "Recevoir les informations de suivi",
            () -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11));
        setting("Paramètres de l'application", "Batterie, autorisations, restrictions HyperOS",
            () -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        setting("Arrêter toutes les automatisations", "Désactive les alarmes et actions directes",
            () -> new AlertDialog.Builder(this)
                .setTitle("Arrêter l'automatisation ?")
                .setMessage("Les alarmes seront annulées. Les crédits déjà acquis " +
                    "dans les applications ne seront pas modifiés.")
                .setPositiveButton("Arrêter", (d, w) -> {
                    new RunStore(this).enable(false);
                    new ActionBridgeState(this).enable(false);
                    GuidedClaimService.cancel(this);
                    DailyScheduler.schedule(this);
                    refresh();
                }).setNegativeButton("Annuler", null).show());
        content.addView(settingsContent);
    }

    private void setting(String heading, String detail, Runnable action) {
        LinearLayout line = column();
        line.setPadding(dp(12), dp(14), dp(12), dp(14));
        line.setBackground(DashboardStyles.round(this, DashboardStyles.SURFACE, 14));
        line.addView(label(heading + "  ↗", 14, DashboardStyles.TEXT, true));
        TextView small = label(detail, 12, DashboardStyles.MUTED, false);
        small.setPadding(0, dp(5), 0, 0);
        line.addView(small);
        line.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(7);
        settingsContent.addView(line, params);
    }

    private void timeline() {
        title(content, "HISTORIQUE", "Dernières exécutions sur ce téléphone");
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(15), dp(16), dp(15));
        panel.setBackground(DashboardStyles.stroke(this, DashboardStyles.SURFACE,
            0xff273a51, 20, 1));
        timelineText = label("", 12, DashboardStyles.MUTED, false);
        timelineText.setTypeface(android.graphics.Typeface.MONOSPACE);
        timelineText.setTextIsSelectable(true);
        panel.addView(timelineText);
        space(panel, 16);

        LinearLayout buttons = row();
        TextView copy = action("Copier", DashboardStyles.SURFACE_RAISED,
            DashboardStyles.TEXT, () -> {
                ClipboardManager manager = getSystemService(ClipboardManager.class);
                if (manager != null) manager.setPrimaryClip(ClipData.newPlainText(
                    "Validate Steps · Diagnostic", new NightTimeline(this).recent()));
                toast("Historique copié");
            });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(46), 1f);
        a.rightMargin = dp(6);
        buttons.addView(copy, a);

        TextView erase = action("Effacer", 0xff3a2736, DashboardStyles.TEXT,
            () -> new AlertDialog.Builder(this)
                .setTitle("Effacer l'historique ?")
                .setMessage("Cette action supprime uniquement le diagnostic local.")
                .setPositiveButton("Effacer", (d, w) -> {
                    new NightTimeline(this).clear();
                    refresh();
                })
                .setNegativeButton("Annuler", null).show());
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b.leftMargin = dp(6);
        buttons.addView(erase, b);
        panel.addView(buttons);
        content.addView(panel);
    }

    private void onClaimAll() {
        if (!getPreferences(MODE_PRIVATE).getBoolean("assistance_disclosed", false)) {
            discloseAccessibility();
            return;
        }
        if (!GuidedClaimService.connected()) {
            new AlertDialog.Builder(this)
                .setTitle("Activer l'assistance Android")
                .setMessage("Le service facultatif n'est pas connecté. " +
                    "Autorise Validate Steps dans Accessibilité pour activer le " +
                    "parcours à un appui. Les applications s'ouvriront à l'écran.")
                .setPositiveButton("Ouvrir les réglages", (d, w) ->
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("Annuler", null).show();
            return;
        }
        if (!GuidedClaimService.begin(this)) {
            toast(new ClaimFlowState(this).active()
                ? "Un parcours assisté est déjà en cours"
                : "Impossible de démarrer : vérifie l'autorisation Accessibilité");
            return;
        }
        toast("Parcours lancé · boutons des pas uniquement");
    }

    private void discloseAccessibility() {
        new AlertDialog.Builder(this)
            .setTitle("Assistance à un appui — consentement")
            .setMessage("Cette fonction facultative ouvre WeWard, Macadam, Winwalk " +
                "et Sweatcoin à l'écran après ton appui. Avec l'autorisation " +
                "Accessibilité, elle peut LIRE les libellés des boutons visibles " +
                "des trois premiers services et CLIQUER uniquement sur un bouton " +
                "de validation de pas explicitement reconnu. Elle ne lit pas les " +
                "autres applications, n'enregistre pas les textes ni ne transmet " +
                "de données. Elle n'interagit jamais avec les publicités, mots " +
                "de passe ou opérations financières. Les clics peuvent être " +
                "refusés par les fournisseurs ; aucun crédit n'est garanti. " +
                "Tu peux désactiver le service dans Android à tout moment.")
            .setPositiveButton("J'accepte · Configurer", (dialog, choice) -> {
                getPreferences(MODE_PRIVATE).edit()
                    .putBoolean("assistance_disclosed", true).apply();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            })
            .setNegativeButton("Refuser", null)
            .show();
    }

    private void openProvider(String pkg) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Intent store = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + pkg));
            try {
                startActivity(store);
            } catch (RuntimeException missingStore) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }
            return;
        }
        startActivity(launch);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void fetchSteps(boolean manual) {
        boolean granted = checkSelfPermission(HealthReader.READ_STEPS) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) ==
            PackageManager.PERMISSION_GRANTED;
        if (!granted || !HealthReader.backgroundSupported()) {
            if (manual) toast("Autorise Santé Connect depuis les paramètres");
            return;
        }
        stepSource.setText("Lecture des pas réels en cours…");
        HealthReader.queryDay(this, LocalDate.now(), (steps, status) ->
            runOnUiThread(() -> {
                if (isFinishing() || progress == null) return;
                if (steps != null) {
                    progress.setSteps(steps);
                    stepSource.setText("Santé Connect · données actualisées");
                } else {
                    stepSource.setText("Lecture indisponible · " + status);
                }
                if (manual) toast(steps == null ? "Lecture : " + status
                    : "Pas réels actualisés : " + steps);
            }));
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) {
            refresh();
            fetchSteps(false);
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (content != null) {
            refresh();
            fetchSteps(false);
        }
    }

    private void refresh() {
        if (content == null) return;
        RunStore run = new RunStore(this);
        ActionBridgeState action = new ActionBridgeState(this);
        updating = true;
        if (scheduleSwitch != null) scheduleSwitch.setChecked(run.enabled());
        if (notificationSwitch != null) notificationSwitch.setChecked(action.enabled());
        updating = false;
        if (scheduleStatus != null) scheduleStatus.setText(run.enabled()
            ? (DailyScheduler.exactAllowed(this)
                ? "●  AUTOMATIQUE À 22:00 · ACTIVE"
                : "◌  ACTIVE · ALARME APPROXIMATIVE")
            : "○  AUTOMATISATION À 22 H DÉSACTIVÉE");
        rebuildProviders();
        if (timelineText != null) timelineText.setText(
            new NightTimeline(this).recent());
        if (subtitle != null) subtitle.setText(
            GuidedClaimService.connected()
                ? new ClaimFlowState(this).summary()
                : "Parcours à un appui · assistance Android à autoriser");
    }
}
