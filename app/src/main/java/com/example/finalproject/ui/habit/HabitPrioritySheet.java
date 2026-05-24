package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitPriority;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HabitPrioritySheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "habit_priority_picker_result";
    public static final String RESULT_PRIORITY_ID = "priority_id";
    private static final String ARG_SELECTED_ID = "selected_id";
    private static final int PRIORITY_CHIP_WIDTH_DP = 168;

    private HabitRepository repository;
    private long selectedPriorityId;
    private LinearLayout itemsContainer;

    public static HabitPrioritySheet newInstance(long selectedPriorityId) {
        HabitPrioritySheet sheet = new HabitPrioritySheet();
        Bundle args = new Bundle();
        args.putLong(ARG_SELECTED_ID, selectedPriorityId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_habit_priority_picker, container, false);
        repository = new HabitRepository(requireContext());
        selectedPriorityId = requireArguments().getLong(ARG_SELECTED_ID);
        itemsContainer = view.findViewById(R.id.layout_priority_items);
        ImageButton close = view.findViewById(R.id.btn_close_priority_sheet);
        close.setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_add_priority).setOnClickListener(v ->
                HabitPriorityEditorSheet.newInstance(0).show(getParentFragmentManager(), "HabitPriorityEditor"));

        getParentFragmentManager().setFragmentResultListener(HabitPriorityEditorSheet.RESULT_KEY, this, (requestKey, result) -> {
            selectedPriorityId = result.getLong(HabitPriorityEditorSheet.RESULT_PRIORITY_ID, selectedPriorityId);
            publishSelection(selectedPriorityId);
        });

        renderPriorities();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        HabitUiHelper.styleSheetDialog(getDialog());
    }

    private void renderPriorities() {
        itemsContainer.removeAllViews();
        List<HabitPriority> priorities = repository.getPriorities();
        if (priorities.isEmpty()) {
            selectedPriorityId = 0;
            return;
        }

        boolean hasSelectedPriority = false;
        for (HabitPriority priority : priorities) {
            if (priority.getId() == selectedPriorityId) {
                hasSelectedPriority = true;
                break;
            }
        }
        if (!hasSelectedPriority) {
            selectedPriorityId = priorities.get(0).getId();
        }

        for (HabitPriority priority : priorities) {
            itemsContainer.addView(createPriorityRow(priority));
        }
    }

    private View createPriorityRow(HabitPriority priority) {
        FrameLayout row = new FrameLayout(requireContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(requireContext(), 60)
        ));

        TextView check = new TextView(requireContext());
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(
                UiUtils.dp(requireContext(), 24),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        checkParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        check.setLayoutParams(checkParams);
        check.setTextSize(18);
        check.setTextColor(requireContext().getColor(R.color.brand_orange));
        UiUtils.setCheck(check, selectedPriorityId == priority.getId());
        row.addView(check);

        TextView chip = new TextView(requireContext());
        FrameLayout.LayoutParams chipParams = new FrameLayout.LayoutParams(
                UiUtils.dp(requireContext(), PRIORITY_CHIP_WIDTH_DP),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chipParams.gravity = Gravity.CENTER;
        chip.setLayoutParams(chipParams);
        chip.setPadding(UiUtils.dp(requireContext(), 12), UiUtils.dp(requireContext(), 7), UiUtils.dp(requireContext(), 12), UiUtils.dp(requireContext(), 7));
        chip.setGravity(Gravity.CENTER);
        chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        HabitUiHelper.stylePriorityChip(chip, priority.getName(), priority.getColor());
        chip.setTextSize(16);
        row.addView(chip);

        LinearLayout actionRow = new LinearLayout(requireContext());
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actionParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        actionRow.setLayoutParams(actionParams);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        ImageView edit = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                UiUtils.dp(requireContext(), 20),
                UiUtils.dp(requireContext(), 20)
        );
        edit.setLayoutParams(iconParams);
        edit.setImageResource(R.drawable.ic_edit);
        edit.setColorFilter(requireContext().getColor(R.color.brand_orange));
        edit.setOnClickListener(v ->
                HabitPriorityEditorSheet.newInstance(priority.getId())
                        .show(getParentFragmentManager(), "HabitPriorityEditor"));
        actionRow.addView(edit);

        ImageView delete = new ImageView(requireContext());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                UiUtils.dp(requireContext(), 20),
                UiUtils.dp(requireContext(), 20)
        );
        deleteParams.setMargins(UiUtils.dp(requireContext(), 18), 0, 0, 0);
        delete.setLayoutParams(deleteParams);
        delete.setImageResource(R.drawable.ic_delete);
        delete.setColorFilter(requireContext().getColor(R.color.brand_orange));
        delete.setOnClickListener(v -> UiUtils.showDeleteDialog(
                requireContext(),
                getString(R.string.habit_delete_priority_confirm),
                () -> {
                    repository.deletePriority(priority.getId());
                    if (selectedPriorityId == priority.getId()) {
                        selectedPriorityId = 0;
                    }
                    renderPriorities();
                }
        ));
        actionRow.addView(delete);
        row.addView(actionRow);

        row.setOnClickListener(v -> {
            selectedPriorityId = priority.getId();
            publishSelection(selectedPriorityId);
        });
        return row;
    }

    private void publishSelection(long priorityId) {
        selectedPriorityId = priorityId;
        Bundle result = new Bundle();
        result.putLong(RESULT_PRIORITY_ID, selectedPriorityId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }
}
