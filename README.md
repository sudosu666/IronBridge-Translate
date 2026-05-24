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
2. The app shows a compact bottom sheet with the source text.
3. You pick a compatible `.tflite` translation model from your device.
4. The model is copied into private app storage.
5. The app runs inference locally with TensorFlow Lite and shows the result.
6. You can copy the translated text to the clipboard.

## Privacy model

IronBridge Translate does not ask for network access in its manifest. Once a user supplies a compatible model file, translation runs entirely on-device.

Important nuance:

- the app itself is zero-permission with respect to networking
- the model file is user-provided and stored in private app storage
- translation is only as good as the TFLite model you provide
- this implementation expects a TFLite text-translation model with a single string input and a single string output

## Model setup

On first launch, or whenever no model is stored yet, the sheet tells you to pick a compatible model such as `en_ru.tflite`.

You can:

- choose a `.tflite` file with the system file picker
- let the app copy it into internal storage
- keep using it offline after that

## Supported behavior

- selected text from any app that supports `PROCESS_TEXT`
- local model selection through SAF file picker
- private model storage in the app sandbox
- offline inference with TensorFlow Lite
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
  java/com/ironbridge/translate/
    TranslateActivity.java
    TfliteTranslationEngine.java
  res/
    layout/
    values/
    values-night/
```

## License

This project is licensed under the [MIT License](./LICENSE).
