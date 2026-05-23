package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.model.CalendarEvent;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.CalendarRepository;
import com.example.finalproject.repository.TodoRepository;

import java.time.LocalDate;
import java.util.List;

public class DayPagerAdapter extends RecyclerView.Adapter<DayPagerAdapter.DayPageViewHolder> {
    private final CalendarRepository calendarRepository;
    private final TodoRepository todoRepository;
    private final EventListAdapter.Listener eventListener;
    private final TodoListAdapter.Listener todoListener;
    private LocalDate centerDate = LocalDate.now();

    public DayPagerAdapter(
            CalendarRepository calendarRepository,
            TodoRepository todoRepository,
            EventListAdapter.Listener eventListener,
            TodoListAdapter.Listener todoListener
    ) {
        this.calendarRepository = calendarRepository;
        this.todoRepository = todoRepository;
        this.eventListener = eventListener;
        this.todoListener = todoListener;
    }

    public void setCenterDate(@NonNull LocalDate date) {
        centerDate = date;
        notifyDataSetChanged();
    }

    public LocalDate getDateForPosition(int position) {
        return centerDate.plusDays(position - 1L);
    }

    @NonNull
    @Override
    public DayPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_page, parent, false);
        return new DayPageViewHolder(view, eventListener, todoListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DayPageViewHolder holder, int position) {
        holder.bind(getDateForPosition(position), calendarRepository, todoRepository);
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    static class DayPageViewHolder extends RecyclerView.ViewHolder {
        private final NestedScrollView scrollView;
        private final EventListAdapter eventAdapter;
        private final TodoListAdapter todoAdapter;

        DayPageViewHolder(
                @NonNull View itemView,
                EventListAdapter.Listener eventListener,
                TodoListAdapter.Listener todoListener
        ) {
            super(itemView);
            scrollView = itemView.findViewById(R.id.day_page_scroll);

            RecyclerView dayEventRecycler = itemView.findViewById(R.id.day_page_event_recycler);
            dayEventRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            dayEventRecycler.setNestedScrollingEnabled(false);
            eventAdapter = new EventListAdapter(eventListener);
            dayEventRecycler.setAdapter(eventAdapter);

            RecyclerView dayTodoRecycler = itemView.findViewById(R.id.day_page_todo_recycler);
            dayTodoRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            dayTodoRecycler.setNestedScrollingEnabled(false);
            todoAdapter = new TodoListAdapter(todoListener);
            dayTodoRecycler.setAdapter(todoAdapter);
        }

        void bind(LocalDate date, CalendarRepository calendarRepository, TodoRepository todoRepository) {
            List<CalendarEvent> events = calendarRepository.getEventsForDate(date);
            List<TodoItem> todos = todoRepository.getTodosForDate(date);
            eventAdapter.submit(events);
            todoAdapter.submit(todos);
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }
}
