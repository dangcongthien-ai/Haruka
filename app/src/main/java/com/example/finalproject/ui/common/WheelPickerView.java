package com.example.finalproject.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WheelPickerView extends FrameLayout {
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int index, String value);
    }

    private static final int ITEM_HEIGHT_DP = 48;

    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final LinearSnapHelper snapHelper;
    private final WheelAdapter adapter;
    private final View topLine;
    private final View bottomLine;
    private final List<String> items = new ArrayList<>();

    @Nullable
    private OnSelectionChangedListener listener;
    private int selectedIndex;

    public WheelPickerView(@NonNull Context context) {
        this(context, null);
    }

    public WheelPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipChildren(false);
        setClipToPadding(false);

        layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        snapHelper = new LinearSnapHelper();
        adapter = new WheelAdapter();

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setClipToPadding(false);
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateSelectionFromSnap();
                }
            }
        });
        addView(recyclerView);

        topLine = buildLine(context);
        bottomLine = buildLine(context);
        addView(topLine);
        addView(bottomLine);
    }

    public void setItems(@Nullable List<String> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        selectedIndex = clamp(selectedIndex);
        adapter.notifyDataSetChanged();
        post(() -> scrollToSelection(false));
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, false);
    }

    public void setSelectedIndex(int index, boolean smooth) {
        selectedIndex = clamp(index);
        if (items.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }
        adapter.notifyDataSetChanged();
        post(() -> scrollToSelection(smooth));
    }

    public int getSelectedIndex() {
        updateSelectionFromSnap();
        return selectedIndex;
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int itemHeight = dp(ITEM_HEIGHT_DP);
        int verticalPadding = Math.max(0, (h - itemHeight) / 2);
        recyclerView.setPadding(0, verticalPadding, 0, verticalPadding);
        layoutSelectionLines(h, itemHeight);
        post(() -> scrollToSelection(false));
    }

    private void layoutSelectionLines(int height, int itemHeight) {
        int lineHeight = Math.max(1, dp(1));
        int top = Math.max(0, (height - itemHeight) / 2);
        LayoutParams topParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, lineHeight);
        topParams.gravity = Gravity.TOP;
        topParams.topMargin = top;
        topLine.setLayoutParams(topParams);

        LayoutParams bottomParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, lineHeight);
        bottomParams.gravity = Gravity.TOP;
        bottomParams.topMargin = top + itemHeight - lineHeight;
        bottomLine.setLayoutParams(bottomParams);
    }

    private void scrollToSelection(boolean smooth) {
        if (items.isEmpty()) {
            return;
        }
        int target = clamp(selectedIndex);
        if (smooth) {
            recyclerView.smoothScrollToPosition(target);
            recyclerView.postDelayed(this::updateSelectionFromSnap, 180L);
        } else {
            layoutManager.scrollToPositionWithOffset(target, recyclerView.getPaddingTop());
            updateSelectionFromSnap();
        }
    }

    private void updateSelectionFromSnap() {
        if (items.isEmpty()) {
            selectedIndex = 0;
            adapter.notifyDataSetChanged();
            return;
        }
        View snappedView = snapHelper.findSnapView(layoutManager);
        if (snappedView != null) {
            int snappedPosition = layoutManager.getPosition(snappedView);
            if (snappedPosition != RecyclerView.NO_POSITION) {
                int normalized = clamp(snappedPosition);
                if (selectedIndex != normalized) {
                    selectedIndex = normalized;
                    if (listener != null) {
                        listener.onSelectionChanged(selectedIndex, items.get(selectedIndex));
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private int clamp(int index) {
        if (items.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(items.size() - 1, index));
    }

    private View buildLine(Context context) {
        View line = new View(context);
        line.setBackgroundColor(Color.BLACK);
        return line;
    }

    private int dp(int value) {
        return UiUtils.dp(getContext(), value);
    }

    private class WheelAdapter extends RecyclerView.Adapter<WheelViewHolder> {
        @NonNull
        @Override
        public WheelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(ITEM_HEIGHT_DP)
            ));
            textView.setGravity(Gravity.CENTER);
            textView.setIncludeFontPadding(false);
            textView.setTextColor(Color.BLACK);
            textView.setSingleLine(true);
            return new WheelViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull WheelViewHolder holder, int position) {
            boolean selected = position == selectedIndex;
            holder.textView.setText(items.get(position));
            holder.textView.setTextColor(Color.BLACK);
            holder.textView.setAlpha(1f);
            holder.textView.setTypeface(Typeface.create(
                    selected ? "sans-serif-medium" : "sans-serif",
                    Typeface.NORMAL
            ));
            holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, selected ? 24f : 22f);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class WheelViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        WheelViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
