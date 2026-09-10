/*
 * Copyright (C) 2026 jpi59
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.jpi59.ethiccompass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** A precise, canvas-drawn compass dial. It uses no images or external assets. */
public final class CompassView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pointer = new Path();
    private float heading;
    private String cardinal = "";
    private boolean darkMode;

    public CompassView(Context context) {
        super(context);
    }

    public void setHeading(float value, String cardinalName) {
        heading = value;
        cardinal = cardinalName;
        invalidate();
    }

    public void setDarkMode(boolean enabled) {
        darkMode = enabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float boundary = Math.min(getWidth(), getHeight()) * 0.47f;
        float dialRadius = boundary * 0.88f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.surface, R.color.surface_dark));
        canvas.drawCircle(centerX, centerY, dialRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, boundary * 0.012f));
        paint.setColor(color(R.color.on_background, R.color.on_background_dark));
        canvas.drawCircle(centerX, centerY, dialRadius, paint);
        paint.setStrokeWidth(Math.max(1.5f, boundary * 0.005f));
        paint.setColor(color(R.color.compass_ring, R.color.compass_ring_dark));
        canvas.drawCircle(centerX, centerY, dialRadius * 0.91f, paint);

        drawTicks(canvas, centerX, centerY, dialRadius);
        drawLabels(canvas, centerX, centerY, dialRadius, boundary);
        drawReading(canvas, centerX, centerY, dialRadius);
        drawPointer(canvas, centerX, centerY, dialRadius);
    }

    private void drawTicks(Canvas canvas, float centerX, float centerY, float radius) {
        canvas.save();
        canvas.rotate(-heading, centerX, centerY);
        for (int degree = 0; degree < 360; degree += 5) {
            boolean cardinalTick = degree % 90 == 0;
            boolean majorTick = degree % 30 == 0;
            float length = cardinalTick ? radius * 0.14f
                    : majorTick ? radius * 0.10f : radius * 0.040f;
            paint.setStrokeWidth(cardinalTick ? radius * 0.020f
                    : majorTick ? radius * 0.014f : radius * 0.005f);
            paint.setColor(cardinalTick ? color(R.color.primary, R.color.primary_dark)
                    : color(majorTick ? R.color.on_background : R.color.muted,
                            majorTick ? R.color.on_background_dark : R.color.muted_dark));
            canvas.drawLine(centerX, centerY - radius, centerX, centerY - radius + length, paint);
            canvas.rotate(5, centerX, centerY);
        }
        canvas.restore();
    }

    private void drawLabels(Canvas canvas, float centerX, float centerY, float radius,
            float boundary) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(color(R.color.muted, R.color.muted_dark));
        paint.setTextSize(boundary * 0.062f);
        for (int degree = 0; degree < 360; degree += 30) {
            if (degree % 90 == 0) {
                continue;
            }
            double angle = Math.toRadians(degree - heading - 90f);
            float x = centerX + (float) Math.cos(angle) * radius * 1.10f;
            float y = centerY + (float) Math.sin(angle) * radius * 1.10f
                    - (paint.descent() + paint.ascent()) / 2f;
            canvas.drawText(String.valueOf(degree), x, y, paint);
        }

        String[] directions = {
                getContext().getString(R.string.dial_north),
                getContext().getString(R.string.dial_east),
                getContext().getString(R.string.dial_south),
                getContext().getString(R.string.dial_west)
        };
        paint.setTextSize(boundary * 0.10f);
        for (int index = 0; index < directions.length; index++) {
            double angle = Math.toRadians(index * 90f - heading - 90f);
            float x = centerX + (float) Math.cos(angle) * radius * 1.10f;
            float y = centerY + (float) Math.sin(angle) * radius * 1.10f
                    - (paint.descent() + paint.ascent()) / 2f;
            paint.setColor(index == 0 ? color(R.color.north, R.color.north_dark)
                    : color(R.color.on_background, R.color.on_background_dark));
            canvas.drawText(directions[index], x, y, paint);
        }
    }

    private void drawReading(Canvas canvas, float centerX, float centerY, float radius) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(color(R.color.on_background, R.color.on_background_dark));
        paint.setTextSize(radius * 0.30f);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(Math.round(heading) + "°", centerX,
                centerY + radius * 0.07f - (paint.descent() + paint.ascent()) / 2f, paint);
        paint.setColor(color(R.color.primary, R.color.primary_dark));
        paint.setTextSize(radius * 0.115f);
        canvas.drawText(cardinal, centerX,
                centerY + radius * 0.33f - (paint.descent() + paint.ascent()) / 2f, paint);
    }

    private void drawPointer(Canvas canvas, float centerX, float centerY, float radius) {
        pointer.reset();
        pointer.moveTo(centerX, centerY - radius * 0.84f);
        pointer.lineTo(centerX - radius * 0.062f, centerY - radius * 0.20f);
        pointer.lineTo(centerX + radius * 0.062f, centerY - radius * 0.20f);
        pointer.close();
        paint.setColor(color(R.color.north, R.color.north_dark));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pointer, paint);
    }

    private int color(int lightColor, int darkColor) {
        return getContext().getColor(darkMode ? darkColor : lightColor);
    }
}
