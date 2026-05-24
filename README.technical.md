# IronBridge Translate - Technical Notes

## Purpose

IronBridge Translate is a minimal Java Android app that implements `PROCESS_TEXT` and serves the selected text to a Firefox-family browser through a local HTTP bridge on `127.0.0.1`.

## Data flow

```text
User selects text
  -> Android invokes TranslateActivity via android.intent.action.PROCESS_TEXT
  -> app reads Intent.EXTRA_PROCESS_TEXT
  -> app URL-encodes the text with UTF-8
  -> app starts a NanoHTTPD server on 127.0.0.1:8080
  -> app launches Firefox-family browser with ACTION_VIEW
  -> browser opens http://127.0.0.1:8080/translate?q=...
  -> local HTML page reads q from location.search
  -> Firefox may show its built-in translation UI
  -> optional clipboard copy from the page's MutationObserver bridge
  -> finish()
```

## Manifest constraints

- No `android.permission.INTERNET`
- `<queries>` contains the 11 browser package names
- No `FileProvider`
- Activity is exported so the system text-selection menu can invoke it

## Runtime behavior

- The activity is intentionally short-lived.
- It exits immediately after dispatching the browser intent.
- There is no background service.
- There is no remote server.
- The server listens only on the device loopback interface.

## Browser bridge

- `LocalBridgeServer` extends NanoHTTPD.
- The server serves a single HTML page at `/translate`.
- The page contains a `<div id="text" lang="en" dir="ltr"></div>`.
- JavaScript reads `q` from `location.search`.
- A `MutationObserver` watches for page content changes and attempts to copy translated text back to the clipboard.

## Build

```bash
./gradlew :app:assembleDebug
```

Environment:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34
