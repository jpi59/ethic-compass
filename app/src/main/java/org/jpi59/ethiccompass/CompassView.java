/*
 * Copyright (C) 2026 jpi59
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.jpi59.ethiccompass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A high-contrast, drawn-from-primitives compass. No images or external assets are used. */
public final class CompassView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float heading;
    private String cardinal = "";

    public CompassView(Context context) {
        super(context);
    }

    public void setHeading(float value, String cardinalName) {
        heading = value;
        cardinal = cardinalName;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.36f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getContext().getColor(R.color.surface));
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, radius * 0.025f));
        paint.setColor(getContext().getColor(R.color.on_background));
        canvas.drawCircle(centerX, centerY, radius, paint);

        canvas.save();
        canvas.rotate(-heading, centerX, centerY);
        for (int degree = 0; degree < 360; degree += 15) {
            boolean cardinalTick = degree % 90 == 0;
            float tickLength = cardinalTick ? radius * 0.16f : radius * 0.08f;
            paint.setStrokeWidth(cardinalTick ? radius * 0.032f : radius * 0.016f);
            paint.setColor(getContext().getColor(cardinalTick ? R.color.primary : R.color.muted));
            canvas.drawLine(centerX, centerY - radius, centerX, centerY - radius + tickLength, paint);
            canvas.rotate(15, centerX, centerY);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(radius * 0.20f);
        paint.setColor(getContext().getColor(R.color.north));
        canvas.drawText("N", centerX, centerY - radius * 0.70f, paint);
        paint.setColor(getContext().getColor(R.color.on_background));
        canvas.drawText("E", centerX + radius * 0.70f, centerY + radius * 0.07f, paint);
        canvas.drawText("S", centerX, centerY + radius * 0.78f, paint);
        canvas.drawText("O", centerX - radius * 0.70f, centerY + radius * 0.07f, paint);
        canvas.restore();

        paint.setColor(getContext().getColor(R.color.primary));
        canvas.drawCircle(centerX, centerY, radius * 0.06f, paint);
        paint.setColor(getContext().getColor(R.color.north));
        android.graphics.Path needle = new android.graphics.Path();
        needle.moveTo(centerX, centerY - radius * 0.85f);
        needle.lineTo(centerX - radius * 0.07f, centerY + radius * 0.13f);
        needle.lineTo(centerX + radius * 0.07f, centerY + radius * 0.13f);
        needle.close();
        canvas.drawPath(needle, paint);
    }
}
