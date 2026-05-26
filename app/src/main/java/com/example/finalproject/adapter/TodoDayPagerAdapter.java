package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.TodoRepository;

import java.time.LocalDate;
import java.util.List;

public class TodoDayPagerAdapter extends RecyclerView.Adapter<TodoDayPagerAdapter.TodoDayPageViewHolder> {
    private final TodoRepository todoRepository;
    private final TodoListAdapter.Listener todoListener;
    private LocalDate centerDate = LocalDate.now();
    private int filter = TodoRepository.FILTER_ALL;

    public TodoDayPagerAdapter(TodoRepository todoRepository, TodoListAdapter.Listener todoListener) {
        this.todoRepository = todoRepository;
        this.todoListener = todoListener;
    }

    public void setCenterDate(@NonNull LocalDate date) {
        if (centerDate.equals(date)) {
            return;
        }
        centerDate = date;
        notifyDataSetChanged();
    }

    public void setFilter(int filter) {
        if (this.filter == filter) {
            return;
        }
        this.filter = filter;
        notifyDataSetChanged();
    }

    public void updateState(@NonNull LocalDate date, int filter) {
        boolean dateChanged = !centerDate.equals(date);
        boolean filterChanged = this.filter != filter;
        if (!dateChanged && !filterChanged) {
            return;
        }
        centerDate = date;
        this.filter = filter;
        notifyDataSetChanged();
    }

    public LocalDate getDateForPosition(int position) {
        return centerDate.plusDays(position - 1L);
    }

    @NonNull
    @Override
    public TodoDayPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo_day_page, parent, false);
        return new TodoDayPageViewHolder(view, todoListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoDayPageViewHolder holder, int position) {
        holder.bind(getDateForPosition(position), filter, todoRepository);
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    static class TodoDayPageViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView recyclerView;
        private final TextView emptyView;
        private final TodoListAdapter todoAdapter;

        TodoDayPageViewHolder(@NonNull View itemView, TodoListAdapter.Listener todoListener) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.todo_page_recycler);
            emptyView = itemView.findViewById(R.id.tv_todo_page_empty);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerView.setItemAnimator(null);
            todoAdapter = new TodoListAdapter(todoListener);
            recyclerView.setAdapter(todoAdapter);
        }

        void bind(LocalDate date, int filter, TodoRepository todoRepository) {
            List<TodoItem> items = todoRepository.getTodos(date, filter);
            todoAdapter.submit(items);
            recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
