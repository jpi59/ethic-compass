/*
 * Copyright (C) 2026 jpi59
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.jpi59.ethiccompass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;
import android.view.WindowInsets;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Displays a magnetic heading only. It never requests location or network access. */
public final class MainActivity extends Activity implements SensorEventListener {
    private static final String PREFERENCES = "settings";
    private static final String HAPTICS_ENABLED = "haptics_enabled";
    private static final String DARK_MODE = "dark_mode";
    private static final String DISPLAY_LANGUAGE = "display_language";
    private static final String SYSTEM_LANGUAGE = "system";

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private TextView headingText;
    private TextView cardinalText;
    private TextView statusText;
    private CompassView compassView;
    private boolean hapticsEnabled;
    private boolean darkMode;
    private String displayLanguage;
    private String lastAnnouncedCardinal;
    private String lastHapticCardinal;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(localizedContext(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        hapticsEnabled = preferences.getBoolean(HAPTICS_ENABLED, false);
        darkMode = preferences.getBoolean(DARK_MODE, false);
        displayLanguage = preferences.getString(DISPLAY_LANGUAGE, SYSTEM_LANGUAGE);
        setTheme(darkMode ? R.style.Theme_EthicCompass_Dark : R.style.Theme_EthicCompass);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        setContentView(createContent());
        applySystemBars();

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            showSensorUnavailable();
        }
    }

    private View createContent() {
        if (getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            return createLandscapeContent();
        }
        return createPortraitContent();
    }

