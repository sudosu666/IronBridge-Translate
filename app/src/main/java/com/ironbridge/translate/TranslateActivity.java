package com.ironbridge.translate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class TranslateActivity extends Activity {

    private static final String TAG = "IronBridgeTranslate";
    private static final String FILE_PROVIDER_AUTHORITY = "com.ironbridge.translate.fileprovider";

    private static final String[] FIREFOX_PACKAGES = {
            "org.ironfox.browser",
            "us.mull.mull",
            "org.mozilla.fennec_fdroid",
            "io.github.forkmaintainers.iceraven",
            "net.waterfox.android.release",
            "org.mozilla.firefox",
            "org.mozilla.fenix",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix.nightly",
            "org.mozilla.focus",
            "com.cookiecutter.cookiecutter"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence text = getIntent() != null
                ? getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                : null;
        String textToTranslate = text != null ? text.toString() : "";

        if (!textToTranslate.isEmpty()) {
            String targetPackage = findInstalledBrowser();
            if (targetPackage != null) {
                launchWithLocalFile(targetPackage, textToTranslate);
            } else {
                Log.d(TAG, "No compatible Firefox-based browser found.");
            }
        }

        finish();
    }

    private String findInstalledBrowser() {
        for (String packageName : FIREFOX_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
                return packageName;
            } catch (Exception ignored) {
                // Not installed, continue probing the fallback list.
            }
        }
        return null;
    }

    private void launchWithLocalFile(String targetPackage, String text) {
        try {
            String htmlContent = buildHtmlContent(text);

            File cacheDir = new File(getCacheDir(), "shared");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw new IllegalStateException("Unable to create cache directory");
            }

            File htmlFile = new File(cacheDir, "translate.html");
            try (FileOutputStream stream = new FileOutputStream(htmlFile, false)) {
                stream.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            }

            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    FILE_PROVIDER_AUTHORITY,
                    htmlFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "text/html");
            intent.setPackage(targetPackage);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching Firefox viewer", e);
        }
    }

    private String buildHtmlContent(String text) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<title>IronBridge Translate</title>"
                + "<style>"
                + "body{background:#111;color:#eee;font-family:sans-serif;padding:20px;font-size:18px;line-height:1.6;}"
                + ".badge{font-size:12px;color:#888;margin-bottom:10px;font-weight:bold;}"
                + "</style></head><body>"
                + "<div class=\"badge\">IRONBRIDGE OFFLINE TRANSLATE</div>"
                + "<div id=\"content\">" + escapeHtml(text) + "</div>"
                + "</body></html>";
    }

    private String escapeHtml(String str) {
        return str
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }
}
