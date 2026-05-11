package com.example.finalproject.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;

public final class UiUtils {
    private UiUtils() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int safeColor(String color, int fallback) {
        try {
            return color == null || color.isEmpty() ? fallback : Color.parseColor(color);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, Math.round(radiusDp)));
        return drawable;
    }

    public static GradientDrawable roundedStroke(int fill, int stroke, int radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    public static void selectSegment(Context context, MaterialButton selected, MaterialButton... others) {
        selected.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getColor(R.color.brand_orange_light)));
        selected.setTextColor(context.getColor(R.color.brand_orange_dark));
        selected.setTypeface(Typeface.DEFAULT_BOLD);
        for (MaterialButton button : others) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getColor(R.color.surface)));
            button.setTextColor(context.getColor(R.color.text_primary));
            button.setTypeface(Typeface.DEFAULT);
        }
    }

    public static void setCheck(TextView view, boolean checked) {
        view.setText(checked ? "✓" : "");
    }

    public static void visible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static void showDeleteDialog(Context context, String message, Runnable onConfirm) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null, false);
        TextView messageView = content.findViewById(R.id.tv_delete_message);
        messageView.setText(message);
        content.findViewById(R.id.btn_delete_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_delete_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }
}
