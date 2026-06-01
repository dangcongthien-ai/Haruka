package com.example.finalproject.ui.journal;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;

import java.util.ArrayList;
import java.util.List;

public final class JournalMoodPickerDialog {
    private static final String MOOD_BUON_NGU = "journal_emo_buon_ngu";
    private static final int MOOD_SLOT_WEATHER = 0;
    private static final int MOOD_SLOT_DAY = 1;
    private static final int MOOD_SLOT_BODY = 2;

    private JournalMoodPickerDialog() {
    }

    public interface MoodPickerCallback {
        void run();
    }

    public static int[] defaultMoodResources() {
        return new int[]{
                R.drawable.journal_emo_nang,
                R.drawable.journal_emo_vui,
                R.drawable.journal_emo_sang_khoai
        };
    }

    public static void show(
            @NonNull Context context,
            @NonNull int[] selectedMoodResources,
            @Nullable MoodPickerCallback onSelectionChanged,
            @Nullable MoodPickerCallback onDone
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_journal_mood_picker);

        ImageButton closeButton = dialog.findViewById(R.id.btn_close_journal_mood_picker);
        TextView doneButton = dialog.findViewById(R.id.btn_done_journal_mood_picker);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        doneButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDone != null) {
                onDone.run();
            }
        });

        View[] selectedTiles = new View[3];
        populateMoodGrid(context, dialog.findViewById(R.id.grid_journal_weather),
                weatherMoodOptions(), selectedMoodResources, selectedTiles, onSelectionChanged);
        populateMoodGrid(context, dialog.findViewById(R.id.grid_journal_day_mood),
                dayMoodOptions(), selectedMoodResources, selectedTiles, onSelectionChanged);
        populateMoodGrid(context, dialog.findViewById(R.id.grid_journal_body_mood),
                bodyMoodOptions(), selectedMoodResources, selectedTiles, onSelectionChanged);

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.dimAmount = 0f;
            window.setAttributes(params);
        }
    }

    public static ArrayList<String> selectedMoodResourceNames(
            @NonNull Context context,
            @NonNull int[] selectedMoodResources
    ) {
        ArrayList<String> moodResourceNames = new ArrayList<>();
        for (int moodResource : selectedMoodResources) {
            try {
                String resourceName = context.getResources().getResourceEntryName(moodResource);
                moodResourceNames.add("journal_emo_buon_ngu_clean".equals(resourceName)
                        ? MOOD_BUON_NGU
                        : resourceName);
            } catch (android.content.res.Resources.NotFoundException ignored) {
                moodResourceNames.add("");
            }
        }
        return moodResourceNames;
    }

    public static int moodResource(@NonNull Context context, @Nullable String resourceName, int fallback) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            return fallback;
        }
        String resolvedName = MOOD_BUON_NGU.equals(resourceName.trim())
                ? "journal_emo_buon_ngu_clean"
                : resourceName.trim();
        int resourceId = context.getResources().getIdentifier(resolvedName, "drawable", context.getPackageName());
        return resourceId == 0 ? fallback : resourceId;
    }

    private static void populateMoodGrid(
            Context context,
            GridLayout grid,
            MoodOption[] options,
            int[] selectedMoodResources,
            View[] selectedTiles,
            @Nullable MoodPickerCallback onSelectionChanged
    ) {
        if (grid == null) {
            return;
        }
        grid.removeAllViews();
        grid.setColumnCount(5);
        for (int index = 0; index < options.length; index++) {
            MoodOption option = options[index];
            LinearLayout tile = new LinearLayout(context);
            tile.setGravity(Gravity.CENTER);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
            tile.setClickable(true);
            tile.setFocusable(true);
            android.content.res.TypedArray attrs = context.obtainStyledAttributes(
                    new int[]{android.R.attr.selectableItemBackground}
            );
            tile.setForeground(attrs.getDrawable(0));
            attrs.recycle();
            tile.setContentDescription(context.getString(option.labelRes));

            ImageView image = new ImageView(context);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(context, 58), dp(context, 58));
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageResource(option.drawableRes);
            tile.addView(image, imageParams);

            TextView label = new TextView(context);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setText(option.labelRes);
            label.setTextColor(context.getResources().getColor(R.color.text_primary));
            label.setTextSize(10);
            tile.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 20)));

            GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
            tileParams.columnSpec = GridLayout.spec(index % 5, 1f);
            tileParams.width = 0;
            tileParams.height = dp(context, 82);
            tile.setLayoutParams(tileParams);

            if (selectedMoodResources[option.slotIndex] == option.drawableRes) {
                tile.setBackgroundResource(R.drawable.bg_journal_mood_tile_selected);
                selectedTiles[option.slotIndex] = tile;
            }
            tile.setOnClickListener(v -> {
                selectedMoodResources[option.slotIndex] = option.drawableRes;
                if (selectedTiles[option.slotIndex] != null) {
                    selectedTiles[option.slotIndex].setBackgroundColor(Color.TRANSPARENT);
                }
                v.setBackgroundResource(R.drawable.bg_journal_mood_tile_selected);
                selectedTiles[option.slotIndex] = v;
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
            });
            grid.addView(tile);
        }
    }

    private static MoodOption[] weatherMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_nang, R.string.journal_emo_nang, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_mua, R.string.journal_emo_mua, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_am_u, R.string.journal_emo_am_u, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_nhieu_may, R.string.journal_emo_nhieu_may, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_tuyet, R.string.journal_emo_tuyet, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_gio, R.string.journal_emo_gio, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_bao, R.string.journal_emo_bao, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_nong, R.string.journal_emo_nong, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_sam, R.string.journal_emo_sam, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_suong, R.string.journal_emo_suong, MOOD_SLOT_WEATHER)
        };
    }

    private static MoodOption[] dayMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_vui, R.string.journal_emo_vui, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_tu_hao, R.string.journal_emo_tu_hao, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_phan_khich, R.string.journal_emo_phan_khich, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_binh_yen, R.string.journal_emo_binh_yen, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_binh_thuong, R.string.journal_emo_binh_thuong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_lo_lang, R.string.journal_emo_lo_lang, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_met_moi, R.string.journal_emo_met_moi, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_buon_ba, R.string.journal_emo_buon, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_chan_nan, R.string.journal_emo_chan_nan, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_tuc_gian, R.string.journal_emo_tuc_gian, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ton_thuong, R.string.journal_emo_ton_thuong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_hi_vong, R.string.journal_emo_hi_vong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_hoai_niem, R.string.journal_emo_hoai_niem, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_nang_suat, R.string.journal_emo_nang_suat, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ban_ron, R.string.journal_emo_ban_ron, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ham_bik_nua, R.string.journal_emo_ham_bik_nua, MOOD_SLOT_DAY)
        };
    }

    private static MoodOption[] bodyMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_sang_khoai, R.string.journal_emo_sang_khoai, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_om, R.string.journal_emo_om, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_buon_ngu_clean, R.string.journal_emo_buon_ngu, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_tinh_than_on, R.string.journal_emo_tinh_than_on, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_cang_thang, R.string.journal_emo_cang_thang, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_doi_bung, R.string.journal_emo_doi_bung, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_dang_hoi_phuc, R.string.journal_emo_dang_hoi_phuc, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_kiet_suc, R.string.journal_emo_kiet_suc, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_chi_muon_nam, R.string.journal_emo_chi_muon_nam, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_thu_gian, R.string.journal_emo_thu_gian, MOOD_SLOT_BODY)
        };
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static class MoodOption {
        final int drawableRes;
        final int labelRes;
        final int slotIndex;

        MoodOption(int drawableRes, int labelRes, int slotIndex) {
            this.drawableRes = drawableRes;
            this.labelRes = labelRes;
            this.slotIndex = slotIndex;
        }
    }
}
