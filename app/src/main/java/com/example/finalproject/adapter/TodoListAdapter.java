package com.example.finalproject.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.ui.common.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class TodoListAdapter extends RecyclerView.Adapter<TodoListAdapter.TodoViewHolder> {
    public interface Listener {
        void onClick(TodoItem item);

        void onToggle(TodoItem item, boolean completed);

        void onEdit(TodoItem item);

        void onDelete(TodoItem item);
    }

    private final List<TodoItem> items = new ArrayList<>();
    private final Listener listener;
    private boolean showActions = true;

    public TodoListAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<TodoItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setShowActions(boolean showActions) {
        this.showActions = showActions;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        TodoItem item = items.get(position);
        Context context = holder.itemView.getContext();
        int accent = priorityAccent(item.getPriorityQuadrant(), context);
        int fill = ColorUtils.setAlphaComponent(accent, item.isCompleted() ? 22 : 28);
        holder.card.setBackground(UiUtils.rounded(fill, 10, context));
        holder.title.setText(item.getTitle());
        holder.title.setTextColor(context.getColor(R.color.text_primary));
        holder.description.setText(item.getDescription());
        holder.description.setTextColor(context.getColor(R.color.text_secondary));
        holder.description.setVisibility(item.getDescription() == null || item.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        holder.checkIcon.setImageResource(item.isCompleted() ? R.drawable.ic_todo_check_done : R.drawable.ic_todo_check_empty);
        holder.toggle.setContentDescription(context.getString(item.isCompleted() ? R.string.completed : R.string.active));
        UiUtils.setDebouncedClickListener(holder.toggle, () -> listener.onToggle(item, !item.isCompleted()));
        holder.edit.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.delete.setVisibility(showActions ? View.VISIBLE : View.GONE);
        UiUtils.setDebouncedClickListener(holder.card, () -> listener.onClick(item));
        UiUtils.setDebouncedClickListener(holder.edit, () -> listener.onEdit(item));
        UiUtils.setDebouncedClickListener(holder.delete, () -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    private int priorityAccent(int priority, Context context) {
        if (priority == 1) {
            return context.getColor(R.color.priority_1_accent);
        }
        if (priority == 2) {
            return context.getColor(R.color.priority_2_accent);
        }
        if (priority == 3) {
            return context.getColor(R.color.priority_3_accent);
        }
        if (priority == 4) {
            return context.getColor(R.color.priority_4_accent);
        }
        return Color.parseColor("#EB9B54");
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final View toggle;
        final ImageView checkIcon;
        final TextView title;
        final TextView description;
        final ImageButton edit;
        final ImageButton delete;

        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.todo_card);
            toggle = itemView.findViewById(R.id.btn_toggle_completed);
            checkIcon = itemView.findViewById(R.id.iv_completed);
            title = itemView.findViewById(R.id.tv_todo_title);
            description = itemView.findViewById(R.id.tv_todo_description);
            edit = itemView.findViewById(R.id.btn_todo_edit);
            delete = itemView.findViewById(R.id.btn_todo_delete);
        }
    }
}
