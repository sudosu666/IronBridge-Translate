package com.ironbridge.translate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TranslateActivity extends Activity {

    private static final String TAG = "IronBridgeTranslate";
    private static final int REQUEST_PICK_MODEL = 1001;
    private static final String PREFS_NAME = "ironbridge_translate";
    private static final String PREF_MODEL_PATH = "model_path";
    private static final String PREF_MODEL_NAME = "model_name";
    private static final String MODEL_DIRECTORY = "models";
    private static final String DEFAULT_MODEL_NAME = "en_ru.tflite";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BottomSheetDialog bottomSheetDialog;
    private TfliteTranslationEngine translationEngine;
    private String selectedText = "";
    private File activeModelFile;
    private String activeModelDisplayName = "";
    private boolean modelReady;

    private TextView modelStatusView;
    private TextView sourceTextView;
    private TextView translationStatusView;
    private TextView translatedTextView;
    private ProgressBar progressBar;
    private Button pickModelButton;
    private Button translateButton;
    private Button copyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence text = getIntent() != null
                ? getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                : null;
        selectedText = text != null ? text.toString().trim() : "";

        if (selectedText.isEmpty()) {
            finish();
            return;
        }

        translationEngine = new TfliteTranslationEngine();
        showBottomSheet();
        restoreStoredModel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bottomSheetDialog != null) {
            bottomSheetDialog.dismiss();
            bottomSheetDialog = null;
        }
        executor.shutdownNow();
        if (translationEngine != null) {
            translationEngine.close();
            translationEngine = null;
        }
    }

    private void showBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_translate, null, false);
        bottomSheetDialog.setContentView(content);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(false);
        bottomSheetDialog.setOnDismissListener(dialog -> finish());

        TextView titleView = content.findViewById(R.id.sheet_title);
        TextView hintView = content.findViewById(R.id.model_hint);
        modelStatusView = content.findViewById(R.id.model_status);
        sourceTextView = content.findViewById(R.id.source_text);
        translationStatusView = content.findViewById(R.id.translation_status);
        translatedTextView = content.findViewById(R.id.translated_text);
        progressBar = content.findViewById(R.id.progress_bar);
        pickModelButton = content.findViewById(R.id.pick_model_button);
        translateButton = content.findViewById(R.id.translate_button);
        copyButton = content.findViewById(R.id.copy_button);
        Button closeButton = content.findViewById(R.id.close_button);

        titleView.setText(getString(R.string.bottom_sheet_title));
        hintView.setText(getString(R.string.model_hint, DEFAULT_MODEL_NAME));
        sourceTextView.setText(selectedText);
        modelStatusView.setText(getString(R.string.no_model_loaded));
        translationStatusView.setText(getString(R.string.ready_to_pick_model));
        translatedTextView.setText("");
        progressBar.setVisibility(View.GONE);

        pickModelButton.setOnClickListener(v -> launchModelPicker());
        translateButton.setOnClickListener(v -> translateSelectedText());
        copyButton.setOnClickListener(v -> copyToClipboard(translatedTextView.getText().toString()));
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        updateUiForMissingModel();
        bottomSheetDialog.show();
    }

    private void restoreStoredModel() {
        File storedModel = getStoredModelFile();
        String storedName = getStoredModelName();

        if (storedModel != null && storedModel.exists()) {
            activeModelFile = storedModel;
            activeModelDisplayName = storedName != null && !storedName.trim().isEmpty()
                    ? storedName
                    : storedModel.getName();
            loadModelAsync(storedModel, activeModelDisplayName, true);
            return;
        }

        updateModelStatus(getString(R.string.no_model_loaded), getString(R.string.pick_model_prompt));
    }

    private void launchModelPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_model_button)), REQUEST_PICK_MODEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PICK_MODEL || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        String displayName = resolveDisplayName(uri);

        try {
            File copiedModel = copyModelIntoPrivateStorage(uri, displayName);
            persistModelReference(copiedModel, displayName);
            activeModelFile = copiedModel;
            activeModelDisplayName = displayName;
            loadModelAsync(copiedModel, displayName, true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy selected model", e);
            Toast.makeText(this, getString(R.string.model_copy_failed), Toast.LENGTH_SHORT).show();
            updateModelStatus(getString(R.string.model_copy_failed), e.getMessage());
        }
    }

    private void loadModelAsync(File modelFile, String displayName, boolean autoTranslate) {
        updateProgress(true, getString(R.string.loading_model));
        executor.submit(() -> {
            try {
                translationEngine.load(modelFile);
                modelReady = true;
                persistModelReference(modelFile, displayName);
                runOnUiThread(() -> {
                    updateModelStatus(
                            getString(R.string.model_loaded, displayName),
                            getString(R.string.model_ready_offline)
                    );
                    translateButton.setEnabled(true);
                    pickModelButton.setText(getString(R.string.replace_model_button));
                    updateProgress(false, getString(R.string.model_ready_offline));
                    if (autoTranslate) {
                        translateSelectedText();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load TFLite model", e);
                modelReady = false;
                runOnUiThread(() -> {
                    updateModelStatus(
                            getString(R.string.model_load_failed),
                            safeMessage(e)
                    );
                    updateUiForMissingModel();
                    Toast.makeText(this, getString(R.string.model_load_failed_toast), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void translateSelectedText() {
        if (!modelReady || activeModelFile == null) {
            updateModelStatus(getString(R.string.no_model_loaded), getString(R.string.pick_model_prompt));
            updateUiForMissingModel();
            return;
        }

        updateProgress(true, getString(R.string.translating_offline));
        translationStatusView.setText(getString(R.string.translating_offline));
        translatedTextView.setText("");
        copyButton.setEnabled(false);

        executor.submit(() -> {
            try {
                String translated = translationEngine.translate(selectedText);
                runOnUiThread(() -> {
                    updateProgress(false, getString(R.string.translation_ready));
                    translatedTextView.setText(translated);
                    translationStatusView.setText(getString(R.string.translation_ready));
                    copyButton.setEnabled(true);
                });
            } catch (Exception e) {
                Log.e(TAG, "Translation failed", e);
                runOnUiThread(() -> {
                    updateProgress(false, getString(R.string.translation_failed));
                    translationStatusView.setText(getString(R.string.translation_failed));
                    translatedTextView.setText(safeMessage(e));
                    Toast.makeText(this, getString(R.string.translation_failed_toast), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUiForMissingModel() {
        if (pickModelButton != null) {
            pickModelButton.setEnabled(true);
            pickModelButton.setText(getString(R.string.pick_model_button));
        }
        if (translateButton != null) {
            translateButton.setEnabled(false);
        }
        if (copyButton != null) {
            copyButton.setEnabled(false);
        }
    }

    private void updateModelStatus(String title, String subtitle) {
        if (modelStatusView != null) {
            modelStatusView.setText(title);
        }
        if (translationStatusView != null) {
            translationStatusView.setText(subtitle);
        }
    }

    private void updateProgress(boolean visible, String statusText) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (translationStatusView != null && statusText != null) {
            translationStatusView.setText(statusText);
        }
    }

    private void copyToClipboard(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        clipboardManager.setPrimaryClip(ClipData.newPlainText("IronBridge Translate", value));
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
    }

    private File copyModelIntoPrivateStorage(Uri uri, String displayName) throws IOException {
        File modelDir = new File(getFilesDir(), MODEL_DIRECTORY);
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            throw new IOException("Unable to create model directory");
        }

        String safeName = sanitizeFileName(displayName);
        if (!safeName.toLowerCase(Locale.US).endsWith(".tflite")) {
            safeName = safeName + ".tflite";
        }

        File destination = new File(modelDir, safeName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destination, false)) {
            if (inputStream == null) {
                throw new IOException("Unable to open selected file");
            }
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return destination;
    }

    private String resolveDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String displayName = cursor.getString(nameIndex);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        String lastSegment = uri.getLastPathSegment();
        if (lastSegment != null && !lastSegment.trim().isEmpty()) {
            return lastSegment;
        }
        return DEFAULT_MODEL_NAME;
    }

    private void persistModelReference(File file, String displayName) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_MODEL_PATH, file.getAbsolutePath())
                .putString(PREF_MODEL_NAME, displayName)
                .apply();
    }

    private File getStoredModelFile() {
        String path = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_MODEL_PATH, null);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return new File(path);
    }

    private String getStoredModelName() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_MODEL_NAME, null);
    }

    private String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return DEFAULT_MODEL_NAME;
        }
        return rawName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }
}
