# IronBridge Translate

IronBridge Translate is a tiny Android `PROCESS_TEXT` companion app that turns selected text into an offline translation inside a compact bottom sheet.

The app is privacy-first:

- no `INTERNET` permission
- no background service
- no browser handoff
- no analytics
- no account sign-in

## What it does

1. Android sends the selected text to `TranslateActivity` through `android.intent.action.PROCESS_TEXT`.
2. The app detects the source language locally with ML Kit.
3. It chooses a target language from the supported ML Kit translation set.
4. It downloads the required on-device translation model if needed.
5. It shows the result in a lightweight bottom sheet and lets you copy it to the clipboard.

## Privacy model

IronBridge Translate does not ask for network access in its manifest. Translation happens on-device through ML Kit once the language model exists on the phone.

Important nuance:

- the translation engine is offline once the model is present
- the model may be provisioned dynamically by Google Play services the first time a language pair is used
- if the model is not yet available, the app shows a clear failure state instead of pretending to translate

## Supported behavior

- selected text from any app that supports `PROCESS_TEXT`
- source language detection
- target language selection from ML Kit's supported languages
- translated output display
- one-tap copy to clipboard

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
  java/com/ironbridge/translate/TranslateActivity.java
  res/
    layout/
    values/
    values-night/
```

## License

This project is licensed under the [MIT License](./LICENSE).
