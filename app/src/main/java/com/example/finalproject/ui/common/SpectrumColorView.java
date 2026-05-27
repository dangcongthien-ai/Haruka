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

public class SpectrumColorView extends View {
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF contentRect = new RectF();

    private float hue = 28f;
    private float saturation = 0.68f;
    private float value = 0.92f;
    private float selectorX;
    private float selectorY;
    private int selectedColor = Color.parseColor("#EB9B54");
    private OnColorSelectedListener listener;

    public SpectrumColorView(Context context) {
        this(context, null);
    }

    public SpectrumColorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpectrumColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        selectorOuterPaint.setStyle(Paint.Style.STROKE);
        selectorOuterPaint.setStrokeWidth(UiUtils.dp(context, 2));
        selectorInnerPaint.setStyle(Paint.Style.STROKE);
        selectorInnerPaint.setStrokeWidth(UiUtils.dp(context, 1));
    }

    public void setOnColorSelectedListener(@Nullable OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public void setHue(float hue) {
        this.hue = normalizeHue(hue);
        updateShaders();
        updateColorFromSelection(false);
        invalidate();
    }

    public void setSelectedColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = normalizeHue(hsv[0]);
        saturation = clamp(hsv[1], 0f, 1f);
        value = clamp(hsv[2], 0f, 1f);
        updateShaders();
        selectedColor = Color.HSVToColor(new float[]{hue, saturation, value});
        syncSelectorToColor();
        invalidate();
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int inset = UiUtils.dp(getContext(), 4);
        contentRect.set(inset, inset, Math.max(inset, w - inset), Math.max(inset, h - inset));
        updateShaders();
        syncSelectorToColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (contentRect.width() <= 0f || contentRect.height() <= 0f) {
            return;
        }
        float radius = UiUtils.dp(getContext(), 18);
        canvas.drawRoundRect(contentRect, radius, radius, basePaint);
        canvas.drawRoundRect(contentRect, radius, radius, whiteOverlayPaint);
        canvas.drawRoundRect(contentRect, radius, radius, blackOverlayPaint);
        drawSelector(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateSelection(event.getX(), event.getY(), true);
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

    private void updateSelection(float x, float y, boolean notifyListener) {
        if (contentRect.width() <= 0f || contentRect.height() <= 0f) {
            return;
        }
        selectorX = clamp(x, contentRect.left, contentRect.right);
        selectorY = clamp(y, contentRect.top, contentRect.bottom);
        saturation = contentRect.width() == 0f ? 0f : (selectorX - contentRect.left) / contentRect.width();
        value = contentRect.height() == 0f ? 0f : 1f - ((selectorY - contentRect.top) / contentRect.height());
        updateColorFromSelection(notifyListener);
        invalidate();
    }

    private void updateColorFromSelection(boolean notifyListener) {
        selectedColor = Color.HSVToColor(new float[]{hue, saturation, value});
        syncSelectorToColor();
        if (notifyListener && listener != null) {
            listener.onColorSelected(selectedColor);
        }
    }

    private void syncSelectorToColor() {
        if (contentRect.width() <= 0f || contentRect.height() <= 0f) {
            return;
        }
        selectorX = contentRect.left + (saturation * contentRect.width());
        selectorY = contentRect.top + ((1f - value) * contentRect.height());
    }

    private void updateShaders() {
        basePaint.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
        whiteOverlayPaint.setShader(new LinearGradient(
                contentRect.left,
                contentRect.top,
                contentRect.right,
                contentRect.top,
                Color.WHITE,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        blackOverlayPaint.setShader(new LinearGradient(
                contentRect.left,
                contentRect.top,
                contentRect.left,
                contentRect.bottom,
                Color.TRANSPARENT,
                Color.BLACK,
                Shader.TileMode.CLAMP
        ));
    }

    private void drawSelector(Canvas canvas) {
        int outerColor = UiUtils.readableTextColor(selectedColor, getContext());
        int innerColor = outerColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        selectorOuterPaint.setColor(outerColor);
        selectorInnerPaint.setColor(innerColor);
        float radius = UiUtils.dp(getContext(), 10);
        canvas.drawCircle(selectorX, selectorY, radius + UiUtils.dp(getContext(), 2), selectorInnerPaint);
        canvas.drawCircle(selectorX, selectorY, radius, selectorOuterPaint);
    }

    private float normalizeHue(float hue) {
        float normalized = hue % 360f;
        return normalized < 0f ? normalized + 360f : normalized;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }
}
