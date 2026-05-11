package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        int fallback = holder.itemView.getContext().getColor(R.color.surface_soft);
        holder.card.setBackground(UiUtils.rounded(UiUtils.safeColor(item.getColor(), fallback), 8, holder.itemView.getContext()));
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        holder.description.setVisibility(item.getDescription() == null || item.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isCompleted());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggle(item, isChecked));
        holder.edit.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.delete.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.card.setOnClickListener(v -> listener.onClick(item));
        holder.edit.setOnClickListener(v -> listener.onEdit(item));
        holder.delete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final CheckBox checkBox;
        final TextView title;
        final TextView description;
        final ImageButton edit;
        final ImageButton delete;

        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.todo_card);
            checkBox = itemView.findViewById(R.id.check_completed);
            title = itemView.findViewById(R.id.tv_todo_title);
            description = itemView.findViewById(R.id.tv_todo_description);
            edit = itemView.findViewById(R.id.btn_todo_edit);
            delete = itemView.findViewById(R.id.btn_todo_delete);
        }
    }
}
