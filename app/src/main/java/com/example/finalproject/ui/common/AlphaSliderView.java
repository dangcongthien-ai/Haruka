package com.example.finalproject.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.finalproject.R;

public class AlphaSliderView extends View {
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private int baseColor = Color.parseColor("#EB9B54");
    private int alphaValue = 255;
    private OnAlphaChangeListener listener;

    public AlphaSliderView(Context context) {
        this(context, null);
    }

    public AlphaSliderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphaSliderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        thumbOuterPaint.setStyle(Paint.Style.FILL);
        thumbInnerPaint.setStyle(Paint.Style.STROKE);
        thumbInnerPaint.setStrokeWidth(UiUtils.dp(context, 1));
    }

    public void setOnAlphaChangeListener(@Nullable OnAlphaChangeListener listener) {
        this.listener = listener;
    }

    public void setBaseColor(int baseColor) {
        this.baseColor = Color.rgb(Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        updateGradient();
        invalidate();
    }

    public void setAlphaValue(int alphaValue) {
        this.alphaValue = clamp(alphaValue, 0, 255);
        invalidate();
    }

    public int getAlphaValue() {
        return alphaValue;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int inset = UiUtils.dp(getContext(), 4);
        float top = (h - UiUtils.dp(getContext(), 16)) / 2f;
        float bottom = top + UiUtils.dp(getContext(), 16);
        trackRect.set(inset, top, Math.max(inset, w - inset), bottom);
        updateGradient();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCheckerboard(canvas);
        float radius = trackRect.height() / 2f;
        canvas.drawRoundRect(trackRect, radius, radius, gradientPaint);
        drawThumb(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateAlpha(event.getX(), true);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void updateGradient() {
        gradientPaint.setShader(new LinearGradient(
                trackRect.left,
                trackRect.top,
                trackRect.right,
                trackRect.top,
                Color.argb(0, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Color.argb(255, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Shader.TileMode.CLAMP
        ));
    }

    private void drawCheckerboard(Canvas canvas) {
        int cell = UiUtils.dp(getContext(), 6);
        int light = getContext().getColor(android.R.color.white);
        int dark = getContext().getColor(R.color.line);
        float radius = trackRect.height() / 2f;
        canvas.save();
        canvas.clipRect(trackRect);
        for (int row = 0; row * cell < trackRect.height(); row++) {
            for (int col = 0; col * cell < trackRect.width(); col++) {
                boolean useLight = (row + col) % 2 == 0;
                checkerPaint.setColor(useLight ? light : dark);
                float left = trackRect.left + (col * cell);
                float top = trackRect.top + (row * cell);
                canvas.drawRect(left, top, left + cell, top + cell, checkerPaint);
            }
        }
        canvas.restore();
        Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        mask.setStyle(Paint.Style.STROKE);
        mask.setStrokeWidth(UiUtils.dp(getContext(), 1));
        mask.setColor(getContext().getColor(R.color.line));
        canvas.drawRoundRect(trackRect, radius, radius, mask);
    }

    private void drawThumb(Canvas canvas) {
        float progress = alphaValue / 255f;
        float centerX = trackRect.left + (trackRect.width() * progress);
        float centerY = trackRect.centerY();
        int thumbColor = Color.argb(alphaValue, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        thumbOuterPaint.setColor(ColorUtilsCompat.compositeOverSurface(thumbColor, getContext().getColor(R.color.surface)));
        thumbInnerPaint.setColor(Color.WHITE);
        float outerRadius = UiUtils.dp(getContext(), 10);
        canvas.drawCircle(centerX, centerY, outerRadius, thumbOuterPaint);
        canvas.drawCircle(centerX, centerY, outerRadius - UiUtils.dp(getContext(), 1), thumbInnerPaint);
    }

    private void updateAlpha(float x, boolean notifyListener) {
        if (trackRect.width() <= 0f) {
            return;
        }
        float clampedX = clamp(x, trackRect.left, trackRect.right);
        float progress = (clampedX - trackRect.left) / trackRect.width();
        alphaValue = clamp(Math.round(progress * 255f), 0, 255);
        invalidate();
        if (notifyListener && listener != null) {
            listener.onAlphaChanged(alphaValue);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface OnAlphaChangeListener {
        void onAlphaChanged(int alphaValue);
    }

    private static final class ColorUtilsCompat {
        private static int compositeOverSurface(int color, int surface) {
            float alpha = Color.alpha(color) / 255f;
            int red = Math.round(Color.red(color) * alpha + Color.red(surface) * (1f - alpha));
            int green = Math.round(Color.green(color) * alpha + Color.green(surface) * (1f - alpha));
            int blue = Math.round(Color.blue(color) * alpha + Color.blue(surface) * (1f - alpha));
            return Color.rgb(red, green, blue);
        }
    }
}
