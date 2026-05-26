package com.example.finalproject.ui.todo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.TodoItem;
import com.example.finalproject.repository.TodoRepository;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;

public class TodoDetailFragment extends Fragment implements ScreenBackHandler {
    private static final String ARG_TODO_ID = "todo_id";

    private TodoRepository repository;
    private long todoId;
    private TodoItem item;
    private TextView title;
    private TextView summary;
    private TextView description;
    private View notesSection;
    private TextView date;
    private TextView reminder;
    private TodoPriorityMatrixController priorityMatrixController;

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
        bind(view);
        setupClicks(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshContent();
        }
    }

    private void refreshContent() {
        if (!isAdded() || getView() == null) {
            return;
        }
        loadTodo();
        render();
    }

    private void bind(View view) {
        title = view.findViewById(R.id.tv_detail_title);
        summary = view.findViewById(R.id.tv_detail_summary);
        description = view.findViewById(R.id.tv_detail_description);
        notesSection = view.findViewById(R.id.todo_detail_notes_section);
        date = view.findViewById(R.id.tv_detail_date);
        reminder = view.findViewById(R.id.tv_detail_reminder);
        priorityMatrixController = new TodoPriorityMatrixController(view.findViewById(R.id.priority_matrix));
        priorityMatrixController.setInteractive(false);
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        view.findViewById(R.id.btn_edit_todo).setOnClickListener(v -> {
            if (item != null) {
                ((MainActivity) requireActivity()).pushFullScreenFragment(
                        TodoEditFragment.newInstance(item.getId(), item.getTodoDate()),
                        TodoEditFragment.class.getSimpleName(),
                        false
                );
            }
        });
        view.findViewById(R.id.btn_delete_todo).setOnClickListener(v -> {
            if (item == null) {
                return;
            }
            UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_todo), () -> {
                repository.deleteTodo(item.getId());
                ((MainActivity) requireActivity()).finishFullScreenOrHome();
            });
        });
    }

    @Override
    public boolean onHandleBackPressed() {
        ((MainActivity) requireActivity()).finishFullScreenOrHome();
        return true;
    }

    private void loadTodo() {
        item = repository.getTodo(todoId);
        if (item == null && isAdded()) {
            ((MainActivity) requireActivity()).finishFullScreenOrHome();
        }
    }

    private void render() {
        if (!isAdded() || item == null) {
            title.setText("");
            summary.setText("");
            description.setText("");
            date.setText(R.string.none);
            reminder.setText(R.string.none);
            return;
        }
        title.setText(item.getTitle());
        summary.setText(DateTimeUtils.formatDateWithDow(item.getTodoDate())
                + "  ·  "
                + getString(item.isCompleted() ? R.string.completed : R.string.active));
        description.setText(emptyToNone(item.getDescription()));
        UiUtils.visible(notesSection, !isEmpty(item.getDescription()));
        date.setText(DateTimeUtils.formatDateWithDow(item.getTodoDate()));

        if (item.getReminders().isEmpty()) {
            reminder.setText(R.string.none);
            reminder.setTextColor(requireContext().getColor(R.color.text_muted));
        } else if (item.getReminders().size() == 1) {
            reminder.setText(item.getReminders().get(0).getDisplayText());
            reminder.setTextColor(requireContext().getColor(R.color.text_primary));
        } else {
            reminder.setText(getString(
                    R.string.reminder_count_compact,
                    item.getReminders().size() - 1,
                    item.getReminders().get(0).getDisplayText()
            ));
            reminder.setTextColor(requireContext().getColor(R.color.text_primary));
        }
        priorityMatrixController.setPriority(item.getPriorityQuadrant());
    }

    private String emptyToNone(String value) {
        return isEmpty(value) ? getString(R.string.none) : value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
