# IronBridge Translate - Technical Notes

## Purpose

IronBridge Translate is a minimal Java Android app that implements `PROCESS_TEXT` and performs offline-first translation in a bottom sheet using ML Kit.

## Data flow

```text
User selects text
  -> Android invokes TranslateActivity via android.intent.action.PROCESS_TEXT
  -> app reads Intent.EXTRA_PROCESS_TEXT
  -> app shows a bottom sheet UI
  -> ML Kit language ID detects the source language
  -> ML Kit Translation resolves a target language
  -> Translator.downloadModelIfNeeded(...)
  -> Translator.translate(...)
  -> translated text shown in the sheet
  -> optional clipboard copy
  -> finish()
```

## Manifest constraints

- No `android.permission.INTERNET`
- No browser package queries
- No `FileProvider`
- Activity is exported so the system text-selection menu can invoke it

## Runtime behavior

- The activity is intentionally short-lived.
- It exits when the sheet is dismissed.
- There is no background service.
- There is no local HTTP server.
- There is no `WebView`.

## ML Kit behavior

- Language detection uses ML Kit's on-device language ID API.
- Translation uses ML Kit's on-device translation API.
- The app only uses supported translation language tags from `TranslateLanguage.getAllLanguages()`.
- A translation model may still need to be provisioned by Google Play services the first time a language pair is used.
- After the model is available, translation runs locally on the device.

## UI

- The bottom sheet shows the original text, detected source language, selectable target language, translation status, and translated output.
- Copy-to-clipboard is available after translation completes.

## Build

```bash
./gradlew :app:assembleDebug
```

Environment:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34
