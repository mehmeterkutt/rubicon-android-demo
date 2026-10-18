# Rubicon demo app

Android host app used to exercise the Rubicon analytics SDK by hand and in instrumented tests. The SDK
itself lives in its own repository; its design decisions are in Confluence (Software Development >
Analytics SDK Design Decisions). This repository holds only the app and a fake backend.

## Layout

| Path | What it is |
|---|---|
| `app/.../RubiconApp.kt` | `Application`; creates the analytics instance |
| `app/.../analytics/` | `RubiconAnalytics` (stand-in for the SDK's `Analytics`), `LoggingAnalytics` (its fake), `Diagnostics` (log entries for the Diagnostics screen) |
| `app/.../net/BackendProbe.kt` | Checks that the fake backend is reachable |
| `app/.../ui/` | Compose screens: Events, Identity, Diagnostics, Settings; `RubiconScaffold` with bottom navigation |
| `MainActivity`, `SettingsActivity` | Two activities, so automatic screen views can be observed; `MainActivity` is `singleTop` with the deep link `rubicon://open` |
| `fake-server/server.py` | Standard-library Python backend |

Package and application id: `com.rubicon.demo`.

## Stack

Kotlin 2.2.10, AGP 8.12.0, Gradle 8.14.3, Compose BOM 2025.06.01, navigation-compose 2.9.0, JDK 17,
minSdk 24, target and compile SDK 36. AGP stays at 8.12: IntelliJ 2025.3's Android plugin supports no
newer version. The JDK is set in `~/.gradle/gradle.properties`, not in the project.

## Commands

```bash
./gradlew :app:assembleDebug           # build
./gradlew :app:installDebug            # install on the running emulator
~/Library/Android/sdk/emulator/emulator -avd Pixel8_API36
python3 fake-server/server.py          # emulator reaches it at http://10.0.2.2:8080
```

adb recipes (logcat, deep links, offline, killing the process) are in `README.md`.

## Fake backend

- `POST /v1/batch`: accepts gzip or plain JSON, stores the events and answers with the current
  status.
- `GET /control?status=429&retry_after=30`: sets the status (and `Retry-After`) of the following
  responses until changed, to exercise the SDK's retry rules; `GET /control` shows the current setting.
  Its `invalid=` parameter (400 with `invalid_events`) predates the decision that the server drops
  invalid events itself and answers 200; it is to be removed.
- `GET /events`: everything received so far, as JSON.
- `GET /v1/config` (remote configuration) is not implemented yet; the SDK treats a 404 as "no remote
  values".

## Using the SDK

Until the SDK is published the app runs on `LoggingAnalytics`. When it is:

- Depend on the published `sdk-android` artifact; for local work against an SDK checkout, use a
  composite build (`includeBuild("../<sdk repo>")` in `settings.gradle.kts`) instead of copying code.
- Replace `RubiconAnalytics` with the SDK's `Analytics` and create it in `RubiconApp`:
  ```kotlin
  analytics = Analytics(this, AnalyticsConfig(BuildConfig.RUBICON_KEY).apply {
      debug = BuildConfig.DEBUG
      host = "http://10.0.2.2:8080"
      if (BuildConfig.DEBUG) {
          logging.level = LogLevel.DEBUG
          flush.at = 1; flush.intervalMs = 1_000
      }
      logging.eventObserver = diagnostics      // feeds the Diagnostics screen
  })
  ```
- Cleartext is allowed only for `10.0.2.2` and `localhost` (`res/xml/network_security_config.xml`).

## Rules

- The app is a test harness: every screen and button exists to trigger an SDK behaviour. No product
  features.
- Keep dependencies to what the scenarios need.
- Compose for UI, one file per screen under `ui/screens/`, shared pieces in `ui/components/`.
- Every change keeps `./gradlew :app:assembleDebug` green.

## Git

- `master` receives releases only, `develop` is the default branch, work happens on
  `feature/<topic>` and is squash-merged into `develop`; releases are merged into `master` with a
  merge commit and tagged.
- Commit and pull request titles follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`, `test:`). Pull requests use `.github/pull_request_template.md`.
- There is no branch protection (GitHub Free, private repository): never push to `master` or
  `develop` directly.
