package fr.validatesteps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;

/** Optional one-time setup and diagnostics; never launched by the nightly worker. */
public final class MainActivity extends Activity {
    private TextView info;

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
                info.append("\\nAccès en arrière-plan indisponible : mise à jour système nécessaire.");
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
        addButton(layout, "Désactiver et annuler les alarmes", v -> {
            new RunStore(this).enable(false);
            DailyScheduler.schedule(this);
            refresh();
        });
        setContentView(layout);
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
        info.setText("VALIDATE STEPS 0.5\n\n" +
            "Lecture à 22 h : " + (store.enabled() ? "ACTIVE" : "INACTIVE") +
            "\nSurveillance de secours : " + (store.enabled() ? "2 contrôles planifiés (22 h 20 / 23 h 15)" : "inactive") +
            "\nSanté Connect arrière-plan pris en charge : " +
                (HealthReader.backgroundSupported() ? "oui" : "non") +
            "\nAlarme exacte : " + (DailyScheduler.exactAllowed(this) ? "autorisée" : "non autorisée") +
            "\nSanté Connect (arrière-plan) : " + (health ? "autorisée" : "non autorisée") +
            "\n\n" + store.describe() +
            "\n\nLa lecture s'effectue sans ouvrir d'application. Ce relevé ne valide " +
            "PAS les pièces WeWard, Macadam ou Winwalk. Sweatcoin est géré " +
            "indépendamment par sa propre application.");
    }
}
