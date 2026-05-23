package com.example.finalproject.ui.habit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public final class HabitUiHelper {
    public interface OnColorSelected {
        void onSelect(String color);
    }

    private HabitUiHelper() {
    }

    public static void styleSheetDialog(Dialog dialog) {
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    public static void populateColorDots(Context context, LinearLayout container, String[] colors, String selectedColor, OnColorSelected listener) {
        container.removeAllViews();
        for (String color : colors) {
            FrameLayout wrapper = new FrameLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(UiUtils.dp(context, 28), UiUtils.dp(context, 28));
            params.setMargins(UiUtils.dp(context, 6), 0, UiUtils.dp(context, 6), 0);
            wrapper.setLayoutParams(params);

            View dot = new View(context);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(UiUtils.dp(context, 18), UiUtils.dp(context, 18));
            dotParams.gravity = android.view.Gravity.CENTER;
            dot.setLayoutParams(dotParams);
            dot.setBackground(circleDrawable(color, selected(color, selectedColor) ? "#E89A54" : null, context));
            wrapper.addView(dot);
            wrapper.setOnClickListener(v -> listener.onSelect(color));
            container.addView(wrapper);
        }
    }

    public static void bindStickerCell(Context context, FrameLayout cell, int imageRes, boolean selected) {
        cell.removeAllViews();
        if (cell.getLayoutParams() == null) {
            cell.setLayoutParams(new ViewGroup.MarginLayoutParams(UiUtils.dp(context, 74), UiUtils.dp(context, 74)));
        }
        cell.setPadding(UiUtils.dp(context, 8), UiUtils.dp(context, 8), UiUtils.dp(context, 8), UiUtils.dp(context, 8));
        cell.setBackground(UiUtils.roundedStroke(
                selected ? context.getColor(R.color.brand_orange_light) : Color.TRANSPARENT,
                selected ? context.getColor(R.color.brand_orange) : Color.TRANSPARENT,
                14,
                context
        ));

        ImageView imageView = new ImageView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(params);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.TRANSPARENT);
        imageView.setImageResource(imageRes);
        cell.addView(imageView);
    }

    public static void stylePriorityChip(TextView view, String label, String color) {
        Context context = view.getContext();
        view.setText(label);
        view.setBackground(UiUtils.rounded(parseColor(color, context.getColor(R.color.brand_orange_light)), 8, context));
        view.setTextColor(context.getColor(R.color.text_primary));
    }

    private static GradientDrawable circleDrawable(String fillColor, String strokeColor, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(parseColor(fillColor, context.getColor(R.color.brand_orange)));
        if (strokeColor != null) {
            drawable.setStroke(UiUtils.dp(context, 2), parseColor(strokeColor, context.getColor(R.color.brand_orange)));
        }
        return drawable;
    }

    private static int parseColor(String value, int fallback) {
        try {
            return value == null ? fallback : Color.parseColor(value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static boolean selected(String left, String right) {
        return left != null && left.equalsIgnoreCase(right == null ? "" : right);
    }
}
