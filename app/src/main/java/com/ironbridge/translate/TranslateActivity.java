package com.ironbridge.translate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public final class TranslateActivity extends Activity {

    private static final String TAG = "IronBridgeTranslate";
    private static final String PROCESS_TEXT_EXTRA = Intent.EXTRA_PROCESS_TEXT;
    private static final String UTF_8 = "UTF-8";
    private static final String QUERY_PREFIX = "#q=";
    private static final String QUERY_LANGUAGE = "&lang=en";
    private static final String NO_BROWSER_MESSAGE = "No compatible Firefox-based browser found.";
    static final String EXTRA_DIAGNOSTIC_REPORT = "com.ironbridge.translate.EXTRA_DIAGNOSTIC_REPORT";

    private static final String BASE64_HTML =
            "data:text/html;base64,PCFkb2N0eXBlIGh0bWw+CjxodG1sIGxhbmc9ImVuIj4KPGhlYWQ+CjxtZXRhIGNoYXJzZXQ9InV0Zi04Ij4KPHRpdGxlPk9mZmxpbmUgVHJhbnNsYXRlIEJyaWRnZTwvdGl0bGU+CjxzdHlsZT5ib2R5e21hcmdpbjowO2JhY2tncm91bmQ6IzExMTtjb2xvcjojZWVlO2ZvbnQtZmFtaWx5OnN5c3RlbS11aSxzYW5zLXNlcmlmO3BhZGRpbmc6MXJlbX0jdGV4dHt3aGl0ZS1zcGFjZTpwcmUtd3JhcDt3b3JkLWJyZWFrOmJyZWFrLXdvcmQ7cGFkZGluZzouOHJlbTtib3JkZXItcmFkaXVzOjE2cHg7YmFja2dyb3VuZDpyZ2JhKDI1NSwyNTUsMjU1LC4wNCk7Ym9yZGVyOjFweCBzb2xpZCAjMzMzO21pbi1oZWlnaHQ6NnJlbTtsaW5lLWhlaWdodDoxLjV9PC9zdHlsZT4KPC9oZWFkPgo8Ym9keT4KPGRpdiBpZD0idGV4dCIgbGFuZz0iZW4iIGRpcj0ibHRyIj48L2Rpdj4KPHNjcmlwdD4KKGZ1bmN0aW9uKCl7CiBjb25zdCBwYXJhbXM9bmV3IFVSTFNlYXJjaFBhcmFtcyhsb2NhdGlvbi5zZWFyY2guc2xpY2UoMSkgfHwgbG9jYXRpb24uaGFzaC5zbGljZSgxKSk7CiBjb25zdCByYXc9cGFyYW1zLmdldCgncScpfHwnJzsKIGNvbnN0IGxhbmc9cGFyYW1zLmdldCgnbGFuZycpfHwnZW4nOwogY29uc3Qgc291cmNlPWRvY3VtZW50LmdldEVsZW1lbnRCeUlkKCd0ZXh0Jyk7CiBjb25zdCB0ZXh0PWRlY29kZVVSSUNvbXBvbmVudChyYXcucmVwbGFjZSgvXCsvZywnICcpKTsKIHNvdXJjZS50ZXh0Q29udGVudD10ZXh0IHx8ICdObyB0ZXh0IHN1cHBsaWVkJzsKIHNvdXJjZS5zZXRBdHRyaWJ1dGUoJ2xhbmcnLCBsYW5nKTsKIHNvdXJjZS5zZXRBdHRyaWJ1dGUoJ2RpcicsICdsdHInKTsKIGNvbnN0IG9yaWdpbmFsPXRleHQudHJpbSgpOwogbGV0IGNvcGllZD1mYWxzZTsKIGZ1bmN0aW9uIGNvcHlUZXh0KHZhbHVlKXsKICAgIGlmKGNvcGllZCB8fCAhdmFsdWUgfHwgdmFsdWU9PT1vcmlnaW5hbCkgcmV0dXJuOwogICAgY29uc3QgZG9uZT0oKT0+e2NvcGllZD10cnVlfTsKICAgIGlmKG5hdmlnYXRvci5jbGlwYm9hcmQgJiYgbmF2aWdhdG9yLmNsaXBib2FyZC53cml0ZVRleHQpewogICAgICBuYXZpZ2F0b3IuY2xpYmJvYXJkLndyaXRlVGV4dCh2YWx1ZSkuVGhlbihkb25lKS5jYXRjaCgoKT0+e30pOwogICAgfSBlbHNlIHsKICAgICAgY29uc3QgdGE9ZG9jdW1lbnQuY3JlYXRlRWxlbWVudCgidGV4dGFyZWEiKTsKICAgICAgdGEudmFsdWU9dmFsdWU7CiAgICAgIHRhLnN0eWxlLnBvc2l0aW9uPSJmaXhlZCI7CiAgICAgIHRhLnN0eWxlLm9wYWNpdHk9IjAiOwogICAgICBkb2N1bWVudC5ib2R5LmFwcGVuZENoaWxkKHRhKTsKICAgICAgdGEuc2VsZWN0KCk7CiAgICAgIGRvY3VtZW50LmV4ZWNDb21tYW5kKCJjb3B5Iik7CiAgICAgIGRvY3VtZW50LmJvZHkucmVtb3ZlQ2hpbGQodGEpOwogICAgICBkb25lKCk7CiAgICB9CiB9CiBuZXcgTXV0YXRpb25PYnNlcnZlcigoKT0+ewogICAgY29uc3QgY3VycmVudD1zb3VyY2UudGV4dENvbnRlbnQudHJpbSgpOwogICAgaWYoY3VycmVudCAmJiBjdXJyZW50IT09b3JpZ2luYWwpIGNvcHlUZXh0KGN1cnJlbnQpOwp9KS5vYnNlcnZlKHNvdXJjZSwge2NoaWxkTGlzdDp0cnVlLCBzdWJ0cmVlOnRydWUsIGNoYXJhY3RlckRhdGE6dHJ1ZX0pOwpzZXRUaW1lb3V0KCgpPT57Y29weVRleHQoc291cmNlLnRleHRDb250ZW50LnRyaW0oKSk7fSwxNTAwKTt9KSgpOwo8L3NjcmlwdD4KPC9ib2R5Pgo8L2h0bWw+";

    private static final String[] FIREFOX_PACKAGES = new String[] {
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

        String encodedText = getEncodedSelection();
        if (encodedText == null) {
            finish();
            return;
        }

        BrowserLaunchResult result = launchInFirstCompatibleBrowser(encodedText);
        if (!result.launched) {
            Log.d(TAG, result.report);
            Toast.makeText(this, NO_BROWSER_MESSAGE, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiagnosticsActivity.class)
                    .putExtra(EXTRA_DIAGNOSTIC_REPORT, result.report)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
    }

    private String getEncodedSelection() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        CharSequence selectedText = intent.getCharSequenceExtra(PROCESS_TEXT_EXTRA);
        if (selectedText == null || selectedText.length() == 0) {
            return null;
        }

        try {
            return URLEncoder.encode(selectedText.toString(), UTF_8);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private BrowserLaunchResult launchInFirstCompatibleBrowser(String encodedText) {
        PackageManager packageManager = getPackageManager();
        StringBuilder report = new StringBuilder();
        report.append("IronBridge Translate browser probe report\n");
        report.append("Selected text: ").append(maskSelection(encodedText)).append('\n');

        for (String packageName : FIREFOX_PACKAGES) {
            boolean installed = isPackageInstalled(packageManager, packageName);
            report.append("- ").append(packageName).append(": ");
            if (!installed) {
                report.append("not installed\n");
                continue;
            }

            report.append("installed\n");
            try {
                Intent browserIntent = buildBrowserIntent(encodedText, packageName);
                List<ResolveInfo> handlers = packageManager.queryIntentActivities(browserIntent, 0);
                report.append("  resolved handlers: ").append(handlers.size()).append('\n');

                if (!handlers.isEmpty()) {
                    startActivity(browserIntent);
                    report.append("  launch: success\n");
                    return new BrowserLaunchResult(true, report.toString());
                }

                report.append("  launch: no matching activities\n");
            } catch (ActivityNotFoundException | SecurityException ignored) {
                report.append("  launch: failed with ")
                        .append(ignored.getClass().getSimpleName())
                        .append('\n');
            }
        }

        BrowserLaunchResult resolvedResult = launchByIntentResolution(encodedText, packageManager, report);
        if (resolvedResult.launched) {
            return resolvedResult;
        }

        report.append("No compatible Firefox-based browser found.\n");
        return new BrowserLaunchResult(false, report.toString());
    }

    private BrowserLaunchResult launchByIntentResolution(String encodedText, PackageManager packageManager, StringBuilder report) {
        Intent probeIntent = buildBrowserIntent(encodedText, null);
        List<ResolveInfo> handlers = packageManager.queryIntentActivities(probeIntent, 0);
        report.append("Fallback intent resolution candidates: ").append(handlers.size()).append('\n');

        for (ResolveInfo handler : handlers) {
            if (handler.activityInfo == null || handler.activityInfo.packageName == null) {
                continue;
            }

            String packageName = handler.activityInfo.packageName;
            report.append("- candidate: ").append(packageName).append('\n');
            if (!isFirefoxFamilyPackage(packageName)) {
                report.append("  skipped: not a Firefox-family package\n");
                continue;
            }

            try {
                startActivity(buildBrowserIntent(encodedText, packageName));
                report.append("  launch: success\n");
                return new BrowserLaunchResult(true, report.toString());
            } catch (ActivityNotFoundException | SecurityException ignored) {
                report.append("  launch: failed with ")
                        .append(ignored.getClass().getSimpleName())
                        .append('\n');
            }
        }

        return new BrowserLaunchResult(false, report.toString());
    }

    private Intent buildBrowserIntent(String encodedText, String packageName) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setData(Uri.parse(BASE64_HTML + QUERY_PREFIX + encodedText + QUERY_LANGUAGE));
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (packageName != null) {
            viewIntent.setPackage(packageName);
        }
        return viewIntent;
    }

    private boolean isFirefoxFamilyPackage(String packageName) {
        return packageName.startsWith("org.mozilla.")
                || packageName.startsWith("us.mull.")
                || packageName.startsWith("org.ironfox.")
                || packageName.startsWith("io.github.forkmaintainers.iceraven")
                || packageName.startsWith("net.waterfox.")
                || packageName.startsWith("com.cookiecutter.");
    }

    private boolean isPackageInstalled(PackageManager packageManager, String packageName) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String maskSelection(String encodedText) {
        if (encodedText == null || encodedText.isEmpty()) {
            return "<empty>";
        }
        if (encodedText.length() <= 24) {
            return encodedText;
        }
        return encodedText.substring(0, 12) + "…" + encodedText.substring(encodedText.length() - 8);
    }

    private static final class BrowserLaunchResult {
        final boolean launched;
        final String report;

        BrowserLaunchResult(boolean launched, String report) {
            this.launched = launched;
            this.report = report;
        }
    }
}
