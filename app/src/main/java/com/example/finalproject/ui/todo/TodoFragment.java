package com.example.finalproject.ui.todo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.TodoListAdapter;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.TodoRepository;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.List;

public class TodoFragment extends Fragment {
    private static final String ARG_DATE = "date";
    private static final String DATE_RESULT_KEY = "todo_date_result";

    private TodoRepository repository;
    private TodoListAdapter adapter;
    private TextView dateLabel;
    private TextView emptyLabel;
    private RecyclerView recyclerView;
    private MaterialButton allButton;
    private MaterialButton activeButton;
    private MaterialButton doneButton;
    private LocalDate selectedDate;
    private int filter = TodoRepository.FILTER_ALL;

    public static TodoFragment newInstance(LocalDate date) {
        TodoFragment fragment = new TodoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);
        repository = new TodoRepository(requireContext());
        selectedDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        bind(view);
        setupAdapter();
        setupClicks(view);
        setupDateResult();
        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void bind(View view) {
        dateLabel = view.findViewById(R.id.tv_todo_date);
        emptyLabel = view.findViewById(R.id.tv_todo_empty);
        recyclerView = view.findViewById(R.id.todo_recycler);
        allButton = view.findViewById(R.id.btn_filter_all);
        activeButton = view.findViewById(R.id.btn_filter_active);
        doneButton = view.findViewById(R.id.btn_filter_done);
    }

    private void setupAdapter() {
        adapter = new TodoListAdapter(new TodoListAdapter.Listener() {
            @Override
            public void onClick(TodoItem item) {
                ((MainActivity) requireActivity()).openTodoDetail(item.getId());
            }

            @Override
            public void onToggle(TodoItem item, boolean completed) {
                repository.setCompleted(item.getId(), completed);
                refresh();
            }

            @Override
            public void onEdit(TodoItem item) {
                ((MainActivity) requireActivity()).openTodoEditor(item.getId(), item.getTodoDate());
            }

            @Override
            public void onDelete(TodoItem item) {
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_todo), () -> {
                    repository.deleteTodo(item.getId());
                    refresh();
                });
            }
        });
        adapter.setShowActions(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupClicks(View view) {
        ImageButton previous = view.findViewById(R.id.btn_todo_prev_day);
        ImageButton next = view.findViewById(R.id.btn_todo_next_day);
        previous.setOnClickListener(v -> moveDay(-1));
        next.setOnClickListener(v -> moveDay(1));
        dateLabel.setOnClickListener(v -> DatePickerDialogFragment
                .newInstance(DATE_RESULT_KEY, selectedDate)
                .show(getParentFragmentManager(), DATE_RESULT_KEY));
        allButton.setOnClickListener(v -> {
            filter = TodoRepository.FILTER_ALL;
            refresh();
        });
        activeButton.setOnClickListener(v -> {
            filter = TodoRepository.FILTER_ACTIVE;
            refresh();
        });
        doneButton.setOnClickListener(v -> {
            filter = TodoRepository.FILTER_DONE;
            refresh();
        });
    }

    private void setupDateResult() {
        getParentFragmentManager().setFragmentResultListener(DATE_RESULT_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                selectedDate = picked;
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
                refresh();
            }
        });
    }

    private void moveDay(int amount) {
        selectedDate = selectedDate.plusDays(amount);
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        refresh();
    }

    private void refresh() {
        if (!isAdded()) {
            return;
        }
        dateLabel.setText(DateTimeUtils.formatVietnameseDate(selectedDate));
        if (filter == TodoRepository.FILTER_ALL) {
            UiUtils.selectSegment(requireContext(), allButton, activeButton, doneButton);
        } else if (filter == TodoRepository.FILTER_ACTIVE) {
            UiUtils.selectSegment(requireContext(), activeButton, allButton, doneButton);
        } else {
            UiUtils.selectSegment(requireContext(), doneButton, allButton, activeButton);
        }
        List<TodoItem> items = repository.getTodos(selectedDate, filter);
        adapter.submit(items);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        emptyLabel.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
