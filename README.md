# IronBridge Translate

**IronBridge Translate** is a tiny Android `PROCESS_TEXT` companion app for private, offline text translation workflows.

Select text anywhere on Android, tap **IronBridge Translate**, and the app forwards that text to the first compatible Firefox-based browser through a fully local `data:text/html;base64,...` bridge.

## Why this exists

Most translation flows on mobile are either cloud-backed, heavyweight, or buried behind broken UI surfaces. IronBridge Translate keeps the experience simple:

- no `INTERNET` permission
- no background service
- no local web server
- no account sign-in
- no analytics

The app does one job and exits immediately.

## How it works

1. Android sends the selected text to `TranslateActivity` through `android.intent.action.PROCESS_TEXT`.
2. The app reads `Intent.EXTRA_PROCESS_TEXT` and URL-encodes it with UTF-8.
3. It checks a fixed list of 11 Firefox-family packages using `PackageManager.getPackageInfo(...)`.
4. The first installed browser is chosen with `.setPackage(...)`.
5. A local HTML page is injected as a single Base64 `data:` URI and opened with `Intent.ACTION_VIEW`.
6. The activity calls `finish()` right away.

## Privacy by design

- The manifest contains **zero internet permissions**.
- The selected text stays local to the app process.
- The app never creates a socket or localhost server.
- Browser discovery is limited to packages explicitly declared in `<queries>`.

## Supported browsers

The fallback order is:

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

## Screenshots

No separate UI. The app exists as a translucent system action.

## Build

```bash
./gradlew :app:assembleDebug
```

Requirements:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34

If Gradle needs your SDK path, create `local.properties`:

```properties
sdk.dir=/home/niko/Android/Sdk
```

## Project layout

```text
app/src/main/
  AndroidManifest.xml
  java/com/ironbridge/translate/TranslateActivity.java
  res/
    drawable/
    mipmap-anydpi-v26/
    values/
    values-night/
```

## License

Add your preferred license here before publishing.

