# Rubicon

Host application used to exercise the analytics SDK (to be added as `:sdk-core` and `:sdk-android`).

## Run
1. Open this folder in IntelliJ IDEA (File > Open). Gradle needs JDK 17: either set it as the Gradle JVM in
   Settings > Build Tools > Gradle, or add `org.gradle.java.home=<path to JDK 17>` to `~/.gradle/gradle.properties`.
2. Start the emulator: Device Manager > Pixel8_API36 > Run, or `~/Library/Android/sdk/emulator/emulator -avd Pixel8_API36`.
3. Run the `app` configuration.
4. Fake backend on the Mac: `python3 fake-server/server.py` (emulator reaches it at http://10.0.2.2:8080).

## Useful adb commands
```
export PATH=$PATH:~/Library/Android/sdk/platform-tools
adb logcat -s Rubicon                               # app and SDK logs
adb shell am start -a android.intent.action.VIEW -d "rubicon://open?campaign=test"   # deep link
adb shell svc wifi disable && adb shell svc data disable            # go offline
adb shell svc wifi enable  && adb shell svc data enable             # back online
adb shell am kill com.rubicon.demo                        # kill process (background only)
adb shell am force-stop com.rubicon.demo                  # hard stop
```

## Branching

- `master`: releasable; only release and hotfix merges, each tagged.
- `develop`: integration branch; feature pull requests are squash-merged here.
- `feature/<topic>`, `release/<version>`, `hotfix/<topic>`: short-lived branches.

Commit and pull request titles follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
