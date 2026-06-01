package com.example.finalproject.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WheelPickerView extends FrameLayout {
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int index, String value);
    }

    private static final int ITEM_HEIGHT_DP = 42;
    private static final int SELECTED_TEXT_SP = 18;
    private static final int NORMAL_TEXT_SP = 16;

    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final LinearSnapHelper snapHelper;
    private final PickerAdapter adapter;
    private final View topDivider;
    private final View bottomDivider;
    private final int itemHeightPx;
    private final int dividerHeightPx;
    private final List<String> items = new ArrayList<>();

    private int selectedIndex;
    private OnSelectionChangedListener selectionChangedListener;

    public WheelPickerView(@NonNull Context context) {
        this(context, null);
    }

    public WheelPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForceDarkAllowed(false);
        }
        itemHeightPx = dp(context, ITEM_HEIGHT_DP);
        dividerHeightPx = 1;

        layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);
        recyclerView.setItemAnimator(null);
        recyclerView.setHasFixedSize(true);
        recyclerView.setVerticalScrollBarEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recyclerView.setForceDarkAllowed(false);
        }

        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        adapter = new PickerAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    syncSelectionFromScroll();
                }
            }
        });

        addView(recyclerView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        topDivider = createDivider(context);
        bottomDivider = createDivider(context);
        addView(topDivider, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dividerHeightPx));
        addView(bottomDivider, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dividerHeightPx));
    }

    public void setItems(@Nullable List<String> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        if (selectedIndex >= items.size()) {
            selectedIndex = Math.max(0, items.size() - 1);
        }
        adapter.notifyDataSetChanged();
        post(() -> scrollToIndex(selectedIndex, false));
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, false);
    }

    public void setSelectedIndex(int index, boolean smooth) {
        if (items.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        int newIndex = Math.max(0, Math.min(index, items.size() - 1));
        int oldIndex = selectedIndex;
        selectedIndex = newIndex;
        if (oldIndex != newIndex) {
            if (oldIndex >= 0 && oldIndex < items.size()) {
                adapter.notifyItemChanged(oldIndex);
            }
            adapter.notifyItemChanged(newIndex);
        }
        post(() -> scrollToIndex(selectedIndex, smooth));
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChangedListener listener) {
        selectionChangedListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int centerPadding = Math.max(0, (h - itemHeightPx) / 2);
        recyclerView.setPadding(0, centerPadding, 0, centerPadding);
        float topY = (h - itemHeightPx) / 2f;
        float bottomY = topY + itemHeightPx - dividerHeightPx;
        topDivider.setY(topY);
        bottomDivider.setY(bottomY);
        scrollToIndex(selectedIndex, false);
    }

    private void scrollToIndex(int index, boolean smooth) {
        if (layoutManager == null || items.isEmpty() || getHeight() == 0) {
            return;
        }
        if (!smooth) {
            layoutManager.scrollToPositionWithOffset(index, 0);
            syncSelectionFromScroll();
            return;
        }
        RecyclerView.SmoothScroller scroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 72f / displayMetrics.densityDpi;
            }

            @Override
            protected int calculateTimeForDeceleration(int dx) {
                return 220;
            }

            @Override
            public int calculateDyToMakeVisible(View view, int snapPreference) {
                return recyclerView.getPaddingTop() - view.getTop();
            }
        };
        scroller.setTargetPosition(index);
        layoutManager.startSmoothScroll(scroller);
    }

    private void syncSelectionFromScroll() {
        View snapped = snapHelper.findSnapView(layoutManager);
        if (snapped == null) {
            return;
        }
        int position = layoutManager.getPosition(snapped);
        if (position == RecyclerView.NO_POSITION || position == selectedIndex) {
            return;
        }
        int oldIndex = selectedIndex;
        selectedIndex = position;
        if (oldIndex >= 0 && oldIndex < items.size()) {
            adapter.notifyItemChanged(oldIndex);
        }
        adapter.notifyItemChanged(position);
        if (selectionChangedListener != null && position < items.size()) {
            selectionChangedListener.onSelectionChanged(position, items.get(position));
        }
    }

    private View createDivider(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(Color.argb(148, 44, 34, 28));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            divider.setForceDarkAllowed(false);
        }
        return divider;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private class PickerAdapter extends RecyclerView.Adapter<PickerAdapter.PickerViewHolder> {
        @NonNull
        @Override
        public PickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    itemHeightPx
            );
            textView.setLayoutParams(params);
            textView.setGravity(Gravity.CENTER);
            textView.setIncludeFontPadding(false);
            textView.setTextColor(Color.BLACK);
            textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                textView.setForceDarkAllowed(false);
            }
            return new PickerViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull PickerViewHolder holder, int position) {
            boolean selected = position == selectedIndex;
            holder.textView.setText(items.get(position));
            holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, selected ? SELECTED_TEXT_SP : NORMAL_TEXT_SP);
            holder.textView.setTypeface(Typeface.create(selected ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
            holder.textView.setTextColor(Color.BLACK);
            holder.textView.setAlpha(selected ? 1f : 0.42f);
            holder.textView.setOnClickListener(v -> setSelectedIndex(holder.getBindingAdapterPosition(), true));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PickerViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            PickerViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }
}
