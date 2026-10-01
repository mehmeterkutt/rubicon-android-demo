# Manual checklist

Run before every SDK release, with the demo app and the fake backend, and record the result in the
release pull request (SDK implementation plan, step 9.3). Setup: README "Run it"; the Server tab must
say "reachable". Settings changes need "Save and restart". Start from the defaults
(Settings > Defaults and restart).

| # | Scenario | Steps in the app | Expected |
|---|---|---|---|
| 1 | Fresh install | Uninstall, install, open, press Home | `Session Started`, `Application Installed` (version, build, installed_at), `Application Opened` (`from_background: false`), the screen `home`, then `Application Backgrounded` and an upload, all in one session |
| 2 | Background and return | Return within the timeout; then Settings > timeout 10 s, Save and restart, Home, wait 15 s, open | Within: same session, no `Session Started`. After: `Session Ended` backdated to the last activity with `duration_ms`, a new session, `Application Opened` with `from_background: true` |
| 3 | Offline | `svc wifi/data disable`, Events > Burst 50, wait, enable | Nothing arrives while offline; all 50 arrive once when online (Server tab) |
| 4 | Server errors | Server > 503 + Retry-After 30 > Apply, Events > Track, Flush; then 401 | 503: the retry comes 30 s later. 401: one request, then a pause of about an hour even after Reset to 200 and a new flush; one ERROR `UPLOAD_UNAUTHORIZED`. Identity > Clear local data ends the pause |
| 5 | Kill from recent apps | Events > Track ×5, swipe the app away in recents, open, Home | The 5 events arrive once |
| 6 | Opt-out and consent | Identity > Opt out, Events > Track, Purchase, Flush, Identity > Opt in, Events > Screen. Then Settings > pending, choice not stored, Save and restart, Track, Purchase, Identity > Opt in | Nothing from the opted-out period, no `Session Ended`, a new session after opting in, no opt-in event. Pending: nothing sent, not even the immediate Purchase; after opting in, every waiting event arrives |
| 7 | Identity | Identity > Identify, Events > Track, Identity > Reset, Track, Identify; change the id while identified, Identify | Events carry the user id; reset: `Session Ended` with the old user, then `user_id: null` and a new session; another id while identified: `Session Ended` for the old user, a new session for the new one |
| 8 | Deep links | Capture > Campaign link, Token in path, setIntent activity; a second `rubicon://legacy` link while it is shown; an adb cold start link | Only scheme, host, path, `utm_*` and referrer; the reset token becomes `:token`; never a private parameter or fragment |
| 9 | Network capture | Capture > every HTTP call; then Server > remote `autocapture_off: network_requests`, Save, Restart app, call again | Ids masked, no query or fragment, short errors (`timeout`, `connect`, `canceled`), unknown host and the SDK's host not captured. After the switch-off: the call works, nothing is captured |
| 10 | Kill switch | Server > Kill switch, Restart app, Track, Flush; then Delete, restart twice | Nothing collected or sent after the configuration arrives; after Delete (404), collection resumes from the following launch |
| 11 | Silent Logcat | Settings > warn, In-app logger off, Save and restart; use the app normally; `adb logcat -s Rubicon` | No line, apart from `CLEARTEXT_HOST` at start because the fake backend is http |

Also worth a look before a release: Events > every validation case and the hooks (Settings), the
:remote process (nothing arrives), the annotated activity (`product_detail`), a system dialog and a
rotation (no new screen view), debug on (a mistake is shown as "The SDK threw ...").

## Last run

2026-10-01, Pixel 8 emulator, API 36, demo `feature/sdk-integration`, SDK `feature/clear-ends-retry-wait`:
all 11 passed. Found and fixed on the way: `clearLocalData()` did not end a running retry wait (SDK);
the demo's depth example and hook hints were wrong (demo).
