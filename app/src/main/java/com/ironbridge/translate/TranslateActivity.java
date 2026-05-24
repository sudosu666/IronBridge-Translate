package com.ironbridge.translate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TranslateActivity extends Activity {

    private static final String TAG = "IronBridgeTranslate";
    private static final String DEFAULT_LANGUAGE = TranslateLanguage.ENGLISH;
    private static final String UNDETERMINED = "und";

    private BottomSheetDialog bottomSheetDialog;
    private Translator activeTranslator;
    private String selectedText = "";
    private String detectedSourceLanguage = DEFAULT_LANGUAGE;
    private String chosenTargetLanguage = DEFAULT_LANGUAGE;
    private Spinner targetLanguageSpinner;
    private List<String> supportedTargetLanguages = new ArrayList<>();

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

        showTranslationSheet();
        detectLanguageAndTranslate();
    }

    @Override
    protected void onDestroy() {
        closeTranslator();
        dismissSheet();
        super.onDestroy();
    }

    private void showTranslationSheet() {
        if (bottomSheetDialog != null) {
            return;
        }

        bottomSheetDialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_translate, null, false);
        bottomSheetDialog.setContentView(content);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(false);
        bottomSheetDialog.setOnDismissListener(dialog -> finish());

        TextView sourceText = content.findViewById(R.id.source_text);
        TextView languageLabel = content.findViewById(R.id.language_label);
        TextView statusText = content.findViewById(R.id.status_text);
        TextView translatedText = content.findViewById(R.id.translated_text);
        ProgressBar progressBar = content.findViewById(R.id.progress_bar);
        Spinner targetSpinner = content.findViewById(R.id.target_language_spinner);
        Button copyButton = content.findViewById(R.id.copy_button);
        Button closeButton = content.findViewById(R.id.close_button);

        sourceText.setText(selectedText);
        languageLabel.setText("Preparing on-device translation...");
        statusText.setText("Detecting source language");
        translatedText.setText("");
        progressBar.setVisibility(View.VISIBLE);
        copyButton.setEnabled(false);

        supportedTargetLanguages = new ArrayList<>(TranslateLanguage.getAllLanguages());
        Collections.sort(supportedTargetLanguages, (left, right) ->
                displayLanguageName(left).compareToIgnoreCase(displayLanguageName(right)));
        chosenTargetLanguage = supportedTargetLanguages.get(findDefaultTargetIndex(supportedTargetLanguages, detectedSourceLanguage));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildLanguageLabels(supportedTargetLanguages)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetSpinner.setAdapter(adapter);
        targetLanguageSpinner = targetSpinner;
        targetSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            chosenTargetLanguage = supportedTargetLanguages.get(position);
            languageLabel.setText(buildLanguageLine(detectedSourceLanguage, chosenTargetLanguage));
        }));
        targetSpinner.setSelection(supportedTargetLanguages.indexOf(chosenTargetLanguage));

        copyButton.setOnClickListener(v -> copyToClipboard(translatedText.getText().toString()));
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void detectLanguageAndTranslate() {
        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(selectedText)
                .addOnSuccessListener(languageCode -> {
                    detectedSourceLanguage = normalizeLanguageCode(languageCode);
                    if (TranslateLanguage.fromLanguageTag(detectedSourceLanguage) == null) {
                        detectedSourceLanguage = DEFAULT_LANGUAGE;
                    }

                    if (TranslateLanguage.fromLanguageTag(chosenTargetLanguage) == null) {
                        chosenTargetLanguage = DEFAULT_LANGUAGE;
                    }

                    if (detectedSourceLanguage.equals(chosenTargetLanguage)) {
                        chosenTargetLanguage = chooseAlternateTarget(detectedSourceLanguage);
                        if (targetLanguageSpinner != null && !supportedTargetLanguages.isEmpty()) {
                            targetLanguageSpinner.setSelection(supportedTargetLanguages.indexOf(chosenTargetLanguage));
                        }
                    }

                    updateStatus(buildLanguageLine(detectedSourceLanguage, chosenTargetLanguage),
                            "Downloading offline model if needed");
                    translateText(detectedSourceLanguage, chosenTargetLanguage);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Language detection failed", e);
                    detectedSourceLanguage = DEFAULT_LANGUAGE;
                    if (TranslateLanguage.fromLanguageTag(chosenTargetLanguage) == null) {
                        chosenTargetLanguage = DEFAULT_LANGUAGE;
                    }
                    updateStatus("Language detection failed", "Trying offline translation anyway");
                    translateText(detectedSourceLanguage, chosenTargetLanguage);
                });
    }

    private void translateText(String sourceLanguage, String targetLanguage) {
        closeTranslator();

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build();
        activeTranslator = Translation.getClient(options);

        if (bottomSheetDialog == null) {
            return;
        }

        ProgressBar progressBar = bottomSheetDialog.findViewById(R.id.progress_bar);
        TextView translatedText = bottomSheetDialog.findViewById(R.id.translated_text);
        TextView statusText = bottomSheetDialog.findViewById(R.id.status_text);
        Button copyButton = bottomSheetDialog.findViewById(R.id.copy_button);

        if (progressBar == null || translatedText == null || statusText == null || copyButton == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Preparing translation model");

        DownloadConditions conditions = new DownloadConditions.Builder().build();
        activeTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    statusText.setText("Translating offline");
                    activeTranslator.translate(selectedText)
                            .addOnSuccessListener(result -> {
                                progressBar.setVisibility(View.GONE);
                                translatedText.setText(result);
                                statusText.setText("Translation ready");
                                copyButton.setEnabled(true);
                            })
                            .addOnFailureListener(e -> handleTranslationFailure(statusText, progressBar, e));
                })
                .addOnFailureListener(e -> handleTranslationFailure(statusText, progressBar, e));
    }

    private void handleTranslationFailure(TextView statusText, ProgressBar progressBar, Exception e) {
        Log.e(TAG, "Translation failed", e);
        progressBar.setVisibility(View.GONE);
        statusText.setText("Translation unavailable: " + e.getMessage());
        Toast.makeText(this, "Offline model is not ready yet", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String languageLine, String statusLine) {
        if (bottomSheetDialog == null) {
            return;
        }

        TextView languageLabel = bottomSheetDialog.findViewById(R.id.language_label);
        TextView statusText = bottomSheetDialog.findViewById(R.id.status_text);
        if (languageLabel != null) {
            languageLabel.setText(languageLine);
        }
        if (statusText != null) {
            statusText.setText(statusLine);
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
        Toast.makeText(this, "Translated text copied", Toast.LENGTH_SHORT).show();
    }

    private void closeTranslator() {
        if (activeTranslator != null) {
            activeTranslator.close();
            activeTranslator = null;
        }
    }

    private void dismissSheet() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
        bottomSheetDialog = null;
    }

    private List<String> buildLanguageLabels(List<String> languageTags) {
        List<String> labels = new ArrayList<>(languageTags.size());
        for (String languageTag : languageTags) {
            labels.add(displayLanguageName(languageTag) + " (" + languageTag + ")");
        }
        return labels;
    }

    private int findDefaultTargetIndex(List<String> languageTags, String sourceLanguage) {
        String preferred = preferredTargetLanguage();
        int preferredIndex = languageTags.indexOf(preferred);
        if (preferredIndex >= 0 && !preferred.equals(sourceLanguage)) {
            return preferredIndex;
        }

        int englishIndex = languageTags.indexOf(DEFAULT_LANGUAGE);
        if (englishIndex >= 0 && !DEFAULT_LANGUAGE.equals(sourceLanguage)) {
            return englishIndex;
        }

        for (int i = 0; i < languageTags.size(); i++) {
            if (!languageTags.get(i).equals(sourceLanguage)) {
                return i;
            }
        }
        return 0;
    }

    private String preferredTargetLanguage() {
        String languageTag = Locale.getDefault().toLanguageTag();
        String supported = TranslateLanguage.fromLanguageTag(languageTag);
        if (supported != null) {
            return supported;
        }
        return DEFAULT_LANGUAGE;
    }

    private String chooseAlternateTarget(String sourceLanguage) {
        String[] fallbackTargets = {
                TranslateLanguage.ENGLISH,
                TranslateLanguage.UKRAINIAN,
                TranslateLanguage.RUSSIAN,
                TranslateLanguage.GERMAN,
                TranslateLanguage.FRENCH,
                TranslateLanguage.POLISH,
                TranslateLanguage.SPANISH,
                TranslateLanguage.ITALIAN
        };

        for (String candidate : fallbackTargets) {
            if (!candidate.equals(sourceLanguage) && TranslateLanguage.fromLanguageTag(candidate) != null) {
                return candidate;
            }
        }
        return DEFAULT_LANGUAGE;
    }

    private String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || UNDETERMINED.equals(languageCode)) {
            return DEFAULT_LANGUAGE;
        }

        String supported = TranslateLanguage.fromLanguageTag(languageCode);
        return supported != null ? supported : DEFAULT_LANGUAGE;
    }

    private String buildLanguageLine(String sourceLanguage, String targetLanguage) {
        return "Source: " + displayLanguageName(sourceLanguage)
                + "  •  Target: " + displayLanguageName(targetLanguage);
    }

    private String displayLanguageName(String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);
        String displayName = locale.getDisplayName(Locale.getDefault());
        if (displayName == null || displayName.trim().isEmpty()) {
            return languageTag;
        }
        return Character.toUpperCase(displayName.charAt(0)) + displayName.substring(1);
    }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final OnIndexSelectedListener onIndexSelectedListener;

        SimpleItemSelectedListener(OnIndexSelectedListener onIndexSelectedListener) {
            this.onIndexSelectedListener = onIndexSelectedListener;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onIndexSelectedListener.onSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
            // No-op.
        }
    }

    private interface OnIndexSelectedListener {
        void onSelected(int position);
    }
}
