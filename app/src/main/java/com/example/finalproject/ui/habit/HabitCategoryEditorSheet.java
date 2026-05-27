package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject.R;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.util.HabitDefaults;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class HabitCategoryEditorSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "habit_category_editor_result";
    public static final String RESULT_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_ID = "category_id";

    private HabitRepository repository;
    private long categoryId;
    private String selectedColor = HabitDefaults.COLOR_OPTIONS[0];
    private String selectedSticker = HabitDefaults.STICKER_OPTIONS[0];
    private EditText nameEdit;
    private LinearLayout colorContainer;
    private LinearLayout customColorTrigger;
    private GridLayout stickerGrid;

    public static HabitCategoryEditorSheet newInstance(long categoryId) {
        HabitCategoryEditorSheet sheet = new HabitCategoryEditorSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, categoryId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_habit_category_edit, container, false);
        repository = new HabitRepository(requireContext());
        categoryId = requireArguments().getLong(ARG_CATEGORY_ID);
        nameEdit = view.findViewById(R.id.edit_category_name);
        colorContainer = view.findViewById(R.id.layout_category_colors);
        customColorTrigger = view.findViewById(R.id.custom_category_color_trigger);
        stickerGrid = view.findViewById(R.id.grid_category_stickers);
        view.findViewById(R.id.btn_close_category_editor).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save_category_editor).setOnClickListener(v -> saveCategory());
        loadExistingCategory();
        refreshColors();
        refreshStickers();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        HabitUiHelper.styleSheetDialog(getDialog());
    }

    private void loadExistingCategory() {
        if (categoryId <= 0) {
            return;
        }
        HabitCategory category = repository.getCategory(categoryId);
        if (category == null) {
            return;
        }
        nameEdit.setText(category.getName());
        if (!TextUtils.isEmpty(category.getColor())) {
            selectedColor = category.getColor();
        }
        if (!TextUtils.isEmpty(category.getIconUri())) {
            selectedSticker = category.getIconUri();
        }
    }

    private void refreshStickers() {
        stickerGrid.removeAllViews();
        int spacing = UiUtils.dp(requireContext(), 8);
        int cellHeight = UiUtils.dp(requireContext(), 92);
        for (int index = 0; index < HabitDefaults.STICKER_OPTIONS.length; index++) {
            String sticker = HabitDefaults.STICKER_OPTIONS[index];
            FrameLayout cell = new FrameLayout(requireContext());
            int resId = HabitDefaults.resolveIconRes(requireContext(), sticker);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / 4),
                    GridLayout.spec(index % 4, 1f)
            );
            params.width = 0;
            params.height = cellHeight;
            params.setMargins(spacing / 2, spacing / 2, spacing / 2, spacing / 2);
            cell.setLayoutParams(params);
            HabitUiHelper.bindStickerCell(requireContext(), cell, resId, sticker.equals(selectedSticker));
            cell.setOnClickListener(v -> {
                selectedSticker = sticker;
                refreshStickers();
            });
            stickerGrid.addView(cell);
        }
    }

    private void refreshColors() {
        HabitUiHelper.populateColorDots(requireContext(), colorContainer, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
            selectedColor = color;
            refreshColors();
        });
        HabitUiHelper.bindCustomColorTrigger(requireContext(), customColorTrigger, HabitDefaults.COLOR_OPTIONS, selectedColor, color -> {
            selectedColor = color;
            refreshColors();
        });
    }

    private void saveCategory() {
        String name = nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_title_error, Toast.LENGTH_SHORT).show();
            return;
        }
        HabitCategory category = new HabitCategory();
        category.setId(categoryId);
        category.setName(name);
        category.setColor(selectedColor);
        category.setIconUri(selectedSticker);
        long savedId = repository.saveCategory(category);
        Bundle result = new Bundle();
        result.putLong(RESULT_CATEGORY_ID, savedId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }
}
