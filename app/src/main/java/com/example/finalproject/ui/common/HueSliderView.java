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

public class HueSliderView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private float hue = 28f;
    private OnHueChangeListener listener;

    public HueSliderView(Context context) {
        this(context, null);
    }

    public HueSliderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HueSliderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        thumbOuterPaint.setStyle(Paint.Style.FILL);
        thumbInnerPaint.setStyle(Paint.Style.STROKE);
        thumbInnerPaint.setStrokeWidth(UiUtils.dp(context, 1));
    }

    public void setOnHueChangeListener(@Nullable OnHueChangeListener listener) {
        this.listener = listener;
    }

    public void setHue(float hue) {
        this.hue = normalizeHue(hue);
        invalidate();
    }

    public float getHue() {
        return hue;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int inset = UiUtils.dp(getContext(), 4);
        float top = (h - UiUtils.dp(getContext(), 16)) / 2f;
        float bottom = top + UiUtils.dp(getContext(), 16);
        trackRect.set(inset, top, Math.max(inset, w - inset), bottom);
        trackPaint.setShader(new LinearGradient(
                trackRect.left,
                trackRect.top,
                trackRect.right,
                trackRect.top,
                new int[]{
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.CYAN,
                        Color.BLUE,
                        Color.MAGENTA,
                        Color.RED
                },
                null,
                Shader.TileMode.CLAMP
        ));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = trackRect.height() / 2f;
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
        drawThumb(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateHue(event.getX(), true);
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

    private void updateHue(float x, boolean notifyListener) {
        if (trackRect.width() <= 0f) {
            return;
        }
        float clampedX = clamp(x, trackRect.left, trackRect.right);
        float progress = (clampedX - trackRect.left) / trackRect.width();
        hue = 360f * progress;
        invalidate();
        if (notifyListener && listener != null) {
            listener.onHueChanged(normalizeHue(hue));
        }
    }

    private void drawThumb(Canvas canvas) {
        float progress = hue / 360f;
        float centerX = trackRect.left + (trackRect.width() * progress);
        float centerY = trackRect.centerY();
        int thumbColor = Color.HSVToColor(new float[]{normalizeHue(hue), 1f, 1f});
        thumbOuterPaint.setColor(thumbColor);
        thumbInnerPaint.setColor(Color.WHITE);
        float outerRadius = UiUtils.dp(getContext(), 10);
        canvas.drawCircle(centerX, centerY, outerRadius, thumbOuterPaint);
        canvas.drawCircle(centerX, centerY, outerRadius - UiUtils.dp(getContext(), 1), thumbInnerPaint);
    }

    private float normalizeHue(float hue) {
        float normalized = hue % 360f;
        return normalized < 0f ? normalized + 360f : normalized;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface OnHueChangeListener {
        void onHueChanged(float hue);
    }
}
