package com.example.finalproject.ui.habit;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.AlphaSliderView;
import com.example.finalproject.ui.common.HueSliderView;
import com.example.finalproject.ui.common.SpectrumColorView;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

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
            int fillColor = parseColor(color, context.getColor(R.color.brand_orange));
            FrameLayout wrapper = new FrameLayout(context);
            boolean selected = selected(normalizeColor(color, context), normalizeColor(selectedColor, context));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(UiUtils.dp(context, 26), UiUtils.dp(context, 26));
            params.setMargins(UiUtils.dp(context, 3), 0, UiUtils.dp(context, 3), 0);
            wrapper.setLayoutParams(params);
            wrapper.setBackground(circleDrawable(
                    Color.TRANSPARENT,
                    selected ? context.getColor(R.color.brand_orange) : Color.TRANSPARENT,
                    selected ? 2 : 0,
                    context
            ));

            View dot = new View(context);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(UiUtils.dp(context, 20), UiUtils.dp(context, 20));
            dotParams.gravity = android.view.Gravity.CENTER;
            dot.setLayoutParams(dotParams);
            dot.setBackground(circleDrawable(fillColor, UiUtils.adaptiveStrokeColor(fillColor, context), 1, context));
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

    public static void bindCustomColorTrigger(Context context, LinearLayout trigger, String[] colors, String selectedColor, OnColorSelected listener) {
        if (trigger == null) {
            return;
        }
        trigger.removeAllViews();
        boolean customSelected = !isPresetColor(context, colors, selectedColor);
        int selected = parseColor(normalizeColor(selectedColor, context), context.getColor(R.color.brand_orange));
        int surface = context.getColor(R.color.surface);
        int line = context.getColor(R.color.line);

        trigger.setGravity(Gravity.CENTER_VERTICAL);
        trigger.setOrientation(LinearLayout.HORIZONTAL);
        trigger.setPadding(UiUtils.dp(context, 8), 0, UiUtils.dp(context, 8), 0);
        trigger.setClickable(true);
        trigger.setFocusable(true);
        trigger.setBackground(customSelected
                ? UiUtils.roundedStroke(context.getColor(R.color.brand_orange_light), context.getColor(R.color.brand_orange), 18, context)
                : UiUtils.roundedStroke(surface, line, 18, context));
        TypedArray attrs = context.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackgroundBorderless});
        trigger.setForeground(attrs.getDrawable(0));
        attrs.recycle();

        View preview = new View(context);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(UiUtils.dp(context, 18), UiUtils.dp(context, 18));
        preview.setLayoutParams(previewParams);
        preview.setBackground(circleDrawable(
                customSelected ? selected : surface,
                customSelected ? UiUtils.adaptiveStrokeColor(selected, context) : line,
                1,
                context
        ));
        trigger.addView(preview);

        TextView label = new TextView(context);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMargins(UiUtils.dp(context, 8), 0, 0, 0);
        label.setLayoutParams(labelParams);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setSingleLine(true);
        label.setText(R.string.custom);
        label.setTextColor(context.getColor(customSelected ? R.color.brand_orange_dark : R.color.text_secondary));
        label.setTextSize(13f);
        trigger.addView(label);

        ImageView check = new ImageView(context);
        check.setLayoutParams(new LinearLayout.LayoutParams(UiUtils.dp(context, 14), UiUtils.dp(context, 14)));
        check.setImageResource(R.drawable.ic_check);
        check.setColorFilter(context.getColor(R.color.brand_orange_dark));
        check.setVisibility(customSelected ? View.VISIBLE : View.GONE);
        trigger.addView(check);

        trigger.setOnClickListener(v -> showCustomColorDialog(context, selectedColor, listener));
    }

    private static void showCustomColorDialog(Context context, String selectedColor, OnColorSelected listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_custom_color, null, false);
        dialog.setContentView(content);
        UiUtils.styleDialogWindow(dialog, resolveCustomColorDialogWidth(context), ViewGroup.LayoutParams.WRAP_CONTENT, 0.28f);

        View preview = content.findViewById(R.id.view_custom_color_preview);
        View inlinePreview = content.findViewById(R.id.view_custom_color_inline_preview);
        SpectrumColorView spectrumView = content.findViewById(R.id.view_custom_color_spectrum);
        HueSliderView hueSliderView = content.findViewById(R.id.view_custom_color_hue);
        AlphaSliderView alphaSliderView = content.findViewById(R.id.view_custom_color_alpha);
        EditText hexEdit = content.findViewById(R.id.edit_custom_color_hex);
        TextView opacityValue = content.findViewById(R.id.tv_custom_color_opacity);

        int initialColor = parseColor(normalizeColor(selectedColor, context), context.getColor(R.color.brand_orange));
        int[] rgbHolder = {Color.rgb(Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor))};
        int[] alphaHolder = {Color.alpha(initialColor)};
        boolean[] updatingHex = {false};

        Runnable syncViews = () -> {
            int color = Color.argb(alphaHolder[0], Color.red(rgbHolder[0]), Color.green(rgbHolder[0]), Color.blue(rgbHolder[0]));
            updateColorPreview(context, preview, color);
            updateColorPreview(context, inlinePreview, color);
            opacityValue.setText(context.getString(R.string.custom_color_opacity_value, Math.round(alphaHolder[0] * 100f / 255f)));
            String rgbHex = colorIntToHex(rgbHolder[0]);
            if (!rgbHex.equalsIgnoreCase(hexEdit.getText().toString())) {
                updatingHex[0] = true;
                hexEdit.setText(rgbHex);
                hexEdit.setSelection(hexEdit.getText().length());
                updatingHex[0] = false;
            }
        };

        float[] hsv = new float[3];
        Color.colorToHSV(rgbHolder[0], hsv);
        hueSliderView.setHue(hsv[0]);
        spectrumView.setSelectedColor(rgbHolder[0]);
        alphaSliderView.setBaseColor(rgbHolder[0]);
        alphaSliderView.setAlphaValue(alphaHolder[0]);
        syncViews.run();

        spectrumView.setOnColorSelectedListener(color -> {
            rgbHolder[0] = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
            alphaSliderView.setBaseColor(rgbHolder[0]);
            syncViews.run();
        });
        hueSliderView.setOnHueChangeListener(hue -> {
            spectrumView.setHue(hue);
            rgbHolder[0] = spectrumView.getSelectedColor();
            alphaSliderView.setBaseColor(rgbHolder[0]);
            syncViews.run();
        });
        alphaSliderView.setOnAlphaChangeListener(alpha -> {
            alphaHolder[0] = alpha;
            syncViews.run();
        });
        hexEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingHex[0]) {
                    return;
                }
                String normalized = normalizeRgbHex(s.toString());
                if (normalized == null) {
                    return;
                }
                rgbHolder[0] = Color.parseColor(normalized);
                float[] typedHsv = new float[3];
                Color.colorToHSV(rgbHolder[0], typedHsv);
                hueSliderView.setHue(typedHsv[0]);
                spectrumView.setSelectedColor(rgbHolder[0]);
                alphaSliderView.setBaseColor(rgbHolder[0]);
                syncViews.run();
            }
        });

        content.findViewById(R.id.btn_custom_color_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_custom_color_apply).setOnClickListener(v -> {
            listener.onSelect(colorIntToHex(Color.argb(
                    alphaHolder[0],
                    Color.red(rgbHolder[0]),
                    Color.green(rgbHolder[0]),
                    Color.blue(rgbHolder[0])
            )));
            dialog.dismiss();
        });
        dialog.show();
    }

    private static GradientDrawable circleDrawable(int fillColor, int strokeColor, int strokeWidthDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        if (strokeWidthDp > 0) {
            drawable.setStroke(UiUtils.dp(context, strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private static void updateColorPreview(Context context, View preview, int color) {
        if (preview instanceof TextView) {
            TextView chip = (TextView) preview;
            chip.setTextColor(UiUtils.readableTextColor(color, context));
            chip.setBackground(UiUtils.adaptiveEventBackground(color, 16, context));
            return;
        }
        preview.setBackground(circleDrawable(color, context.getColor(R.color.line), 1, context));
    }

    private static int parseColor(String value, int fallback) {
        try {
            return value == null ? fallback : Color.parseColor(value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static boolean isPresetColor(Context context, String[] colors, String selectedColor) {
        String normalized = normalizeColor(selectedColor, context);
        for (String color : colors) {
            if (normalizeColor(color, context).equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeColor(String color, Context context) {
        if (color == null || color.trim().isEmpty()) {
            return colorIntToHex(context.getColor(R.color.brand_orange));
        }
        String candidate = color.trim();
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        try {
            return colorIntToHex(Color.parseColor(candidate));
        } catch (IllegalArgumentException ex) {
            return colorIntToHex(context.getColor(R.color.brand_orange));
        }
    }

    private static String normalizeRgbHex(String color) {
        if (color == null || color.trim().isEmpty()) {
            return null;
        }
        String candidate = color.trim().toUpperCase(Locale.US);
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        if (candidate.length() != 7) {
            return null;
        }
        try {
            Color.parseColor(candidate);
            return candidate;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String colorIntToHex(int color) {
        return Color.alpha(color) < 255
                ? String.format(Locale.US, "#%08X", color)
                : String.format(Locale.US, "#%06X", (0xFFFFFF & color));
    }

    private static int resolveCustomColorDialogWidth(Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        return Math.min(UiUtils.dp(context, 372), screenWidth - UiUtils.dp(context, 24));
    }

    private static boolean selected(String left, String right) {
        return left != null && left.equalsIgnoreCase(right == null ? "" : right);
    }
}
