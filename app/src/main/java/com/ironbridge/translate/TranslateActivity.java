package com.ironbridge.translate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class TranslateActivity extends Activity {

    private static final String TAG = "IronBridgeTranslate";
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

        CharSequence selectedText = getIntent() != null
                ? getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                : null;

        String text = selectedText != null ? selectedText.toString().trim() : "";
        if (text.isEmpty()) {
            finish();
            return;
        }

        LocalBridgeServer server = null;
        boolean launched = false;
        try {
            server = LocalBridgeServer.getInstance();
            server.ensureStarted();

            String targetPackage = findInstalledBrowser();
            if (targetPackage == null) {
                Toast.makeText(this, R.string.no_browser_found, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "No compatible Firefox-based browser found.");
                finish();
                return;
            }

            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
            String url = "http://127.0.0.1:" + server.getPort() + "/translate?q=" + encodedText + "&lang=en";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(targetPackage);

            Log.d(TAG, "Launching local HTTP bridge via: " + targetPackage + " -> " + url);
            startActivity(intent);
            launched = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch local HTTP bridge", e);
            Toast.makeText(this, R.string.bridge_start_failed, Toast.LENGTH_SHORT).show();
            if (server != null) {
                server.stop();
            }
        } finally {
            if (!launched && server != null) {
                server.stop();
            }
            finish();
        }
    }

    private String findInstalledBrowser() {
        for (String packageName : FIREFOX_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
                return packageName;
            } catch (Exception ignored) {
                // Keep probing the fallback list.
            }
        }
        return null;
    }
}
