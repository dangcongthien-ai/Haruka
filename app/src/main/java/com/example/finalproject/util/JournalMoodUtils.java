package com.example.finalproject.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.List;

public final class JournalMoodUtils {
    private static final String MOOD_BUON_NGU = "journal_emo_buon_ngu";
    private static final int DAY_MOOD_INDEX = 1;

    private JournalMoodUtils() {
    }

    @Nullable
    public static String dayMoodName(@Nullable List<String> moodResourceNames) {
        if (moodResourceNames == null || moodResourceNames.size() <= DAY_MOOD_INDEX) {
            return null;
        }
        String value = moodResourceNames.get(DAY_MOOD_INDEX);
        return TextUtils.isEmpty(value) ? null : value.trim();
    }

    @DrawableRes
    public static int resolveMoodResource(Context context, @Nullable String resourceName) {
        if (context == null || TextUtils.isEmpty(resourceName)) {
            return 0;
        }
        String resolvedName = MOOD_BUON_NGU.equals(resourceName.trim())
                ? "journal_emo_buon_ngu_clean"
                : resourceName.trim();
        return context.getResources().getIdentifier(resolvedName, "drawable", context.getPackageName());
    }
}
