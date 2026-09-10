/*
 * Copyright (C) 2026 jpi59
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.jpi59.ethiccompass;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Displays a magnetic heading only. It never requests location or network access. */
public final class MainActivity extends Activity implements SensorEventListener {
    private static final String PREFERENCES = "settings";
    private static final String HAPTICS_ENABLED = "haptics_enabled";

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
    private String lastAnnouncedCardinal;
    private String lastHapticCardinal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hapticsEnabled = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getBoolean(HAPTICS_ENABLED, false);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        setContentView(createContent());

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            showSensorUnavailable();
        }
    }

    private View createContent() {
        int padding = dp(20);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.background));
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

        headingText = textView(32, true);
        headingText.setGravity(Gravity.CENTER);
        headingText.setTextColor(getColor(R.color.on_background));
        root.addView(headingText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        cardinalText = textView(20, true);
        cardinalText.setGravity(Gravity.CENTER);
        cardinalText.setTextColor(getColor(R.color.primary));
        cardinalText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(cardinalText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        compassView = new CompassView(this);
        compassView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(compassView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(260)));

        statusText = textView(18, false);
        statusText.setTextColor(getColor(R.color.muted));
        statusText.setGravity(Gravity.CENTER);
        statusText.setText(getString(R.string.instruction));
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        CheckBox haptics = new CheckBox(this);
        haptics.setText(getString(R.string.haptics_label));
        haptics.setTextColor(getColor(R.color.on_background));
        haptics.setTextSize(18);
        haptics.setPadding(0, dp(12), 0, 0);
        haptics.setChecked(hapticsEnabled);
        haptics.setContentDescription(getString(R.string.haptics_description));
        haptics.setOnCheckedChangeListener((button, checked) -> {
            hapticsEnabled = checked;
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                    .putBoolean(HAPTICS_ENABLED, checked).apply();
            lastHapticCardinal = null;
        });
        root.addView(haptics, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView privacy = textView(15, false);
        privacy.setTextColor(getColor(R.color.muted));
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(8), 0, 0);
        privacy.setText(getString(R.string.privacy_summary));
        root.addView(privacy, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private TextView textView(int sizeSp, boolean important) {
        TextView view = new TextView(this);
        view.setTextSize(sizeSp);
        view.setImportantForAccessibility(important
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        return view;
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
