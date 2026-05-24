package com.example.finalproject.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitListItem;
import com.example.finalproject.model.RecurrenceRule;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.ui.habit.HabitUiHelper;
import com.example.finalproject.util.HabitDefaults;

import java.util.ArrayList;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {
    public interface Listener {
        void onClick(HabitListItem item);

        void onAction(HabitListItem item);

        void onEdit(HabitListItem item);

        void onDelete(HabitListItem item);
    }

    private final List<HabitListItem> items = new ArrayList<>();
    private final Listener listener;

    public HabitAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<HabitListItem> habits) {
        items.clear();
        if (habits != null) {
            items.addAll(habits);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        HabitListItem item = items.get(position);
        HabitItem habit = item.getHabit();
        holder.title.setText(habit.getTitle());
        String categoryName = habit.getCategory() == null
                ? holder.itemView.getContext().getString(R.string.habit_category_other)
                : habit.getCategory().getName();
        holder.category.setText(categoryName);
        holder.progressTrack.setBackground(null);
        bindProgressFill(holder, item);

        int iconRes = habit.getCategory() == null
                ? R.drawable.habit_category_other
                : HabitDefaults.resolveIconRes(holder.itemView.getContext(), habit.getCategory().getIconUri());
        holder.icon.setImageResource(iconRes);

        if (habit.getPriority() != null) {
            HabitUiHelper.stylePriorityChip(holder.priority, habit.getPriority().getName(), habit.getPriority().getColor());
            holder.priority.setVisibility(View.VISIBLE);
        } else {
            holder.priority.setVisibility(View.GONE);
        }

        boolean showTomorrowNote = item.isArchivedByCompletion() && isRepeating(habit);
        holder.note.setText(R.string.habit_done_tomorrow_note);
        holder.note.setVisibility(showTomorrowNote ? View.VISIBLE : View.GONE);

        holder.action.setVisibility(View.VISIBLE);
        bindAction(holder, item);
        boolean canUseAction = !item.isArchived() || item.isArchivedByCompletion();
        if (canUseAction) {
            holder.action.setClickable(true);
            holder.action.setFocusable(true);
            holder.action.setOnClickListener(v -> listener.onAction(item));
        } else {
            holder.action.setOnClickListener(null);
            holder.action.setClickable(false);
            holder.action.setFocusable(false);
        }
        holder.edit.setOnClickListener(v -> listener.onEdit(item));
        holder.delete.setOnClickListener(v -> listener.onDelete(item));
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    private void bindAction(HabitViewHolder holder, HabitListItem item) {
        holder.action.setVisibility(View.VISIBLE);
        holder.actionIcon.setVisibility(View.GONE);
        holder.actionText.setVisibility(View.GONE);
        if (item.isArchived() && !item.isArchivedByCompletion()) {
            holder.action.setVisibility(View.INVISIBLE);
            return;
        }
        if (item.isArchivedByCompletion() && item.isCompletedOnSelectedDate()) {
            holder.action.setBackgroundResource(R.drawable.bg_habit_action_circle_fill);
            holder.actionIcon.setVisibility(View.VISIBLE);
            return;
        }
        if (item.getHabit().isNumberEvaluation()) {
            holder.action.setBackgroundResource(R.drawable.bg_habit_action_circle_fill);
            holder.actionText.setVisibility(View.VISIBLE);
            return;
        }
        if (item.isCompletedOnSelectedDate()) {
            holder.action.setBackgroundResource(R.drawable.bg_habit_action_circle_fill);
            holder.actionIcon.setVisibility(View.VISIBLE);
            return;
        }
        holder.action.setBackgroundResource(R.drawable.bg_habit_action_circle_outline);
    }

    private void bindProgressFill(HabitViewHolder holder, HabitListItem item) {
        float fraction = Math.max(0f, Math.min(1f, item.getProgressFraction()));
        long habitId = item.getHabit().getId();
        holder.progressTrack.setTag(habitId);
        holder.progressTrack.post(() -> {
            Object tag = holder.progressTrack.getTag();
            if (!(tag instanceof Long) || ((Long) tag) != habitId) {
                return;
            }
            int trackHeight = holder.progressTrack.getHeight();
            int fillHeight = Math.round(trackHeight * fraction);
            ViewGroup.LayoutParams params = holder.accentFill.getLayoutParams();
            params.height = fillHeight;
            holder.accentFill.setLayoutParams(params);
            if (fillHeight <= 0) {
                holder.accentFill.setVisibility(View.INVISIBLE);
                holder.accentFill.setBackground(null);
                return;
            }
            holder.accentFill.setVisibility(View.VISIBLE);
            holder.accentFill.setBackground(progressDrawable(
                    UiUtils.safeColor(
                            item.getHabit().getColor(),
                            holder.itemView.getContext().getColor(R.color.brand_orange_light)
                    ),
                    fraction,
                    holder.itemView.getContext()
            ));
        });
    }

    private GradientDrawable progressDrawable(int color, float fraction, android.content.Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        float radius = UiUtils.dp(context, 22);
        if (fraction >= 0.999f) {
            drawable.setCornerRadii(new float[]{
                    radius, radius,
                    0f, 0f,
                    0f, 0f,
                    radius, radius
            });
        } else {
            drawable.setCornerRadii(new float[]{
                    0f, 0f,
                    0f, 0f,
                    0f, 0f,
                    radius, radius
            });
        }
        return drawable;
    }

    private boolean isRepeating(HabitItem habit) {
        RecurrenceRule rule = habit == null ? null : habit.getRecurrenceRule();
        return rule != null && !rule.isNone();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout progressTrack;
        final View accentFill;
        final ImageView icon;
        final TextView category;
        final TextView title;
        final TextView priority;
        final TextView note;
        final FrameLayout action;
        final ImageView actionIcon;
        final TextView actionText;
        final ImageView edit;
        final ImageView delete;

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            progressTrack = itemView.findViewById(R.id.layout_habit_progress);
            accentFill = itemView.findViewById(R.id.view_habit_accent_fill);
            icon = itemView.findViewById(R.id.img_habit_category);
            category = itemView.findViewById(R.id.tv_habit_category);
            title = itemView.findViewById(R.id.tv_habit_title);
            priority = itemView.findViewById(R.id.tv_habit_priority);
            note = itemView.findViewById(R.id.tv_habit_note);
            action = itemView.findViewById(R.id.layout_habit_action);
            actionIcon = itemView.findViewById(R.id.img_habit_action_icon);
            actionText = itemView.findViewById(R.id.tv_habit_action_text);
            edit = itemView.findViewById(R.id.btn_habit_edit);
            delete = itemView.findViewById(R.id.btn_habit_delete);
        }
    }
}
