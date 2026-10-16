# Validate Steps — Android 15, 22:00

Open-source Android application for an unattended **daily 22:00 local-time step audit**. Installation and one-time health permission setup are required; the evening operation never opens an Activity or another app.

## Implemented
- Android **Health Connect** reads real, aggregated steps for the current local day (requires **READ_STEPS** and background health access).
- AlarmManager schedules 22:00 each day and re-schedules on restart, timezone/clock change, app update, or alarm-permission change.
- Exact alarms are used when the Android system grants permission; otherwise a power-saving fallback can be delayed.
- Local-only last execution, step total, and clear success/failure status.
- Optional notification with the real step tally and explicit provider-validation limitations.
- Build + unit tests via GitHub Actions; no advertisements, Internet permission, third-party credentials, or artificial steps.

## What it does NOT do
**It cannot automatically collect all rewards in WeWard, Macadam or Winwalk.** No official headless reward-claim API has been verified for these services. WeWard explicitly requires an app-open and manual validation. Macadam requires manual validation before midnight and prohibits automated access to its platform in its terms. Sweatcoin performs step conversion in its own background process but still has device-specific restrictions. A Health Connect read does not credit any provider wallet. This project never records a provider claim as successful.

## Installation on Redmi / Android 15
1. Go to **GitHub Actions → Android APK**, choose the last successful build and download the `validate-steps-debug` artifact (ZIP). Extract and install `app-debug.apk`.
2. Open **Validate Steps** once. Grant **Health Connect read steps and background data access** using Android's permission prompts/settings. Android may not offer background access on outdated Health Connect versions.
3. Allow **Alarms & reminders** for precise 22:00 execution and **Notifications** if desired.
4. Tap **Activer 22 h en arrière-plan**, set Battery → Validate Steps → **Sans restriction** (and Autostart if available on HyperOS).

No interaction with Validate Steps is required each evening. Android may delay a scheduled run due to battery management; **an exact time is not guaranteed without Android exact-alarm permission**. Reward collection within other apps remains manual.

## Developer commands
```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```
SDK 35, Java 17, Gradle 8.10.2. The app's minimum supported version is Android 14 (API 34). The version built by GitHub Actions is a debug APK for personal installation.

### Privacy
Data remains on the device in private SharedPreferences (`last_date`, `last_steps`, `last_status`, `last_executed_at`). No network permission, no step injection, no access to other accounts.

### Sources
- WeWard: https://faq.weward.fr/hc/fr/articles/28122448171794-Valider-mes-pas-et-obtenir-des-r%C3%A9compenses
- Macadam: https://help.macadam.app/fr/articles/469049-comment-fonctionnent-la-validation-des-pas-et-les-recompenses
- Macadam terms: https://www.macadam.app/fr/terms
- Android step aggregation: https://developer.android.com/health-and-fitness/health-connect/features/steps
- Android background reading: https://developer.android.com/health-and-fitness/health-connect/read-data
