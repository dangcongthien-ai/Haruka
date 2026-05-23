package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.ui.habit.HabitUiHelper;
import com.example.finalproject.util.HabitDefaults;

import java.util.ArrayList;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {
    public interface Listener {
        void onClick(HabitItem item);
    }

    private final List<HabitItem> items = new ArrayList<>();
    private final Listener listener;

    public HabitAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<HabitItem> habits) {
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
        HabitItem item = items.get(position);
        holder.title.setText(item.getTitle());
        String categoryName = item.getCategory() == null ? holder.itemView.getContext().getString(R.string.habit_category_other) : item.getCategory().getName();
        String dateText = DateTimeUtils.formatVietnameseDate(item.getStartDate());
        holder.meta.setText(categoryName + " • " + dateText);
        holder.repeat.setText(item.getRecurrenceRule() == null ? holder.itemView.getContext().getString(R.string.habit_repeat_none) : item.getRecurrenceRule().getDisplayText());
        int iconRes = item.getCategory() == null
                ? R.drawable.habit_category_other
                : HabitDefaults.resolveIconRes(holder.itemView.getContext(), item.getCategory().getIconUri());
        holder.icon.setImageResource(iconRes);
        if (item.getPriority() != null) {
            HabitUiHelper.stylePriorityChip(holder.priority, item.getPriority().getName(), item.getPriority().getColor());
            holder.priority.setVisibility(View.VISIBLE);
        } else {
            holder.priority.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView meta;
        final TextView repeat;
        final TextView priority;

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_habit_category);
            title = itemView.findViewById(R.id.tv_habit_title);
            meta = itemView.findViewById(R.id.tv_habit_meta);
            repeat = itemView.findViewById(R.id.tv_habit_repeat);
            priority = itemView.findViewById(R.id.tv_habit_priority);
        }
    }
}
