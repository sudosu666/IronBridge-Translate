# IronBridge Translate - Technical Notes

## Purpose

IronBridge Translate is a minimal Java Android app that implements `PROCESS_TEXT` and forwards highlighted text to a Firefox-family browser using a local `data:text/html;base64,...` bridge.

## Data Flow

```text
User selects text
  -> Android invokes TranslateActivity via android.intent.action.PROCESS_TEXT
  -> app reads Intent.EXTRA_PROCESS_TEXT
  -> app encodes text with URLEncoder UTF-8
  -> app scans 11 browser packages in priority order
  -> first installed package is selected
  -> ACTION_VIEW + setPackage(...) + data:text/html;base64,...
  -> finish()
```

## Manifest constraints

- No `android.permission.INTERNET`
- `<queries>` contains the 11 browser package names
- Activity is exported and translucent

## Runtime behavior

- The activity is intentionally non-interactive.
- It exits immediately after dispatching the browser intent.
- There is no background service and no local HTTP server.

## Browser fallback order

1. `org.ironfox.browser`
2. `us.mull.mull`
3. `org.mozilla.fennec_fdroid`
4. `io.github.forkmaintainers.iceraven`
5. `net.waterfox.android.release`
6. `org.mozilla.firefox`
7. `org.mozilla.fenix`
8. `org.mozilla.firefox_beta`
9. `org.mozilla.fenix.nightly`
10. `org.mozilla.focus`
11. `com.cookiecutter.cookiecutter`

## Build

```bash
./gradlew :app:assembleDebug
```

Environment:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34

