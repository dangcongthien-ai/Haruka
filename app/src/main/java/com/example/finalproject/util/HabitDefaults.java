package com.example.finalproject.util;

import android.content.Context;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.model.HabitPriority;

import java.util.ArrayList;
import java.util.List;

public final class HabitDefaults {
    public static final String[] COLOR_OPTIONS = {
            "#F7C4DA",
            "#F29DD3",
            "#D8A8E8",
            "#9CC0E7",
            "#49C8D7",
            "#8BE663",
            "#FFBE4D",
            "#FFD342",
            "#F3B46E",
            "#FF4036",
            "#25D07F",
            "#3E66C7"
    };

    public static final String[] STICKER_OPTIONS = {
            "habit_category_work",
            "habit_category_study",
            "habit_category_art",
            "habit_category_outdoor",
            "habit_category_health",
            "habit_category_gym",
            "habit_category_other",
            "habit_sticker_hat_dress",
            "habit_sticker_witch",
            "habit_sticker_hoodie",
            "habit_sticker_chef",
            "habit_sticker_yellow_dress"
    };

    private HabitDefaults() {
    }

    public static List<HabitCategory> defaultCategories(Context context) {
        List<HabitCategory> categories = new ArrayList<>();
        categories.add(category(context.getString(R.string.habit_category_work), "habit_category_work", "#F0D4C0"));
        categories.add(category(context.getString(R.string.habit_category_study), "habit_category_study", "#D6E4FF"));
        categories.add(category(context.getString(R.string.habit_category_art), "habit_category_art", "#F8D8F1"));
        categories.add(category(context.getString(R.string.habit_category_outdoor), "habit_category_outdoor", "#E5F0FF"));
        categories.add(category(context.getString(R.string.habit_category_health), "habit_category_health", "#D9F6EE"));
        categories.add(category(context.getString(R.string.habit_category_gym), "habit_category_gym", "#E3F0FF"));
        categories.add(category(context.getString(R.string.habit_category_other), "habit_category_other", "#EAE7FF"));
        return categories;
    }

    public static List<HabitPriority> defaultPriorities(Context context) {
        List<HabitPriority> priorities = new ArrayList<>();
        priorities.add(priority(context.getString(R.string.high), "#F7B7D9", 1));
        priorities.add(priority(context.getString(R.string.habit_priority_medium), "#9BF15C", 2));
        priorities.add(priority(context.getString(R.string.low), "#9EC2EA", 3));
        return priorities;
    }

    public static int resolveIconRes(Context context, String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            return R.drawable.habit_category_other;
        }
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        return resId == 0 ? R.drawable.habit_category_other : resId;
    }

    private static HabitCategory category(String name, String iconUri, String color) {
        HabitCategory category = new HabitCategory();
        category.setName(name);
        category.setIconUri(iconUri);
        category.setColor(color);
        return category;
    }

    private static HabitPriority priority(String name, String color, int order) {
        HabitPriority priority = new HabitPriority();
        priority.setName(name);
        priority.setColor(color);
        priority.setPriorityOrder(order);
        return priority;
    }
}
