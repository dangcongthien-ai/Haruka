package com.example.finalproject.ui.todo;

import android.os.Bundle;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.TodoRepository;

public class TodoDetailFragment extends Fragment {
    private static final String ARG_TODO_ID = "todo_id";

    private TodoRepository repository;
    private long todoId;
    private TodoItem item;
    private TextView title;
    private TextView description;
    private TextView reminder;
    private TextView priority1;
    private TextView priority2;
    private TextView priority3;
    private TextView priority4;

    public static TodoDetailFragment newInstance(long todoId) {
        TodoDetailFragment fragment = new TodoDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TODO_ID, todoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo_detail, container, false);
        repository = new TodoRepository(requireContext());
        todoId = requireArguments().getLong(ARG_TODO_ID);
        item = repository.getTodo(todoId);
        bind(view);
        setupClicks(view);
        render();
        return view;
    }

    private void bind(View view) {
        title = view.findViewById(R.id.tv_detail_title);
        description = view.findViewById(R.id.tv_detail_description);
        reminder = view.findViewById(R.id.tv_detail_reminder);
        priority1 = view.findViewById(R.id.priority_1);
        priority2 = view.findViewById(R.id.priority_2);
        priority3 = view.findViewById(R.id.priority_3);
        priority4 = view.findViewById(R.id.priority_4);
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).finishToHome());
        view.findViewById(R.id.btn_edit_todo).setOnClickListener(v -> {
            if (item != null) {
                ((MainActivity) requireActivity()).switchFullScreen(TodoEditFragment.newInstance(item.getId(), item.getTodoDate()));
            }
        });
    }

    private void render() {
        if (item == null) {
            title.setText("");
            description.setText("");
            reminder.setText(R.string.none);
            return;
        }
        title.setText(item.getTitle());
        description.setText(item.getDescription());
        Reminder itemReminder = item.getReminder() == null ? Reminder.none() : item.getReminder();
        reminder.setText(itemReminder.getDisplayText() + " ›");
        int priority = item.getPriorityQuadrant();
        stylePriority(priority1, priority == 1);
        stylePriority(priority2, priority == 2);
        stylePriority(priority3, priority == 3);
        stylePriority(priority4, priority == 4);
        priority1.setClickable(false);
        priority2.setClickable(false);
        priority3.setClickable(false);
        priority4.setClickable(false);
    }

    private void stylePriority(TextView view, boolean selected) {
        view.setBackground(selected
                ? com.example.finalproject.ui.common.UiUtils.roundedStroke(Color.TRANSPARENT, requireContext().getColor(R.color.brand_orange), 12, requireContext())
                : null);
    }
}
