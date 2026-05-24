# IronBridge Translate

IronBridge Translate is a tiny Android `PROCESS_TEXT` companion app that forwards selected text to a Firefox-based browser through a local HTTP bridge on `127.0.0.1`.

The app is privacy-first:

- no `INTERNET` permission
- no background service
- no analytics
- no account sign-in
- no remote server

## What it does

1. Android sends selected text to `TranslateActivity` through `android.intent.action.PROCESS_TEXT`.
2. The app URL-encodes that text with UTF-8.
3. A tiny local HTTP server serves an HTML page from `http://127.0.0.1:8080/translate`.
4. The app launches the first compatible Firefox-family browser with an `ACTION_VIEW` intent.
5. Firefox opens the page locally and can show its built-in offline translation UI.

## Privacy model

IronBridge Translate does not ask for network access in its manifest. The only network path involved is the loopback interface on the device itself.

Important nuance:

- the selected text stays on the device
- the bridge is local only
- the app does not speak to external servers
- browser support depends on the installed Firefox-family build

## Supported browsers

The app probes these packages in order:

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

Requirements:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34

If Gradle needs your SDK path, create `local.properties` in the project root:

```properties
sdk.dir=/home/niko/Android/Sdk
```

## Project layout

```text
app/src/main/
  AndroidManifest.xml
  java/com/ironbridge/translate/
    TranslateActivity.java
    LocalBridgeServer.java
  res/
    values/
    values-night/
```

## License

This project is licensed under the [MIT License](./LICENSE).
