package fr.validatesteps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** First-run system consent only. No UI is opened by the nightly worker. */
public final class MainActivity extends Activity {
    private LinearLayout layout;
    private TextView info;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(34, 46, 34, 34);

        info = new TextView(this);
        info.setTextSize(18);
        layout.addView(info);
        addButton("Autoriser les données Santé Connect", v ->
            requestPermissions(new String[] {
                HealthReader.READ_STEPS, HealthReader.READ_BACKGROUND
            }, 10));
        addButton("Autoriser les notifications", v ->
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 11));
        addButton("Autoriser les alarmes exactes à 22 h", v -> {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        addButton("Activer 22 h en arrière-plan", v -> {
            new RunStore(this).enable(true);
            DailyScheduler.schedule(this);
            refresh();
        });
        addButton("Désactiver", v -> {
            new RunStore(this).enable(false);
            DailyScheduler.schedule(this);
            refresh();
        });
        setContentView(layout);
        refresh();
    }

    private void addButton(String label, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(onClick);
        layout.addView(button);
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
        String enabled = store.enabled() ? "ACTIF" : "INACTIF";
        String exact = DailyScheduler.exactAllowed(this) ? "oui" : "non";
        boolean health = checkSelfPermission(HealthReader.READ_STEPS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(HealthReader.READ_BACKGROUND) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED;
        info.setText("VALIDATE STEPS - configuration initiale\n\n" +
            "Tâche quotidienne à 22 h : " + enabled + "\n" +
            "Alarme exacte autorisée : " + exact + "\n" +
            "Lecture Santé Connect en arrière-plan : " + (health ? "oui" : "non") +
            "\n\n" + store.describe() + "\n\n" +
            "Une lecture de pas n'est PAS une validation de récompenses WeWard, " +
            "Macadam ou Winwalk. Aucun écran ne sera ouvert automatiquement.");
    }
}