    private View createPortraitContent() {
        int padding = dp(6);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(R.color.background, R.color.background_dark));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(padding, padding + bars.top, padding, padding + bars.bottom);
                return insets;
            });
        } else {
            scrollView.setFitsSystemWindows(true);
        }

        TextView title = textView(30, true);
        title.setText(R.string.app_name);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(color(R.color.on_background, R.color.on_background_dark));
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // The live reading is drawn in the dial. These views retain a concise
        // accessibility description without duplicating the visual reading.
        headingText = textView(1, false);
        cardinalText = textView(1, false);
        cardinalText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        compassView = new CompassView(this);
        compassView.setDarkMode(darkMode);
        compassView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        root.addView(compassView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(370)));

        statusText = textView(18, false);
        statusText.setTextColor(color(R.color.muted, R.color.muted_dark));
        statusText.setGravity(Gravity.CENTER);
        statusText.setText(getString(R.string.status_summary));
        statusText.setPadding(0, dp(4), 0, 0);
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(createHapticsControl(17), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(createDarkModeControl(17), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(createLanguageControl(17), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView details = textView(16, true);
        medium(details);
        details.setTextColor(color(R.color.primary, R.color.primary_dark));
        details.setGravity(Gravity.CENTER);
        details.setMinHeight(dp(48));
        details.setPadding(dp(12), dp(8), dp(12), dp(8));
        details.setText(getString(R.string.details_label));
        details.setClickable(true);
        details.setFocusable(true);
        details.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.details_label)
                .setMessage(R.string.details_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
        root.addView(details, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private View createLandscapeContent() {
        int padding = dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(color(R.color.background, R.color.background_dark));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(padding + bars.left, padding + bars.top,
                        padding + bars.right, padding + bars.bottom);
                return insets;
            });
        } else {
            root.setFitsSystemWindows(true);
        }

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(controls, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 0.42f));

        TextView title = textView(24, true);
        title.setText(R.string.app_name);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(color(R.color.on_background, R.color.on_background_dark));
        controls.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        headingText = textView(1, false);
        cardinalText = textView(1, false);
        cardinalText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        statusText = textView(16, false);
        statusText.setTextColor(color(R.color.muted, R.color.muted_dark));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(16), 0, 0);
        statusText.setText(getString(R.string.status_summary));
        controls.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        controls.addView(createHapticsControl(16), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        controls.addView(createDarkModeControl(16), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        controls.addView(createLanguageControl(16), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView details = textView(16, true);
        medium(details);
        details.setTextColor(color(R.color.primary, R.color.primary_dark));
        details.setGravity(Gravity.CENTER);
        details.setMinHeight(dp(48));
        details.setPadding(dp(12), dp(8), dp(12), dp(8));
        details.setText(getString(R.string.details_label));
        details.setClickable(true);
        details.setFocusable(true);
        details.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.details_label)
                .setMessage(R.string.details_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
        controls.addView(details, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        compassView = new CompassView(this);
        compassView.setDarkMode(darkMode);
        compassView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        root.addView(compassView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 0.58f));
        return root;
    }

    private TextView textView(int sizeSp, boolean important) {
        TextView view = new TextView(this);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setImportantForAccessibility(important
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        return view;
    }

    private void medium(TextView view) {
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private View createHapticsControl(int textSizeSp) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, 0);

        CheckBox haptics = new CheckBox(this);
        haptics.setText(getString(R.string.haptics_label));
        haptics.setTextColor(color(R.color.on_background, R.color.on_background_dark));
        haptics.setTextSize(textSizeSp);
        haptics.setGravity(Gravity.CENTER_VERTICAL);
        haptics.setButtonTintList(null);
        haptics.setButtonDrawable(darkMode
                ? R.drawable.haptic_checkbox_dark : R.drawable.haptic_checkbox);
        haptics.setChecked(hapticsEnabled);
        haptics.setContentDescription(getString(R.string.haptics_description));
        haptics.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                showHapticsDialog(haptics);
            } else {
                setHapticsEnabled(false);
            }
        });
        row.addView(haptics, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView info = textView(24, true);
        medium(info);
        info.setText("ⓘ");
        info.setTextColor(color(R.color.primary, R.color.primary_dark));
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(8), 0, 0, 0);
        info.setClickable(true);
        info.setFocusable(true);
        info.setContentDescription(getString(R.string.haptics_info_description));
        info.setOnClickListener(view -> showHapticsDialog(null));
        row.addView(info, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return row;
    }

    private View createDarkModeControl(int textSizeSp) {
        CheckBox darkModeControl = new CheckBox(this);
        darkModeControl.setText(getString(R.string.dark_mode_label));
        darkModeControl.setTextColor(color(R.color.on_background, R.color.on_background_dark));
        darkModeControl.setTextSize(textSizeSp);
        darkModeControl.setGravity(Gravity.CENTER_VERTICAL);
        darkModeControl.setPadding(0, dp(2), 0, 0);
        darkModeControl.setButtonTintList(null);
        darkModeControl.setButtonDrawable(darkMode
                ? R.drawable.haptic_checkbox_dark : R.drawable.haptic_checkbox);
        darkModeControl.setChecked(darkMode);
        darkModeControl.setContentDescription(getString(R.string.dark_mode_description));
        darkModeControl.setOnCheckedChangeListener((button, checked) -> setDarkMode(checked));
        return darkModeControl;
    }

    private View createLanguageControl(int textSizeSp) {
        TextView language = textView(textSizeSp, true);
        medium(language);
        language.setText(getString(R.string.language_value_format,
                getString(languageNameResource())));
        language.setTextColor(color(R.color.primary, R.color.primary_dark));
        language.setGravity(Gravity.CENTER);
        language.setMinHeight(dp(48));
        language.setPadding(dp(12), dp(8), dp(12), dp(8));
        language.setClickable(true);
        language.setFocusable(true);
        language.setContentDescription(getString(R.string.language_description));
        language.setOnClickListener(view -> showLanguageDialog());
        return language;
    }

    private void showLanguageDialog() {
        CharSequence[] languages = {
                getString(R.string.language_spanish), getString(R.string.language_english),
                getString(R.string.language_system)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(languages, languageSelectionIndex(),
                        (dialog, which) -> {
                            setDisplayLanguage(which == 0 ? "es" : which == 1 ? "en" : SYSTEM_LANGUAGE);
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showHapticsDialog(CheckBox haptics) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.haptics_dialog_title)
                .setMessage(R.string.haptics_dialog_message)
                .setNegativeButton(android.R.string.cancel, (ignored, which) -> {
                    if (haptics != null) {
                        haptics.setChecked(false);
                    }
                });
        if (haptics != null) {
            dialog.setPositiveButton(R.string.haptics_dialog_enable, (ignored, which) ->
                    setHapticsEnabled(true));
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }
        dialog.show();
    }

    private void setHapticsEnabled(boolean enabled) {
        hapticsEnabled = enabled;
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(HAPTICS_ENABLED, enabled).apply();
        lastHapticCardinal = null;
    }

    private void setDarkMode(boolean enabled) {
        if (darkMode == enabled) {
            return;
        }
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(DARK_MODE, enabled).apply();
        recreate();
    }

    private void setDisplayLanguage(String language) {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        if (language.equals(preferences.getString(DISPLAY_LANGUAGE, null))) {
            return;
        }
        preferences.edit().putString(DISPLAY_LANGUAGE, language).apply();
        recreate();
    }

    private static Context localizedContext(Context base) {
        String language = base.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(DISPLAY_LANGUAGE, null);
        if (!"es".equals(language) && !"en".equals(language)) {
            return base;
        }
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        return base.createConfigurationContext(configuration);
    }

    private int languageNameResource() {
        if ("es".equals(displayLanguage)) {
            return R.string.language_spanish;
        }
        if ("en".equals(displayLanguage)) {
            return R.string.language_english;
        }
        return R.string.language_system;
    }

    private int languageSelectionIndex() {
        if ("es".equals(displayLanguage)) {
            return 0;
        }
        if ("en".equals(displayLanguage)) {
            return 1;
        }
        return 2;
    }

    private int color(int lightColor, int darkColor) {
        return getColor(darkMode ? darkColor : lightColor);
    }

    private void applySystemBars() {
        Window window = getWindow();
        int background = color(R.color.background, R.color.background_dark);
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotation = new float[9];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            publishHeading(headingFromRotationMatrix(rotation));
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }
        if (gravity != null && geomagnetic != null) {
            float[] rotation = new float[9];
            if (SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) {
                publishHeading(headingFromRotationMatrix(rotation));
            }
        }
    }

    private float headingFromRotationMatrix(float[] rotation) {
        float[] adjusted = new float[9];
        int rotationConstant = getDisplayRotation();
        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Y;
        if (rotationConstant == Surface.ROTATION_90) {
            axisX = SensorManager.AXIS_Y;
            axisY = SensorManager.AXIS_MINUS_X;
        } else if (rotationConstant == Surface.ROTATION_180) {
            axisX = SensorManager.AXIS_MINUS_X;
            axisY = SensorManager.AXIS_MINUS_Y;
        } else if (rotationConstant == Surface.ROTATION_270) {
            axisX = SensorManager.AXIS_MINUS_Y;
            axisY = SensorManager.AXIS_X;
        }
        SensorManager.remapCoordinateSystem(rotation, axisX, axisY, adjusted);
        // This is the azimuth component of the documented orientation matrix.
        // Calculating it directly avoids the deprecated getOrientation() helper.
        return (float) ((Math.toDegrees(Math.atan2(adjusted[1], adjusted[4])) + 360.0) % 360.0);
    }

    private int getDisplayRotation() {
        DisplayManager displayManager = getSystemService(DisplayManager.class);
        Display display = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        return display == null ? Surface.ROTATION_0 : display.getRotation();
    }

    private void publishHeading(float heading) {
        int degrees = Math.round(heading) % 360;
        String cardinal = cardinalFor(degrees);
        String text = getString(R.string.heading_format, degrees, cardinal);
        headingText.setText(text);
        headingText.setContentDescription(text);
        compassView.setHeading(degrees, cardinal);
        compassView.setContentDescription(text);
        if (!cardinal.equals(lastAnnouncedCardinal)) {
            cardinalText.setText(cardinal);
            lastAnnouncedCardinal = cardinal;
        }
        if (hapticsEnabled && !cardinal.equals(lastHapticCardinal)) {
            compassView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            lastHapticCardinal = cardinal;
        }
    }

    private String cardinalFor(int degrees) {
        int index = ((degrees + 22) % 360) / 45;
        int[] names = {R.string.cardinal_n, R.string.cardinal_ne, R.string.cardinal_e,
                R.string.cardinal_se, R.string.cardinal_s, R.string.cardinal_sw,
                R.string.cardinal_w, R.string.cardinal_nw};
        return getString(names[index]);
    }

    private void showSensorUnavailable() {
        headingText.setText(getString(R.string.heading_unavailable));
        cardinalText.setText("");
        statusText.setText(getString(R.string.sensor_unavailable));
        compassView.setVisibility(View.INVISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Sensor providers report accuracy asynchronously; readings remain available.
    }
}
