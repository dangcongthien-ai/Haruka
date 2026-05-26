package com.example.finalproject.ui.todo;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.UiUtils;

public class TodoPriorityMatrixController {
    public interface OnPrioritySelectedListener {
        void onPrioritySelected(int priority);
    }

    private final Context context;
    private final TextView selectedSummaryView;
    private final View[] cards = new View[5];
    private final View[] badges = new View[5];
    private final View[] iconCircles = new View[5];
    private final TextView[] chips = new TextView[5];
    private int priority = 3;
    private boolean interactive = true;
    private OnPrioritySelectedListener listener;

    public TodoPriorityMatrixController(View root) {
        context = root.getContext();
        selectedSummaryView = root.findViewById(R.id.tv_priority_selected);
        cards[1] = root.findViewById(R.id.priority_1);
        cards[2] = root.findViewById(R.id.priority_2);
        cards[3] = root.findViewById(R.id.priority_3);
        cards[4] = root.findViewById(R.id.priority_4);
        badges[1] = root.findViewById(R.id.priority_badge_1);
        badges[2] = root.findViewById(R.id.priority_badge_2);
        badges[3] = root.findViewById(R.id.priority_badge_3);
        badges[4] = root.findViewById(R.id.priority_badge_4);
        iconCircles[1] = root.findViewById(R.id.priority_icon_circle_1);
        iconCircles[2] = root.findViewById(R.id.priority_icon_circle_2);
        iconCircles[3] = root.findViewById(R.id.priority_icon_circle_3);
        iconCircles[4] = root.findViewById(R.id.priority_icon_circle_4);
        chips[1] = root.findViewById(R.id.priority_chip_1);
        chips[2] = root.findViewById(R.id.priority_chip_2);
        chips[3] = root.findViewById(R.id.priority_chip_3);
        chips[4] = root.findViewById(R.id.priority_chip_4);
        bindClicks();
        refresh();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        for (int index = 1; index <= 4; index++) {
            cards[index].setClickable(interactive);
            cards[index].setFocusable(interactive);
        }
    }

    public void setOnPrioritySelectedListener(OnPrioritySelectedListener listener) {
        this.listener = listener;
    }

    public void setPriority(int priority) {
        this.priority = normalize(priority);
        refresh();
    }

    public int getPriority() {
        return priority;
    }

    private void bindClicks() {
        for (int index = 1; index <= 4; index++) {
            final int selected = index;
            cards[index].setOnClickListener(v -> {
                if (!interactive) {
                    return;
                }
                setPriority(selected);
                if (listener != null) {
                    listener.onPrioritySelected(selected);
                }
            });
        }
    }

    private void refresh() {
        selectedSummaryView.setText(context.getString(
                R.string.priority_selected_format,
                context.getString(nameRes(priority)),
                context.getString(labelRes(priority))
        ));
        for (int index = 1; index <= 4; index++) {
            boolean selected = index == priority;
            cards[index].setBackground(buildCardBackground(fillColor(index), accentColor(index), selected));
            badges[index].setVisibility(selected ? View.VISIBLE : View.GONE);
            badges[index].setBackground(UiUtils.rounded(accentColor(index), 16, context));
            iconCircles[index].setBackground(UiUtils.rounded(ColorUtils.setAlphaComponent(accentColor(index), 24), 21, context));
            chips[index].setBackground(UiUtils.rounded(ColorUtils.setAlphaComponent(accentColor(index), 22), 12, context));
            chips[index].setTextColor(accentColor(index));
            chips[index].setAlpha(selected ? 1f : 0.92f);
            cards[index].setAlpha(selected ? 1f : 0.98f);
        }
    }

    private GradientDrawable buildCardBackground(int fill, int stroke, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(UiUtils.dp(context, 18));
        drawable.setStroke(UiUtils.dp(context, selected ? 2 : 1), stroke);
        return drawable;
    }

    private int normalize(int priority) {
        return priority >= 1 && priority <= 4 ? priority : 3;
    }

    private int fillColor(int priority) {
        if (priority == 1) {
            return context.getColor(R.color.priority_1);
        }
        if (priority == 2) {
            return context.getColor(R.color.priority_2);
        }
        if (priority == 3) {
            return context.getColor(R.color.priority_3);
        }
        return context.getColor(R.color.priority_4);
    }

    private int accentColor(int priority) {
        if (priority == 1) {
            return context.getColor(R.color.priority_1_accent);
        }
        if (priority == 2) {
            return context.getColor(R.color.priority_2_accent);
        }
        if (priority == 3) {
            return context.getColor(R.color.priority_3_accent);
        }
        return context.getColor(R.color.priority_4_accent);
    }

    private int nameRes(int priority) {
        if (priority == 1) {
            return R.string.do_now;
        }
        if (priority == 2) {
            return R.string.delegate;
        }
        if (priority == 3) {
            return R.string.schedule;
        }
        return R.string.eliminate;
    }

    private int labelRes(int priority) {
        if (priority == 1) {
            return R.string.priority_1;
        }
        if (priority == 2) {
            return R.string.priority_2;
        }
        if (priority == 3) {
            return R.string.priority_3;
        }
        return R.string.priority_4;
    }
}
