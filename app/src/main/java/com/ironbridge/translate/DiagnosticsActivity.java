package com.ironbridge.translate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public final class DiagnosticsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String report = getIntent() != null
                ? getIntent().getStringExtra(TranslateActivity.EXTRA_DIAGNOSTIC_REPORT)
                : null;

        setTitle("IronBridge Diagnostics");
        setContentView(buildContent(report));
    }

    private ScrollView buildContent(String report) {
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics());

        textView.setPadding(padding, padding, padding, padding);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextIsSelectable(true);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (report == null || report.trim().isEmpty()) {
            report = "No diagnostic data available.";
        }

        textView.setText(report);
        scrollView.addView(textView);
        return scrollView;
    }
}
