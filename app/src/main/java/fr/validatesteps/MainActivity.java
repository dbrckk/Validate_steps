package fr.validatesteps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.Duration;
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
    private LinearLayout readinessSection;
    private StepRingView progress;
    private TextView activityDate;
    private LocalDate displayedDay;
    private int latestStepRequest;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable midnightRefresh = this::handleNewLocalDay;
    private TextView scheduleStatus;
    private TextView stepSource;
    private TextView subtitle;
    private TextView autonomousSummary;
    private TextView creditSummary;
    private TextView dailyReportText;
    private TextView timelineText;
    private TextView historicalSummaryText;
    private LocalDate selectedHistoryDate;
    private Switch scheduleSwitch;
    private Switch notificationSwitch;
    private Switch reviewSwitch;
    private boolean updating;
    private int lastDirectActions = -1;

    private int dp(float value) { return DashboardStyles.dp(this, value); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(DashboardStyles.BG);
        getWindow().setNavigationBarColor(DashboardStyles.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        render();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
        space(content, 26);
        title(content, "PRÊT À VALIDER ?", "Touchez un indicateur pour configurer Android");
        readinessSection = column();
        content.addView(readinessSection);
        space(content, 28);
        title(content, "VOS APPLICATIONS", "4 services · états distincts");
        providersSection = column();
        content.addView(providersSection);
        space(content, 22);
        dailyReportPanel();
        space(content, 20);
        historyPanel();
        space(content, 24);
        automation();
        space(content, 24);
        settings();
        space(content, 26);
        timeline();
        space(content, 22);
        TextView foot = label("Mode autonome : aucun service Accessibilité requis. " +
            "Les commandes sont envoyées seulement si les fournisseurs publient " +
            "une action directe compatible ; aucun crédit n'est présumé reçu.", 12,
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
        TextView version = label("v1.1", 11, DashboardStyles.ACCENT, true);
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

        displayedDay = LocalDate.now();
        String date = displayedDay.format(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE));
        activityDate = label("VOTRE ACTIVITÉ · " + date.toUpperCase(Locale.FRANCE),
            12, 0xffc5d9e4, true);
        activityDate.setGravity(Gravity.CENTER);
        card.addView(activityDate);

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
        TextView main = label("◉   ACTIVER LE MODE AUTONOME 22 H", 16, 0xff082223, true);
        main.setGravity(Gravity.CENTER);
        main.setPadding(dp(20), dp(21), dp(20), dp(21));
        main.setBackground(DashboardStyles.gradient(this, 0xff82f8d6,
            DashboardStyles.ACCENT, 20));
        main.setElevation(dp(5));
        main.setContentDescription("Configurer le relevé et les actions directes à 22 heures, sans Accessibilité");
        main.setOnClickListener(v -> configureAutonomousMode());
        content.addView(main, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView direct = action("Examiner les actions directes maintenant",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT, this::onClaimAll);
        LinearLayout.LayoutParams directParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        directParams.topMargin = dp(10);
        content.addView(direct, directParams);

        subtitle = label("Sans Accessibilité · commandes de notification uniquement",
            12, DashboardStyles.MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(dp(6), dp(13), dp(6), dp(5));
        content.addView(subtitle);

        creditSummary = label("SUIVI DES CRÉDITS · non vérifiés", 13,
            DashboardStyles.TEXT, true);
        creditSummary.setGravity(Gravity.CENTER);
        creditSummary.setPadding(dp(10), dp(12), dp(10), dp(12));
        creditSummary.setBackground(DashboardStyles.stroke(this,
            DashboardStyles.SURFACE, 0xff2e4963, 14, 1));
        LinearLayout.LayoutParams creditsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        creditsParams.bottomMargin = dp(10);
        content.addView(creditSummary, creditsParams);

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
                toast(connected
                    ? "Diagnostic terminé · consulte les états des applications"
                    : "Service de notifications déconnecté : voir les autorisations");
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

    /** Compact and actionable permission/availability dashboard. */
    private void rebuildReadiness() {
        if (readinessSection == null) return;
        readinessSection.removeAllViews();
        RunStore run = new RunStore(this);
        boolean health = HealthReader.backgroundSupported() &&
            checkSelfPermission(HealthReader.READ_STEPS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) == PackageManager.PERMISSION_GRANTED;
        boolean exact = DailyScheduler.exactAllowed(this);
        boolean scheduled = run.enabled();
        boolean access = RewardActionListener.hasPermission(this);
        boolean listening = new ActionBridgeState(this).enabled();

        LinearLayout first = row();
        readinessTile(first, "SANTÉ CONNECT", health,
            health ? "Accès aux pas autorisé" : "Autorisation requise", () -> {
                if (!HealthReader.backgroundSupported()) {
                    toast("Lecture en arrière-plan non disponible sur cette version Android");
                } else {
                    requestPermissions(new String[]{
                        HealthReader.READ_STEPS, HealthReader.READ_BACKGROUND}, 10);
                }
            }, true);
        readinessTile(first, "ALARME · 22 H", scheduled && exact,
            !scheduled ? "À activer" : exact ? "Programmée" : "Horaire approximatif",
            () -> {
                if (!scheduled) {
                    new RunStore(this).enable(true);
                    DailyScheduler.schedule(this);
                    DailyScheduler.scheduleRecovery(this, true);
                    refresh();
                    toast("Relevé quotidien à 22 h activé");
                } else if (!exact) {
                    Intent exactIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    exactIntent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(exactIntent);
                } else {
                    toast("Relevé à 22 h activé");
                }
            }, false);
        readinessSection.addView(first);

        LinearLayout second = row();
        readinessTile(second, "NOTIFICATIONS", access && listening,
            !access ? "Accès Android requis" : listening ? "Actions actives" : "À activer",
            () -> {
                if (!access) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } else if (!listening) {
                    notificationSwitch.setChecked(true);
                } else {
                    RewardActionListener.diagnoseConnectedNow(this);
                    refresh();
                    toast("Diagnostic des notifications actualisé");
                }
            }, true);
        boolean autonomous = AutonomousModePolicy.autonomousReady(
            scheduled, listening, access, health);
        readinessTile(second, "SANS ASSISTANCE", autonomous,
            autonomous ? "Mode autonome configuré" : "Configuration nécessaire",
            this::configureAutonomousMode, false);
        LinearLayout.LayoutParams secondLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        secondLp.topMargin = dp(10);
        readinessSection.addView(second, secondLp);
    }

    private void readinessTile(LinearLayout parent, String name, boolean ready,
            String detail, Runnable press, boolean left) {
        LinearLayout tile = column();
        tile.setPadding(dp(14), dp(14), dp(10), dp(15));
        tile.setBackground(DashboardStyles.stroke(this,
            DashboardStyles.SURFACE, ready ? 0xff265447 : 0xff3b415a, 16, 1));
        TextView header = label((ready ? "●  " : "○  ") + name,
            12, ready ? DashboardStyles.ACCENT : DashboardStyles.WARNING, true);
        header.setSingleLine(true);
        header.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(header);
        TextView description = label(detail, 12, DashboardStyles.MUTED, false);
        description.setPadding(0, dp(9), 0, 0);
        description.setMaxLines(2);
        tile.addView(description);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(name + " : " + detail + ". Toucher pour configurer.");
        tile.setOnClickListener(v -> press.run());
        tile.setMinimumHeight(dp(98));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (left) params.rightMargin = dp(5);
        else params.leftMargin = dp(5);
        parent.addView(tile, params);
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

        // The user can report an actual provider-side credit after checking
        // the source app. Such a report is never represented as server proof.
        ProviderCreditStore evidence = new ProviderCreditStore(this);
        String credit = evidence.visibleStatus(pkg, LocalDate.now());
        boolean reportedReceived = evidence.userReportedReceivedToday(pkg, LocalDate.now());
        TextView evidenceText = label(credit, 12,
            reportedReceived ? DashboardStyles.ACCENT : DashboardStyles.MUTED, true);
        evidenceText.setPadding(0, dp(12), 0, dp(8));
        card.addView(evidenceText);
        TextView report = action("Vérifier / déclarer le crédit  ↗",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT,
            () -> showCreditEvidenceDialog(name, pkg));
        card.addView(report, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        providersSection.addView(card, cp);
    }

    /**
     * Explicit first-party user statement ONLY. No providers expose authenticated
     * reward settlement information through our Android application.
     */
    private void showCreditEvidenceDialog(String name, String pkg) {
        String[] choices = {
            "Oui, récompenses visibles dans " + name,
            "Non, récompenses absentes de " + name,
            "Effacer ma déclaration du jour"
        };
        new AlertDialog.Builder(this)
            .setTitle("Crédits " + name + " · vérification manuelle")
            .setMessage("Vérifie directement le solde ou l'historique de " + name +
                ". Validate Steps ne peut pas confirmer un gain auprès du fournisseur.")
            .setItems(choices, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this)
                        .setTitle("Récompenses réellement reçues ?")
                        .setMessage("Tu déclares les avoir personnellement vérifiées dans " +
                            name + ". Cette indication est conservée localement et " +
                            "ne constitue pas une attestation du fournisseur.")
                        .setPositiveButton("Je confirme", (d, w) -> {
                            new ProviderCreditStore(this).report(pkg, LocalDate.now(),
                                CreditEvidencePolicy.USER_REPORTED_RECEIVED);
                            new NightTimeline(this).addProvider(
                                NightTimeline.Event.USER_CREDIT_REPORTED_RECEIVED, LocalDate.now(), pkg);
                            refresh();
                        })
                        .setNegativeButton("Annuler", null).show();
                } else if (which == 1) {
                    new ProviderCreditStore(this).report(pkg, LocalDate.now(),
                        CreditEvidencePolicy.USER_REPORTED_MISSING);
                    new NightTimeline(this).addProvider(
                        NightTimeline.Event.USER_CREDIT_REPORTED_MISSING, LocalDate.now(), pkg);
                    refresh();
                } else if (which == 2) {
                    new ProviderCreditStore(this).clear(pkg);
                    new NightTimeline(this).addProvider(
                        NightTimeline.Event.USER_CREDIT_REPORT_CLEARED, LocalDate.now(), pkg);
                    refresh();
                }
            }).setNegativeButton("Fermer", null).show();
    }

    /** Read-only daily status report. No external balances or authentication. */
    private void dailyReportPanel() {
        title(content, "BILAN DU JOUR",
            "Actions, clics et déclarations de récompenses · 4 applications");
        LinearLayout card = column();
        card.setPadding(dp(16), dp(18), dp(16), dp(16));
        card.setBackground(DashboardStyles.stroke(this, DashboardStyles.SURFACE,
            0xff2b4661, 20, 1));

        TextView caution = label(
            "Les commandes et clics restent des tentatives. Seules vos " +
            "déclarations signalent les récompenses consultées dans les applications.",
            12, DashboardStyles.WARNING, false);
        caution.setPadding(0, 0, 0, dp(14));
        card.addView(caution);

        dailyReportText = label("", 12, DashboardStyles.TEXT, false);
        dailyReportText.setLineSpacing(dp(4), 1f);
        dailyReportText.setTextIsSelectable(true);
        card.addView(dailyReportText);
        space(card, 16);

        LinearLayout buttons = row();
        TextView copy = action("Copier le bilan",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT, () -> {
                ClipboardManager manager = getSystemService(ClipboardManager.class);
                if (manager != null) {
                    manager.setPrimaryClip(ClipData.newPlainText(
                        "Validate Steps · Rapport quotidien",
                        DailyProviderReport.build(this, LocalDate.now())));
                    toast("Bilan du jour copié");
                } else {
                    toast("Presse-papiers Android indisponible");
                }
            });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(48), 1f);
        a.rightMargin = dp(6);
        buttons.addView(copy, a);

        TextView update = action("Actualiser",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT, () -> {
                refresh();
                toast("Bilan local mis à jour");
            });
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(48), 1f);
        b.leftMargin = dp(6);
        buttons.addView(update, b);
        card.addView(buttons);
        content.addView(card);
    }

    /**
     * Lightweight archive overview. Detailed historic days open only on demand
     * so a small Android display is not flooded by 30 verbose reports.
     */
    private void historyPanel() {
        title(content, "RAPPORTS PRÉCÉDENTS",
            "30 jours maximum · historiques locaux depuis la v1.0");
        LinearLayout card = column();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(DashboardStyles.stroke(this, DashboardStyles.SURFACE,
            0xff2b4661, 20, 1));

        historicalSummaryText = label("", 12, DashboardStyles.TEXT, false);
        historicalSummaryText.setLineSpacing(dp(4), 1f);
        historicalSummaryText.setTextIsSelectable(true);
        card.addView(historicalSummaryText);
        space(card, 14);

        LinearLayout buttons = row();
        TextView choose = action("Choisir un jour",
            DashboardStyles.SURFACE_RAISED, DashboardStyles.TEXT,
            this::chooseHistoricalDay);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(50), 1f);
        first.rightMargin = dp(6);
        buttons.addView(choose, first);

        TextView clear = action("Effacer archives", 0xff3a2736,
            DashboardStyles.TEXT, () -> new AlertDialog.Builder(this)
                .setTitle("Effacer les bilans des 30 derniers jours ?")
                .setMessage("Efface uniquement les archives locales créées " +
                    "depuis la v1.0. Le bilan courant, les autorisations et " +
                    "la programmation à 22 h restent inchangés.")
                .setPositiveButton("Effacer", (d, w) -> {
                    new DailyHistoryStore(this).clear();
                    refresh();
                    toast("Archives locales effacées");
                })
                .setNegativeButton("Annuler", null)
                .show());
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(50), 1f);
        second.leftMargin = dp(6);
        buttons.addView(clear, second);

        card.addView(buttons);
        content.addView(card);
    }

    private void chooseHistoricalDay() {
        LocalDate now = LocalDate.now();
        LocalDate selection = selectedHistoryDate == null ?
            now.minusDays(1) : selectedHistoryDate;
        DatePickerDialog picker = new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                selectedHistoryDate = LocalDate.of(year, month + 1, dayOfMonth);
                showHistoricalDay(selectedHistoryDate);
            }, selection.getYear(), selection.getMonthValue() - 1,
            selection.getDayOfMonth());
        long min = now.minusDays(HistoryRecordPolicy.RETENTION_DAYS - 1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long max = now.plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L;
        picker.getDatePicker().setMinDate(min);
        picker.getDatePicker().setMaxDate(max);
        picker.show();
    }

    private void showHistoricalDay(LocalDate day) {
        String report = new DailyHistoryStore(this).report(this, day);
        TextView body = label(report, 13, DashboardStyles.TEXT, false);
        body.setTextIsSelectable(true);
        body.setLineSpacing(dp(4), 1f);
        body.setPadding(dp(18), dp(12), dp(18), dp(16));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new AlertDialog.Builder(this)
            .setTitle("Bilan du " + day)
            .setView(scroll)
            .setPositiveButton("Copier le bilan", (d, w) -> {
                ClipboardManager clipboard = getSystemService(ClipboardManager.class);
                if (clipboard == null) {
                    toast("Presse-papiers Android indisponible");
                    return;
                }
                clipboard.setPrimaryClip(ClipData.newPlainText(
                    "Validate Steps · " + day, report));
                toast("Bilan du " + day + " copié");
            })
            .setNegativeButton("Fermer", null)
            .show();
    }

    private void automation() {
        title(content, "AUTOMATISATION SANS ASSISTANCE",
            "Santé Connect + actions directes émises par les fournisseurs");
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
        space(card, 14);

        View reminderSeparator = new View(this);
        reminderSeparator.setBackgroundColor(0xff324155);
        card.addView(reminderSeparator, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        space(card, 14);

        LinearLayout reminderLine = row();
        LinearLayout reminderText = column();
        reminderText.addView(label("Alerte du soir facultative",
            15, DashboardStyles.TEXT, true));
        reminderText.addView(label("Vers 23 h 15 : signale les crédits non vérifiés",
            12, DashboardStyles.MUTED, false));
        reminderLine.addView(reminderText, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        reviewSwitch = new Switch(this);
        reviewSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
            DashboardStyles.ACCENT));
        reviewSwitch.setContentDescription(
            "Rappeler facultativement les récompenses non vérifiées après 23 heures 15");
        reviewSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            if (checked && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                updating = true;
                reviewSwitch.setChecked(false);
                updating = false;
                requestPermissions(new String[]{
                    Manifest.permission.POST_NOTIFICATIONS}, 12);
                toast("Autorise les notifications puis active à nouveau le rappel facultatif");
                return;
            }
            AutonomousReviewReminder.enable(this, checked);
            refresh();
        });
        reminderLine.addView(reviewSwitch);
        card.addView(reminderLine);
        content.addView(card);

        autonomousSummary = label("", 12, DashboardStyles.MUTED, false);
        autonomousSummary.setPadding(dp(12), dp(13), dp(12), dp(13));
        autonomousSummary.setLineSpacing(dp(3), 1f);
        autonomousSummary.setBackground(DashboardStyles.stroke(this,
            DashboardStyles.SURFACE, 0xff273a51, 16, 1));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(12);
        content.addView(autonomousSummary);

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

    /** Explicit autonomous opt-in; no Accessibility service is needed. */
    private void configureAutonomousMode() {
        boolean ready = AutonomousModePolicy.autonomousReady(
            new RunStore(this).enabled(), new ActionBridgeState(this).enabled(),
            RewardActionListener.hasPermission(this),
            HealthReader.backgroundSupported() &&
                checkSelfPermission(HealthReader.READ_STEPS) ==
                    PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(HealthReader.READ_BACKGROUND) ==
                    PackageManager.PERMISSION_GRANTED);
        if (ready) {
            new AlertDialog.Builder(this)
                .setTitle("Mode autonome configuré")
                .setMessage("Le relevé à 22 h est activé avec la recherche des actions " +
                    "directes éventuellement publiées par WeWard, Macadam et Winwalk. " +
                    "Aucun accès Accessibilité n'est nécessaire.\n\n" +
                    "Cela ne permet pas de forcer leur validation interne ni de " +
                    "confirmer le crédit des récompenses. Sweatcoin convertit " +
                    "ses pas avec son propre système.")
                .setPositiveButton("Compris", null)
                .setNeutralButton("Diagnostiquer", (d, w) -> {
                    RewardActionListener.diagnoseConnectedNow(this);
                    refresh();
                })
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Activer le mode autonome à 22 h")
            .setMessage("Validate Steps lira les pas réels via Santé Connect et " +
                "recherchera, entre 22 h et 23 h 40, des boutons directs de " +
                "validation publiés dans les notifications des applications " +
                "autorisées.\n\n" +
                "Android donne au service d'accès aux notifications une " +
                "permission sensible : notre code examine uniquement les " +
                "notifications des trois applications concernées, sans " +
                "enregistrer leurs messages.\n\n" +
                "Aucune intervention Accessibilité ni ouverture automatique " +
                "des applications n'est nécessaire. Cela ne garantit pas " +
                "l'obtention des pièces : les fournisseurs peuvent ne pas " +
                "proposer de commande directe.")
            .setPositiveButton("Activer", (d, w) -> {
                new RunStore(this).enable(true);
                new ActionBridgeState(this).enable(true);
                DailyScheduler.schedule(this);
                DailyScheduler.scheduleRecovery(this, true);
                if (!RewardActionListener.hasPermission(this)) {
                    toast("Accorde l'accès aux notifications à Validate Steps");
                    startActivity(new Intent(
                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } else {
                    RewardActionListener.ensureConnected(this);
                    RewardActionListener.diagnoseConnectedNow(this);
                    if (HealthReader.backgroundSupported() &&
                            (checkSelfPermission(HealthReader.READ_STEPS) !=
                                PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(HealthReader.READ_BACKGROUND) !=
                                PackageManager.PERMISSION_GRANTED)) {
                        requestPermissions(new String[]{
                            HealthReader.READ_STEPS, HealthReader.READ_BACKGROUND}, 10);
                    } else if (!DailyScheduler.exactAllowed(this)) {
                        toast("Autorise les alarmes exactes pour viser 22 h");
                    }
                }
                refresh();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Manual diagnostic/dispatch of official notification actions ONLY.
     * No Accessibility, no opening other apps and no forced provider credit.
     */
    private void onClaimAll() {
        if (ClaimTimingPolicy.risksPartialDayClaim(LocalTime.now())) {
            new AlertDialog.Builder(this)
                .setTitle("Envoyer une commande avant 22 h ?")
                .setMessage("Si un fournisseur publie une commande directe, " +
                    "elle pourrait valider seulement les pas déjà accumulés. " +
                    "Tu peux tester maintenant, ou laisser l'automatisation " +
                    "attendre 22 h. Aucune récompense n'est garantie.")
                .setPositiveButton("Essayer maintenant", (d, w) -> executeDirectOnly())
                .setNeutralButton("Configurer 22 h", (d, w) -> configureAutonomousMode())
                .setNegativeButton("Annuler", null)
                .show();
            return;
        }
        executeDirectOnly();
    }

    private void executeDirectOnly() {
        int sent = RewardActionListener.claimDirectNowFromUserTap(this);
        lastDirectActions = sent;
        refresh();
        String message = sent > 0
            ? sent + " commande(s) Android envoyée(s) depuis des notifications " +
                "des fournisseurs. Le crédit reste à vérifier dans leurs applications."
            : sent == 0
                ? "Aucune nouvelle commande directe de validation des pas " +
                    "n'a été envoyée. Les fournisseurs peuvent ne proposer " +
                    "qu'une validation dans leur propre interface, ou une " +
                    "tentative a peut-être déjà été faite aujourd'hui."
                : "Le service Android de notifications n'est pas connecté " +
                    "ou son autorisation est absente. Aucun essai n'a été envoyé.";
        new AlertDialog.Builder(this)
            .setTitle("Résultat sans assistance")
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .setNeutralButton("Configurer le mode autonome",
                (d, w) -> configureAutonomousMode())
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

    /** Show no step value unless the latest Health Connect read is valid for today. */
    private void fetchSteps(boolean manual) {
        final int requestId = ++latestStepRequest;
        final LocalDate requestedDay = LocalDate.now();
        boolean granted = checkSelfPermission(HealthReader.READ_STEPS) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) ==
            PackageManager.PERMISSION_GRANTED;
        if (!granted || !HealthReader.backgroundSupported()) {
            if (progress != null) progress.setSteps(-1L);
            if (stepSource != null)
                stepSource.setText("Santé Connect · autorisation indisponible");
            if (manual) toast("Autorise Santé Connect depuis les paramètres");
            return;
        }
        if (stepSource != null) stepSource.setText("Lecture des pas réels en cours…");
        HealthReader.queryDay(this, requestedDay, (steps, status) ->
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || progress == null ||
                        !StepDisplayPolicy.isLatestRequest(requestId, latestStepRequest))
                    return;
                boolean usable = StepDisplayPolicy.isCurrentReading(
                    requestedDay, LocalDate.now(), steps, status);
                progress.setSteps(usable ? steps : -1L);
                if (usable) {
                    stepSource.setText("Santé Connect · pas réels actualisés");
                } else if (!requestedDay.equals(LocalDate.now())) {
                    stepSource.setText("Nouveau jour · relevez les pas à nouveau");
                } else {
                    stepSource.setText("Lecture indisponible · " + status);
                }
                if (manual) toast(usable ? "Pas réels actualisés : " + steps
                    : "Lecture indisponible : " + status);
            }));
    }

    /** UI-only date change: no background polls, one callback at local midnight. */
    private void updateDisplayedDay() {
        LocalDate today = LocalDate.now();
        if (activityDate == null || today.equals(displayedDay)) return;
        displayedDay = today;
        String date = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE));
        activityDate.setText("VOTRE ACTIVITÉ · " + date.toUpperCase(Locale.FRANCE));
        if (progress != null) progress.setSteps(-1L);
        if (stepSource != null) stepSource.setText("Santé Connect · nouvelle journée");
    }

    private void handleNewLocalDay() {
        if (isFinishing() || isDestroyed()) return;
        updateDisplayedDay();
        refresh();
        fetchSteps(false);
        scheduleMidnightRefresh();
    }

    private void scheduleMidnightRefresh() {
        uiHandler.removeCallbacks(midnightRefresh);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        long milliseconds = Math.max(1000L,
            Duration.between(now.toInstant(), tomorrow.toInstant()).toMillis() + 1500L);
        uiHandler.postDelayed(midnightRefresh, milliseconds);
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) {
            updateDisplayedDay();
            // Returning from Android notification settings completes the opt-in
            // without requiring an extra launch of an Accessibility workflow.
            RewardActionListener.ensureConnected(this);
            maybeRecoverNightlyAfterPermissionGrant();
            refresh();
            fetchSteps(false);
            scheduleMidnightRefresh();
        }
    }

    @Override protected void onPause() {
        ++latestStepRequest; // Ignore pending results from a no-longer-visible Activity.
        uiHandler.removeCallbacks(midnightRefresh);
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (content != null) {
            if (requestCode == 10) maybeRecoverNightlyAfterPermissionGrant();
            refresh();
            fetchSteps(false);
        }
    }

    /**
     * An evening Health Connect denial cancels further automatic retries
     * until permissions change. When the user restores permission, enqueue
     * ONE auditable same-day retry (no UI automation or provider claims).
     */
    private void maybeRecoverNightlyAfterPermissionGrant() {
        LocalDate today = LocalDate.now();
        boolean granted = HealthReader.backgroundSupported() &&
            checkSelfPermission(HealthReader.READ_STEPS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) == PackageManager.PERMISSION_GRANTED;
        String key = "health_permission_recovery_date";
        String lastRecovery = getPreferences(MODE_PRIVATE).getString(key, "");
        boolean eligible = HealthPermissionRecoveryPolicy.shouldRetry(
            new RunStore(this).enabled(),
            new RunStore(this).statusOn(today),
            granted,
            RetryPolicy.canRecover(ZonedDateTime.now()),
            today.toString().equals(lastRecovery));
        if (!eligible) return;
        if (!getPreferences(MODE_PRIVATE).edit().putString(key, today.toString()).commit())
            return;
        new NightTimeline(this).add(NightTimeline.Event.AUDIT_PERMISSION_RECOVERED, today);
        AuditWorker.enqueue(this, today);
    }

    private void refresh() {
        if (content == null) return;
        RunStore run = new RunStore(this);
        ActionBridgeState action = new ActionBridgeState(this);
        updating = true;
        if (scheduleSwitch != null) scheduleSwitch.setChecked(run.enabled());
        if (notificationSwitch != null) notificationSwitch.setChecked(action.enabled());
        if (reviewSwitch != null)
            reviewSwitch.setChecked(AutonomousReviewReminder.enabled(this));
        updating = false;
        boolean healthReady = HealthReader.backgroundSupported() &&
            checkSelfPermission(HealthReader.READ_STEPS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) == PackageManager.PERMISSION_GRANTED;
        if (scheduleStatus != null) scheduleStatus.setText(!run.enabled()
            ? "○  RELEVÉ À 22 H DÉSACTIVÉ"
            : !healthReady
                ? "◌  RELEVÉ 22 H · ACCÈS AUX PAS MANQUANT"
                : DailyScheduler.exactAllowed(this)
                    ? "●  RELEVÉ DES PAS À 22 H · PROGRAMMÉ"
                    : "◌  RELEVÉ DES PAS · HORAIRE APPROXIMATIF");
        rebuildReadiness();
        rebuildProviders();
        if (creditSummary != null) {
            int declared = new ProviderCreditStore(this)
                .userReportedReceivedCount(LocalDate.now());
            creditSummary.setText("SUIVI DES CRÉDITS · " + declared +
                " / 4 déclarés reçus par vous aujourd'hui");
            creditSummary.setContentDescription(
                declared + " sur 4 récompenses déclarées reçues par vous aujourd'hui. " +
                "Aucune validation automatique des soldes des fournisseurs.");
        }
        if (dailyReportText != null)
            dailyReportText.setText(DailyProviderReport.build(this, LocalDate.now()));
        if (historicalSummaryText != null)
            historicalSummaryText.setText(new DailyHistoryStore(this).recentSummary());
        if (timelineText != null) timelineText.setText(
            new NightTimeline(this).recent()
            + "\n\nAUDIT : lecture Santé Connect · CLAIM_ACTION : action de notification"
            + "\nSANS APPUI : aucune interaction Accessibilité");
        if (subtitle != null) subtitle.setText(
            lastDirectActions > 0
                ? lastDirectActions + " commande(s) Android envoyée(s) · crédits non confirmés"
                : "Mode autonome : relevé à 22 h et actions directes si disponibles");
        if (autonomousSummary != null)
            autonomousSummary.setText(AutonomousSnapshot.describe(
                this, RewardActionListener.isConnected()));
    }
}
