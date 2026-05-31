package com.example.finalproject.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.graphics.ColorUtils;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

public final class UiUtils {
    private static final long DEFAULT_DEBOUNCE_INTERVAL_MS = 550L;
    private static final WeakHashMap<View, Long> LAST_CLICK_TIMES = new WeakHashMap<>();

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

    public static GradientDrawable adaptiveEventBackground(int fill, int radiusDp, Context context) {
        int stroke = adaptiveStrokeColor(fill, context);
        return stroke == Color.TRANSPARENT
                ? rounded(fill, radiusDp, context)
                : roundedStroke(fill, stroke, radiusDp, context);
    }

    public static int readableTextColor(int backgroundColor, Context context) {
        int effectiveBackground = compositeOverSurface(backgroundColor, context);
        int darkText = context.getColor(R.color.text_primary);
        int lightText = context.getColor(R.color.white);
        return contrastRatio(effectiveBackground, lightText) >= contrastRatio(effectiveBackground, darkText)
                ? lightText
                : darkText;
    }

    public static int adaptiveStrokeColor(int fill, Context context) {
        int surface = context.getColor(R.color.surface);
        int effectiveFill = compositeOverSurface(fill, context);
        return contrastRatio(effectiveFill, surface) < 1.25d
                ? context.getColor(R.color.line)
                : Color.TRANSPARENT;
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
        view.setText(checked ? "\u2713" : "");
    }

    public static void visible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static void setDebouncedClickListener(View view, Runnable action) {
        setDebouncedClickListener(view, DEFAULT_DEBOUNCE_INTERVAL_MS, action);
    }

    public static void setDebouncedClickListener(View view, long intervalMs, Runnable action) {
        if (view == null) {
            return;
        }
        view.setOnClickListener(v -> {
            if (action == null || isRapidClick(v, intervalMs)) {
                return;
            }
            action.run();
        });
    }

    public static boolean isRapidClick(View view) {
        return isRapidClick(view, DEFAULT_DEBOUNCE_INTERVAL_MS);
    }

    public static boolean isRapidClick(View view, long intervalMs) {
        if (view == null) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        Long lastClick = LAST_CLICK_TIMES.get(view);
        if (lastClick != null && now - lastClick < Math.max(0L, intervalMs)) {
            return true;
        }
        LAST_CLICK_TIMES.put(view, now);
        return false;
    }

    public static void styleNumberPicker(NumberPicker picker, Context context) {
        if (picker == null) {
            return;
        }
        disableForceDark(picker);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setBackgroundColor(Color.TRANSPARENT);
        picker.setVerticalFadingEdgeEnabled(false);
        picker.setFadingEdgeLength(0);
        picker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        applyNumberPickerColors(picker, context);
        picker.post(() -> applyNumberPickerColors(picker, context));
        picker.postDelayed(() -> applyNumberPickerColors(picker, context), 32L);
        picker.postDelayed(() -> applyNumberPickerColors(picker, context), 96L);
    }

    private static void applyNumberPickerColors(NumberPicker picker, Context context) {
        int fixedBlack = Color.BLACK;
        try {
            Field dividerField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            dividerField.setAccessible(true);
            dividerField.set(picker, new ColorDrawable(fixedBlack));

            Field heightField = NumberPicker.class.getDeclaredField("mSelectionDividerHeight");
            heightField.setAccessible(true);
            heightField.setInt(picker, 1);

            Field paintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            paintField.setAccessible(true);
            Paint paint = (Paint) paintField.get(picker);
            if (paint != null) {
                paint.setAntiAlias(true);
                paint.setColor(fixedBlack);
                paint.setAlpha(255);
                paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                paint.setTextSize(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        24f,
                        context.getResources().getDisplayMetrics()
                ));
            }

            for (int i = 0; i < picker.getChildCount(); i++) {
                View child = picker.getChildAt(i);
                if (child instanceof EditText) {
                    styleNumberPickerInput((EditText) child, fixedBlack, context);
                }
            }
            picker.invalidate();
            picker.requestLayout();
        } catch (Exception ignored) {
        }
    }

    private static void styleNumberPickerInput(EditText editText, int textColor, Context context) {
        disableForceDark(editText);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setTextColor(textColor);
        editText.setHintTextColor(textColor);
        editText.setHighlightColor(ColorUtils.setAlphaComponent(textColor, 72));
        editText.setLinkTextColor(textColor);
        editText.setAlpha(1f);
        editText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
    }

    public static void showDeleteDialog(Context context, String message, Runnable onConfirm) {
        showConfirmationDialog(
                context,
                R.drawable.ic_delete,
                context.getString(R.string.delete_dialog_title),
                message,
                context.getString(R.string.delete),
                onConfirm
        );
    }

    public static void showConfirmationDialog(
            Context context,
            @DrawableRes int iconResId,
            String title,
            String message,
            String confirmText,
            Runnable onConfirm
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null, false);
        ImageView iconView = content.findViewById(R.id.iv_delete_icon);
        TextView titleView = content.findViewById(R.id.tv_delete_title);
        TextView messageView = content.findViewById(R.id.tv_delete_message);
        TextView confirmView = content.findViewById(R.id.btn_delete_confirm);
        iconView.setImageResource(iconResId);
        titleView.setText(title);
        messageView.setText(message);
        confirmView.setText(confirmText);
        setDebouncedClickListener(content.findViewById(R.id.btn_delete_cancel), dialog::dismiss);
        setDebouncedClickListener(confirmView, () -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.setContentView(content);
        styleDialogWindow(dialog, dp(context, 312), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public static void showConfirmDialog(Context context, String message, String confirmText, Runnable onConfirm) {
        showConfirmationDialog(
                context,
                R.drawable.ic_close_centered,
                context.getString(R.string.discard_changes_title),
                message,
                confirmText,
                onConfirm
        );
    }

    public static void styleDialogWindow(Dialog dialog, int width, int height, float dimAmount) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.getDecorView().setForceDarkAllowed(false);
        }
        window.setLayout(width, height);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = dimAmount;
        attributes.windowAnimations = R.style.Haruka_DialogWindowAnimation;
        window.setAttributes(attributes);
    }

    public static void disableForceDark(View view) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        view.setForceDarkAllowed(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                disableForceDark(group.getChildAt(i));
            }
        }
    }

    private static double contrastRatio(int colorA, int colorB) {
        double luminanceA = relativeLuminance(colorA);
        double luminanceB = relativeLuminance(colorB);
        double lighter = Math.max(luminanceA, luminanceB);
        double darker = Math.min(luminanceA, luminanceB);
        return (lighter + 0.05d) / (darker + 0.05d);
    }

    private static int compositeOverSurface(int color, Context context) {
        return Color.alpha(color) < 255
                ? ColorUtils.compositeColors(color, context.getColor(R.color.surface))
                : color;
    }

    private static double relativeLuminance(int color) {
        return 0.2126d * linearizedChannel(Color.red(color) / 255d)
                + 0.7152d * linearizedChannel(Color.green(color) / 255d)
                + 0.0722d * linearizedChannel(Color.blue(color) / 255d);
    }

    private static double linearizedChannel(double channel) {
        return channel <= 0.03928d
                ? channel / 12.92d
                : Math.pow((channel + 0.055d) / 1.055d, 2.4d);
    }
}
