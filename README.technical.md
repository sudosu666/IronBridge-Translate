# IronBridge Translate - Technical Notes

## Purpose

IronBridge Translate is a minimal Java Android app that implements `PROCESS_TEXT` and performs offline translation in a bottom sheet using a user-supplied TensorFlow Lite model.

## Data flow

```text
User selects text
  -> Android invokes TranslateActivity via android.intent.action.PROCESS_TEXT
  -> app reads Intent.EXTRA_PROCESS_TEXT
  -> app shows a bottom sheet UI
  -> user picks a local .tflite model via ACTION_OPEN_DOCUMENT
  -> app copies the model into internal storage
  -> app loads the model with TFLite Interpreter
  -> app validates that the model exposes one STRING input and one STRING output
  -> app runs Interpreter.runForMultipleInputsOutputs(...)
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
- The app copies the selected model into private storage before loading it.

## Model contract

This implementation expects a TFLite model that:

- accepts exactly one input tensor
- returns exactly one output tensor
- uses `STRING` for both tensors

If the supplied model uses token IDs, float tensors, or a different signature, the app will reject it with a clear error instead of pretending the pipeline is compatible.

## UI

- The bottom sheet shows the original text, model status, translation status, and translated output.
- `Pick model` launches SAF to choose a `.tflite` file.
- `Translate` reruns inference with the currently loaded model.
- Copy-to-clipboard is available after translation completes.

## Build

```bash
./gradlew :app:assembleDebug
```

Environment:

- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34
