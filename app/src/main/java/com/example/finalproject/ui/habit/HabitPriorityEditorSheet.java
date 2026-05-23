package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitPriority;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.util.HabitDefaults;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class HabitPriorityEditorSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "habit_priority_editor_result";
    public static final String RESULT_PRIORITY_ID = "priority_id";
    private static final String ARG_PRIORITY_ID = "priority_id";

    private HabitRepository repository;
    private long priorityId;
    private String selectedColor = HabitDefaults.COLOR_OPTIONS[0];
    private EditText nameEdit;
    private LinearLayout colorContainer;

    public static HabitPriorityEditorSheet newInstance(long priorityId) {
        HabitPriorityEditorSheet sheet = new HabitPriorityEditorSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PRIORITY_ID, priorityId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_habit_priority_edit, container, false);
        repository = new HabitRepository(requireContext());
        priorityId = requireArguments().getLong(ARG_PRIORITY_ID);
        nameEdit = view.findViewById(R.id.edit_priority_name);
        colorContainer = view.findViewById(R.id.layout_priority_colors);
        view.findViewById(R.id.btn_close_priority_editor).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save_priority_editor).setOnClickListener(v -> savePriority());
        loadExistingPriority();
        refreshColors();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        HabitUiHelper.styleSheetDialog(getDialog());
    }

    private void loadExistingPriority() {
        if (priorityId <= 0) {
            return;
        }
        HabitPriority priority = repository.getPriority(priorityId);
        if (priority == null) {
            return;
        }
        nameEdit.setText(priority.getName());
        if (!TextUtils.isEmpty(priority.getColor())) {
            selectedColor = priority.getColor();
        }
    }

    private void refreshColors() {
        HabitUiHelper.populateColorDots(requireContext(), colorContainer, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
            selectedColor = color;
            refreshColors();
        });
    }

    private void savePriority() {
        String name = nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_title_error, Toast.LENGTH_SHORT).show();
            return;
        }
        HabitPriority priority = new HabitPriority();
        priority.setId(priorityId);
        priority.setName(name);
        priority.setColor(selectedColor);
        if (priorityId > 0) {
            HabitPriority existing = repository.getPriority(priorityId);
            priority.setPriorityOrder(existing == null ? 0 : existing.getPriorityOrder());
        }
        long savedId = repository.savePriority(priority);
        Bundle result = new Bundle();
        result.putLong(RESULT_PRIORITY_ID, savedId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }
}
