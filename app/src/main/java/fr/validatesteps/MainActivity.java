package fr.validatesteps;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.LocalDate;

/** Optional one-time setup and diagnostics; never launched by the nightly worker. */
public final class MainActivity extends Activity {
    private TextView info;
    private TextView history;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(34, 46, 34, 34);
        info = new TextView(this);
        info.setTextSize(17);
        layout.addView(info);

        addButton(layout, "Autoriser Santé Connect (pas et arrière-plan)", v -> {
            if (HealthReader.backgroundSupported()) {
                requestPermissions(new String[] {
                    HealthReader.READ_STEPS, HealthReader.READ_BACKGROUND
                }, 10);
            } else {
                info.append("\nAccès en arrière-plan indisponible : mise à jour système nécessaire.");
            }
        });
        addButton(layout, "Autoriser les notifications (facultatif)", v ->
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 11));
        addButton(layout, "Autoriser les alarmes exactes", v -> {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        addButton(layout, "Activer la lecture quotidienne à 22 h", v -> {
            new RunStore(this).enable(true);
            DailyScheduler.schedule(this);
            DailyScheduler.scheduleRecovery(this, true);
            refresh();
        });
        addButton(layout, "Tester la lecture réelle maintenant", v ->
            HealthReader.queryDay(this, LocalDate.now(), (steps, status) ->
                info.append("\n\nTEST (sans gain de pièces) : " +
                    (steps == null ? status : steps + " pas détectés"))));
        addButton(layout, "Autoriser l'accès aux notifications dans Android", v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        // Android 13+ can restrict notification access for side-loaded APKs.
        // Opening app info lets the user explicitly enable restricted settings;
        // this is NEVER performed silently or bypassed by the application.
        addButton(layout, "Ouvrir les paramètres système de Validate Steps", v -> {
            Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            details.setData(Uri.parse("package:" + getPackageName()));
            startActivity(details);
        });
        addButton(layout, "Diagnostiquer les actions maintenant (sans envoyer)", v -> {
            RewardActionListener.diagnoseConnectedNow(this);
            refresh();
        });
        addButton(layout, "Activer les actions directes sans écran", v -> {
            if (RewardActionListener.hasPermission(this)) {
                new ActionBridgeState(this).enable(true);
                RewardActionListener.ensureConnected(this);
                RewardActionListener.inspectConnectedNow();
                refresh();
            } else {
                refresh();
                info.append("\nAutorise d'abord l'accès aux notifications dans Android.");
            }
        });
        addButton(layout, "Désactiver les actions directes", v -> {
            new ActionBridgeState(this).enable(false);
            refresh();
        });
        addButton(layout, "Désactiver et annuler les alarmes", v -> {
            new RunStore(this).enable(false);
            new ActionBridgeState(this).enable(false);
            DailyScheduler.schedule(this);
            refresh();
        });
        addButton(layout, "Copier le journal technique (sans données de compte)", v -> {
            String diagnostics = new NightTimeline(this).recent();
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                    "Validate Steps : exécutions locales", diagnostics));
                info.append("\nJournal copié.");
            }
        });
        addButton(layout, "Effacer le journal local", v -> {
            new NightTimeline(this).clear();
            refresh();
        });
        history = new TextView(this);
        history.setTextSize(12);
        history.setTextIsSelectable(true);
        layout.addView(history);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.addView(layout);
        setContentView(scroller);
        refresh();
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(listener);
        parent.addView(button);
    }

    @Override protected void onResume() {
        super.onResume();
        if (info != null) refresh();
    }

    @Override public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refresh();
    }

    private void refresh() {
        RunStore store = new RunStore(this);
        boolean health = checkSelfPermission(HealthReader.READ_STEPS) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) ==
            PackageManager.PERMISSION_GRANTED;
        info.setText("VALIDATE STEPS 0.11\n\n" +
            "Lecture à 22 h : " + (store.enabled() ? "ACTIVE" : "INACTIVE") +
            "\nSurveillance de secours : " + (store.enabled() ? "2 contrôles planifiés (22 h 20 / 23 h 15)" : "inactive") +
            "\nSanté Connect arrière-plan pris en charge : " +
                (HealthReader.backgroundSupported() ? "oui" : "non") +
            "\nAlarme exacte : " + (DailyScheduler.exactAllowed(this) ? "autorisée" : "non autorisée") +
            "\nSanté Connect (arrière-plan) : " + (health ? "autorisée" : "non autorisée") +
            "\nAccès notifications Android : " +
                (RewardActionListener.hasPermission(this) ? "autorisé" :
                    "non autorisé (APK externe : voir paramètres restreints)") +
            "\n" + ProviderPackages.diagnostic(this) +
            "\n\n" + store.describe() +
            "\n\n" + new ActionBridgeState(this).summary() +
            "\n\nLa lecture s'effectue sans ouvrir d'application. Ce relevé ne valide " +
            "PAS les pièces WeWard, Macadam ou Winwalk. Sweatcoin est géré " +
            "indépendamment par sa propre application.");
        if (history != null) {
            history.setText("\nHISTORIQUE D'EXÉCUTION (18 événements récents)\n"
                + new NightTimeline(this).recent()
                + "\n\nALARM = alarme Android, WATCHDOG = secours, "
                + "AUDIT = lecture réelle des pas, CLAIM_ACTION = action de "
                + "notification envoyée (crédit NON confirmé).");
        }
    }
}
