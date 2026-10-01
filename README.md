# Rubicon demo app

The app to try every behaviour of the Rubicon Android SDK by hand, against a fake backend that you
control from the app itself. It is a test harness: every screen and button triggers one SDK
behaviour and says what to expect.

## Run it

1. JDK 17 for Gradle: set it as the Gradle JVM in the IDE, or `org.gradle.java.home=<JDK 17>` in
   `~/.gradle/gradle.properties`.
2. The SDK is built from its checkout (composite build). By default it is expected at
   `../../rubicon-android-sdk` relative to this folder; elsewhere, set `rubicon.sdk.dir=<path>` in
   `~/.gradle/gradle.properties`.
3. Start the emulator: `~/Library/Android/sdk/emulator/emulator -avd Pixel8_API36`.
4. Start the fake backend on the Mac: `python3 fake-server/server.py` (the emulator reaches it at
   `http://10.0.2.2:8080`).
5. Once per emulator start, for the app's own HTTP calls (interceptor tests):
   `adb reverse tcp:8080 tcp:8080`.
6. `./gradlew :app:installDebug`, then open Rubicon.

## The app

| Tab | What it tries |
|---|---|
| Events | Quick events, an event builder with typed properties, every allowed value type, every validation rule (bad types and keys, depth, long strings and names, 32 KB, surrogates, the deep copy), volume and threads, the `beforeSend` hooks |
| Identity | `deviceId`, `identify` with and without traits, another user, a blank id, `reset`, super properties, `optIn`, `optOut`, `clearLocalData`, "forget me" |
| Capture | Automatic screens (annotation, class name, a plain `Activity`, a system dialog, rotation), deep links (utm, tokens, referrer, `singleTop` and `setIntent`), lifecycle and sessions, killing the process, the `:remote` process, nine HTTP calls through the interceptor |
| Server | The fake backend from the app: its batch responses (any status, `Retry-After`, delay, redirect, dropped connection), the remote configuration (kill switch, autocapture off, flush values, sampling), the events it received, and the requests it saw |
| Logs | The SDK's `eventObserver` (queued, sent, dropped with the reason) and its log lines, by level |

Settings (the gear) holds every SDK setting, the hooks, consent and the interceptor's hosts. The
SDK reads its configuration once, so saving restarts the app; that also shows that the SDK keeps
its state across a restart.

With `debug` on, the SDK throws on a developer's mistake; the app shows the exception as a message
instead of crashing, except where a hook breaks the rules on the SDK's own thread, which crashes on
purpose.

The manual checklist run before every release is in [`docs/manual-checklist.md`](docs/manual-checklist.md).

## Fake backend

`fake-server/server.py`, standard library only; its docstring lists every endpoint.

- `POST /v1/batch` stores the events and answers as `/control` says.
- `GET /v1/config` serves `fake-server/config/<api key>.json` with an ETag and 304, or 404.
- `GET /control?status=503&retry_after=30&delay_ms=0&mode=normal|redirect|drop`, `GET /control/reset`.
- `GET|POST|DELETE /control/config?key=...` reads, writes and deletes a remote configuration.
- `GET /events`, `GET /events/clear`, `GET /requests`.
- `/api/...` answers the app's own calls; `/api/status/<code>`, `/api/slow?ms=N`.

## Useful commands

```bash
adb logcat -s Rubicon                                                         # the SDK's log
adb shell am start -a android.intent.action.VIEW -d "rubicon://open/promo?utm_source=adb"   # deep link
adb shell svc wifi disable && adb shell svc data disable                      # offline
adb shell svc wifi enable && adb shell svc data enable                        # online
adb shell am force-stop com.rubicon.demo                                      # stop the app
./gradlew :app:installDebug -PdemoVersionCode=2                               # a higher version: Application Updated
```

## Branching

- `master`: releasable; only release and hotfix merges, each tagged.
- `develop`: integration branch; feature pull requests are squash-merged here.
- `feature/<topic>`, `release/<version>`, `hotfix/<topic>`: short-lived branches.

Commit and pull request titles follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
