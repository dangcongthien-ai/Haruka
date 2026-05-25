package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.HabitDefaults;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HabitCategoryPickerSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "habit_category_picker_result";
    public static final String RESULT_CATEGORY_ID = "category_id";
    private static final String ARG_SELECTED_ID = "selected_id";

    private HabitRepository repository;
    private long selectedCategoryId;
    private LinearLayout itemsContainer;

    public static HabitCategoryPickerSheet newInstance(long selectedCategoryId) {
        HabitCategoryPickerSheet sheet = new HabitCategoryPickerSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_SELECTED_ID, selectedCategoryId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_habit_category_picker, container, false);
        repository = new HabitRepository(requireContext());
        selectedCategoryId = requireArguments().getLong(ARG_SELECTED_ID);
        itemsContainer = view.findViewById(R.id.layout_category_items);
        ImageButton close = view.findViewById(R.id.btn_close_category_picker);
        close.setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_add_category).setOnClickListener(v ->
                HabitCategoryEditorSheet.newInstance(0).show(getParentFragmentManager(), "HabitCategoryEditor"));

        getParentFragmentManager().setFragmentResultListener(HabitCategoryEditorSheet.RESULT_KEY, this, (requestKey, result) -> {
            selectedCategoryId = result.getLong(HabitCategoryEditorSheet.RESULT_CATEGORY_ID, selectedCategoryId);
            publishSelection(selectedCategoryId);
        });

        renderCategories();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        HabitUiHelper.styleSheetDialog(getDialog());
    }

    private void renderCategories() {
        itemsContainer.removeAllViews();
        List<HabitCategory> categories = repository.getCategories();
        if (categories.isEmpty()) {
            selectedCategoryId = 0;
            return;
        }

        boolean hasSelectedCategory = false;
        for (HabitCategory category : categories) {
            if (category.getId() == selectedCategoryId) {
                hasSelectedCategory = true;
                break;
            }
        }
        if (!hasSelectedCategory) {
            selectedCategoryId = categories.get(0).getId();
        }

        for (HabitCategory category : categories) {
            itemsContainer.addView(createCategoryRow(category));
        }
    }

    private View createCategoryRow(HabitCategory category) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiUtils.dp(requireContext(), 52)
        ));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackground((selectedCategoryId == category.getId())
                ? UiUtils.rounded(requireContext().getColor(R.color.brand_orange_light), 14, requireContext())
                : null);

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                UiUtils.dp(requireContext(), 34),
                UiUtils.dp(requireContext(), 34)
        );
        icon.setLayoutParams(iconParams);
        icon.setImageResource(HabitDefaults.resolveIconRes(requireContext(), category.getIconUri()));
        row.addView(icon);

        TextView label = new TextView(requireContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        labelParams.setMargins(UiUtils.dp(requireContext(), 14), 0, 0, 0);
        label.setLayoutParams(labelParams);
        label.setText(category.getName());
        label.setTextColor(requireContext().getColor(R.color.text_primary));
        label.setTextSize(16);
        row.addView(label);

        TextView check = new TextView(requireContext());
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                UiUtils.dp(requireContext(), 26),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        checkParams.setMargins(0, 0, UiUtils.dp(requireContext(), 10), 0);
        check.setLayoutParams(checkParams);
        check.setGravity(android.view.Gravity.CENTER);
        check.setTextSize(18);
        check.setTextColor(requireContext().getColor(R.color.text_primary));
        UiUtils.setCheck(check, selectedCategoryId == category.getId());
        row.addView(check);

        ImageView edit = new ImageView(requireContext());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                UiUtils.dp(requireContext(), 20),
                UiUtils.dp(requireContext(), 20)
        );
        edit.setLayoutParams(editParams);
        edit.setImageResource(R.drawable.ic_edit);
        edit.setColorFilter(requireContext().getColor(R.color.brand_orange));
        edit.setOnClickListener(v ->
                HabitCategoryEditorSheet.newInstance(category.getId())
                        .show(getParentFragmentManager(), "HabitCategoryEditor"));
        row.addView(edit);

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
                getString(R.string.habit_delete_category_confirm),
                () -> {
                    repository.deleteCategory(category.getId());
                    if (selectedCategoryId == category.getId()) {
                        selectedCategoryId = 0;
                    }
                    renderCategories();
                }
        ));
        row.addView(delete);

        row.setOnClickListener(v -> {
            selectedCategoryId = category.getId();
            publishSelection(selectedCategoryId);
        });
        return row;
    }

    private void publishSelection(long categoryId) {
        selectedCategoryId = categoryId;
        Bundle result = new Bundle();
        result.putLong(RESULT_CATEGORY_ID, selectedCategoryId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }
}
