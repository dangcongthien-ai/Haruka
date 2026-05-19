package com.example.finalproject.ui.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.finalproject.R;

public class MonthGridView extends View {
    private static final int COLUMN_COUNT = 7;
    private static final int ROW_COUNT = 6;

    private final Paint paint = new Paint();
    private final float[] columnEdges = new float[COLUMN_COUNT + 1];
    private int bottomInset;
    private int scrollOffset;
    private float gridTop;
    private float rowHeight;
    private boolean hasMeasuredGrid;

    public MonthGridView(Context context) {
        super(context);
        init();
    }

    public MonthGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MonthGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setBottomInset(int bottomInset) {
        int normalizedInset = Math.max(0, bottomInset);
        if (this.bottomInset != normalizedInset) {
            this.bottomInset = normalizedInset;
            invalidate();
        }
    }

    public void setScrollOffset(int scrollOffset) {
        int normalizedOffset = Math.max(0, scrollOffset);
        if (this.scrollOffset != normalizedOffset) {
            this.scrollOffset = normalizedOffset;
            invalidate();
        }
    }

    public void setGridMetrics(float[] newColumnEdges, float newGridTop, float newRowHeight) {
        if (newColumnEdges == null || newColumnEdges.length != columnEdges.length || newRowHeight <= 0) {
            return;
        }
        boolean changed = !hasMeasuredGrid
                || Float.compare(gridTop, newGridTop) != 0
                || Float.compare(rowHeight, newRowHeight) != 0;
        for (int i = 0; i < columnEdges.length; i++) {
            if (Float.compare(columnEdges[i], newColumnEdges[i]) != 0) {
                changed = true;
                columnEdges[i] = newColumnEdges[i];
            }
        }
        gridTop = newGridTop;
        rowHeight = newRowHeight;
        hasMeasuredGrid = true;
        if (changed) {
            invalidate();
        }
    }

    private void init() {
        paint.setAntiAlias(false);
        paint.setColor(getContext().getColor(R.color.line));
        paint.setStrokeWidth(1f);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 0.5f;
        float top = 0.5f - scrollOffset;
        float right = getWidth() - 0.5f;
        float bottom = getHeight() - bottomInset - 0.5f - scrollOffset;
        if (hasMeasuredGrid) {
            left = columnEdges[0];
            top = gridTop;
            right = columnEdges[COLUMN_COUNT];
            bottom = gridTop + rowHeight * ROW_COUNT;
        }
        if (right <= left || bottom <= top) {
            return;
        }

        for (int column = 0; column <= COLUMN_COUNT; column++) {
            float x = hasMeasuredGrid
                    ? crisp(columnEdges[column], left, right)
                    : crisp(left + (right - left) * column / COLUMN_COUNT, left, right);
            canvas.drawLine(x, top, x, bottom, paint);
        }
        for (int row = 0; row <= ROW_COUNT; row++) {
            float y = hasMeasuredGrid
                    ? crisp(gridTop + rowHeight * row, top, bottom)
                    : crisp(top + (bottom - top) * row / ROW_COUNT, top, bottom);
            canvas.drawLine(left, y, right, y, paint);
        }
    }

    private float crisp(float value, float min, float max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return (float) Math.floor(value) + 0.5f;
    }
}
