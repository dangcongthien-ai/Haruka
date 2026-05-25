package com.example.finalproject.ui.journal;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.JournalEntry;
import com.example.finalproject.repository.JournalRepository;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.ScreenBackHandler;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.JournalTextUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalEditFragment extends Fragment implements ScreenBackHandler {
    private static final String ARG_JOURNAL_ID = "journal_id";
    private static final String ARG_DATE = "date";
    private static final String RESULT_DATE = "journal_edit_date";
    private static final String STATE_INITIAL_MOOD_PICKER_SHOWN = "initial_mood_picker_shown";
    private static final String MOOD_BUON_NGU = "journal_emo_buon_ngu";
    private static final int MAX_IMAGE_COUNT = 3;
    private static final long LAYOUT_GINGHAM = 1L;
    private static final long LAYOUT_DENIM = 2L;
    private static final long LAYOUT_PLAID = 3L;
    private static final long[] JOURNAL_LAYOUTS = {LAYOUT_GINGHAM, LAYOUT_DENIM, LAYOUT_PLAID};
    private static final int MOOD_SLOT_WEATHER = 0;
    private static final int MOOD_SLOT_DAY = 1;
    private static final int MOOD_SLOT_BODY = 2;
    private static final int DEFAULT_CAPTION_TEXT_SIZE_SP = 25;
    private static final int DEFAULT_CONTENT_TEXT_SIZE_SP = 20;
    private static final int MIN_TEXT_SIZE_SP = 12;
    private static final int MAX_TEXT_SIZE_SP = 34;
    private static final int TEXT_SIZE_STEP_SP = 2;

    private JournalRepository repository;
    private long journalId;
    private boolean initialized;
    private boolean suppressAutoSave;
    private boolean shouldAutoShowMoodPicker;
    private boolean initialMoodPickerShown;
    private LocalDate journalDate;
    private String title = "";
    private String caption = "";
    private String content = "";
    private long selectedLayoutId = LAYOUT_GINGHAM;
    private Layout.Alignment captionAlignment = Layout.Alignment.ALIGN_NORMAL;
    private int captionTextSizeSp = DEFAULT_CAPTION_TEXT_SIZE_SP;
    private int contentTextSizeSp = DEFAULT_CONTENT_TEXT_SIZE_SP;
    private final List<String> imageUris = new ArrayList<>();
    private int selectedImageSlot = -1;
    private int lastSelectionStart = -1;
    private int lastSelectionEnd = -1;
    private ActivityResultLauncher<String[]> imagePickerLauncher;

    private EditText captionEdit;
    private EditText contentEdit;
    private EditText activeTextEdit;
    private EditText lastSelectionEdit;
    private TextView dateText;
    private ImageButton deleteButton;
    private ImageView patternBackground;
    private ConstraintLayout journalPage;
    private LinearLayout moodRow;
    private ImageButton[] moodButtons;
    private ConstraintLayout photoRow;
    private View formatToolbar;
    private FrameLayout[] photoSlots;
    private ImageView[] photoViews;
    private ImageView[] photoAddViews;
    private ImageButton[] photoRemoveButtons;
    private final int[] selectedMoodResources = {
            R.drawable.journal_emo_nang,
            R.drawable.journal_emo_vui,
            R.drawable.journal_emo_sang_khoai
    };

    private static class MoodOption {
        final int drawableRes;
        final int labelRes;
        final int slotIndex;

        MoodOption(int drawableRes, int labelRes, int slotIndex) {
            this.drawableRes = drawableRes;
            this.labelRes = labelRes;
            this.slotIndex = slotIndex;
        }
    }

    public static JournalEditFragment newInstance(long journalId, LocalDate date) {
        JournalEditFragment fragment = new JournalEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_JOURNAL_ID, journalId);
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialMoodPickerShown = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_INITIAL_MOOD_PICKER_SHOWN, false);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                handlePickedImage(uri);
            }
        });
        getParentFragmentManager().setFragmentResultListener(RESULT_DATE, this, (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                journalDate = picked;
                ((MainActivity) requireActivity()).setSelectedDate(journalDate);
                bindValues();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal_edit, container, false);
        repository = new JournalRepository(requireContext());
        bind(view);
        if (!initialized) {
            initializeFields();
        }
        setupClicks(view);
        setupSystemInsets(view);
        bindValues();
        maybeShowInitialMoodPicker(view);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_INITIAL_MOOD_PICKER_SHOWN, initialMoodPickerShown);
    }

    @Override
    public void onStop() {
        if (!suppressAutoSave && isAdded() && repository != null && captionEdit != null && contentEdit != null) {
            saveDraftIfNeeded();
        }
        super.onStop();
    }

    private void bind(View view) {
        captionEdit = view.findViewById(R.id.edit_journal_caption);
        contentEdit = view.findViewById(R.id.edit_journal_content);
        dateText = view.findViewById(R.id.tv_journal_edit_date);
        deleteButton = view.findViewById(R.id.btn_journal_delete);
        patternBackground = view.findViewById(R.id.journal_pattern_background);
        journalPage = view.findViewById(R.id.journal_page);
        moodRow = view.findViewById(R.id.journal_mood_row);
        moodButtons = new ImageButton[]{
                view.findViewById(R.id.btn_journal_weather_selection),
                view.findViewById(R.id.btn_journal_day_selection),
                view.findViewById(R.id.btn_journal_body_selection)
        };
        photoRow = view.findViewById(R.id.journal_photo_row);
        formatToolbar = view.findViewById(R.id.journal_format_toolbar);
        photoSlots = new FrameLayout[]{
                view.findViewById(R.id.journal_photo_slot_1),
                view.findViewById(R.id.journal_photo_slot_2),
                view.findViewById(R.id.journal_photo_slot_3)
        };
        photoViews = new ImageView[]{
                view.findViewById(R.id.img_journal_photo_1),
                view.findViewById(R.id.img_journal_photo_2),
                view.findViewById(R.id.img_journal_photo_3)
        };
        photoAddViews = new ImageView[]{
                view.findViewById(R.id.img_journal_photo_add_1),
                view.findViewById(R.id.img_journal_photo_add_2),
                view.findViewById(R.id.img_journal_photo_add_3)
        };
        photoRemoveButtons = new ImageButton[]{
                view.findViewById(R.id.btn_remove_journal_photo_1),
                view.findViewById(R.id.btn_remove_journal_photo_2),
                view.findViewById(R.id.btn_remove_journal_photo_3)
        };
        ensureImageListSize();
        setupTextSelectionTracking(captionEdit);
        setupTextSelectionTracking(contentEdit);
        activeTextEdit = contentEdit;
    }

    private void initializeFields() {
        journalId = requireArguments().getLong(ARG_JOURNAL_ID);
        journalDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (journalDate == null) {
            journalDate = LocalDate.now();
        }
        if (journalId > 0) {
            JournalEntry entry = repository.getJournalEntry(journalId);
            if (entry != null) {
                journalDate = entry.getJournalDate();
                title = valueOrEmpty(entry.getTitle());
                caption = valueOrEmpty(entry.getCaption());
                content = valueOrEmpty(entry.getContent());
                captionTextSizeSp = JournalTextUtils.textSizeFromStoredContent(caption, DEFAULT_CAPTION_TEXT_SIZE_SP);
                contentTextSizeSp = JournalTextUtils.textSizeFromStoredContent(content, DEFAULT_CONTENT_TEXT_SIZE_SP);
                selectedLayoutId = normalizeLayoutId(entry.getLayoutId());
                imageUris.clear();
                imageUris.addAll(entry.getImageUris());
                applyStoredMoods(entry.getMoodResourceNames());
            }
        }
        ensureImageListSize();
        shouldAutoShowMoodPicker = journalId <= 0 && !hasDraftData();
        initialized = true;
    }

    private void maybeShowInitialMoodPicker(View rootView) {
        if (!shouldAutoShowMoodPicker || initialMoodPickerShown) {
            return;
        }
        initialMoodPickerShown = true;
        rootView.post(() -> {
            if (isAdded()) {
                showMoodPicker();
            }
        });
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_journal_back).setOnClickListener(v -> ((MainActivity) requireActivity()).handleActivityBackPressed());
        view.findViewById(R.id.btn_journal_prev_day).setOnClickListener(v -> moveDay(-1));
        view.findViewById(R.id.btn_journal_next_day).setOnClickListener(v -> moveDay(1));
        view.findViewById(R.id.btn_journal_save).setOnClickListener(v -> save());
        dateText.setOnClickListener(v -> {
            captureInput();
            DatePickerDialogFragment
                    .newInstance(RESULT_DATE, journalDate)
                    .show(getParentFragmentManager(), RESULT_DATE);
        });
        deleteButton.setOnClickListener(v -> UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_journal), () -> {
            suppressAutoSave = true;
            if (journalId > 0) {
                repository.deleteJournalEntry(journalId);
            }
            ((MainActivity) requireActivity()).finishToHome();
        }));
        view.findViewById(R.id.btn_journal_camera).setOnClickListener(v -> openFirstAvailableImageSlot());
        for (int index = 0; index < MAX_IMAGE_COUNT; index++) {
            final int photoIndex = index;
            photoSlots[index].setOnClickListener(v -> {
                ensureImageListSize();
                if (imageUris.get(photoIndex).trim().isEmpty() && photoIndex == firstEmptyImageSlot()) {
                    openImagePicker(photoIndex);
                }
            });
            photoRemoveButtons[index].setOnClickListener(v -> removeImage(photoIndex));
        }
        View contentTool = view.findViewById(R.id.btn_journal_content_tool);
        prepareToolbarButton(contentTool);
        contentTool.setOnClickListener(v -> {
            rememberSelection();
            EditText target = targetEdit();
            target.requestFocus();
            if (target.getSelectionStart() < 0) {
                target.setSelection(target.getText().length());
            }
            toggleFormatToolbar();
        });
        setupFormatButton(view.findViewById(R.id.btn_journal_align_left), () ->
                applyAlignment(Layout.Alignment.ALIGN_NORMAL));
        setupFormatButton(view.findViewById(R.id.btn_journal_align_center), () ->
                applyAlignment(Layout.Alignment.ALIGN_CENTER));
        setupFormatButton(view.findViewById(R.id.btn_journal_align_right), () ->
                applyAlignment(Layout.Alignment.ALIGN_OPPOSITE));
        setupFormatButton(view.findViewById(R.id.btn_journal_bold), () -> toggleStyle(Typeface.BOLD));
        setupFormatButton(view.findViewById(R.id.btn_journal_italic), () -> toggleStyle(Typeface.ITALIC));
        setupFormatButton(view.findViewById(R.id.btn_journal_underline), this::toggleUnderline);
        setupFormatButton(view.findViewById(R.id.btn_journal_text_decrease), () ->
                adjustTextSize(-TEXT_SIZE_STEP_SP));
        setupFormatButton(view.findViewById(R.id.btn_journal_text_increase), () ->
                adjustTextSize(TEXT_SIZE_STEP_SP));
        view.findViewById(R.id.btn_journal_mood).setOnClickListener(v -> showMoodPicker());
        view.findViewById(R.id.btn_journal_layout).setOnClickListener(v -> showLayoutPicker());
        for (ImageButton moodButton : moodButtons) {
            moodButton.setOnClickListener(v -> showMoodPicker());
        }
    }

    private void showLayoutPicker() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_journal_layout_picker);

        long[] draftLayoutId = {selectedLayoutId};
        updateLayoutPreview(dialog, draftLayoutId[0]);

        ImageButton previousButton = dialog.findViewById(R.id.btn_journal_layout_prev);
        ImageButton nextButton = dialog.findViewById(R.id.btn_journal_layout_next);
        TextView saveButton = dialog.findViewById(R.id.btn_journal_layout_save);

        previousButton.setOnClickListener(v -> {
            draftLayoutId[0] = layoutAtOffset(draftLayoutId[0], -1);
            updateLayoutPreview(dialog, draftLayoutId[0]);
        });
        nextButton.setOnClickListener(v -> {
            draftLayoutId[0] = layoutAtOffset(draftLayoutId[0], 1);
            updateLayoutPreview(dialog, draftLayoutId[0]);
        });
        saveButton.setOnClickListener(v -> {
            selectedLayoutId = draftLayoutId[0];
            applyJournalLayout();
            dialog.dismiss();
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(360));
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.45f;
            window.setAttributes(params);
        }
    }

    private void showMoodPicker() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_journal_mood_picker);

        dialog.findViewById(R.id.btn_close_journal_mood_picker).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_done_journal_mood_picker).setOnClickListener(v -> dialog.dismiss());
        View[] selectedTiles = new View[3];
        populateMoodGrid(dialog.findViewById(R.id.grid_journal_weather), weatherMoodOptions(), selectedTiles);
        populateMoodGrid(dialog.findViewById(R.id.grid_journal_day_mood), dayMoodOptions(), selectedTiles);
        populateMoodGrid(dialog.findViewById(R.id.grid_journal_body_mood), bodyMoodOptions(), selectedTiles);

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.dimAmount = 0f;
            window.setAttributes(params);
        }
    }

    private void populateMoodGrid(GridLayout grid, MoodOption[] options, View[] selectedTiles) {
        if (grid == null) {
            return;
        }
        grid.removeAllViews();
        grid.setColumnCount(5);
        for (int index = 0; index < options.length; index++) {
            MoodOption option = options[index];
            LinearLayout tile = new LinearLayout(requireContext());
            tile.setGravity(Gravity.CENTER);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(2), dp(2), dp(2), dp(2));
            tile.setClickable(true);
            tile.setFocusable(true);
            android.content.res.TypedArray attrs = requireContext().obtainStyledAttributes(
                    new int[]{android.R.attr.selectableItemBackground}
            );
            tile.setForeground(attrs.getDrawable(0));
            attrs.recycle();
            tile.setContentDescription(getString(option.labelRes));

            ImageView image = new ImageView(requireContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(58), dp(58));
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageResource(option.drawableRes);
            tile.addView(image, imageParams);

            TextView label = new TextView(requireContext());
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setText(option.labelRes);
            label.setTextColor(getResources().getColor(R.color.text_primary));
            label.setTextSize(10);
            tile.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));

            GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
            tileParams.columnSpec = GridLayout.spec(index % 5, 1f);
            tileParams.width = 0;
            tileParams.height = dp(82);
            tile.setLayoutParams(tileParams);

            if (selectedMoodResources[option.slotIndex] == option.drawableRes) {
                tile.setBackgroundResource(R.drawable.bg_journal_mood_tile_selected);
                selectedTiles[option.slotIndex] = tile;
            }
            tile.setOnClickListener(v -> {
                selectedMoodResources[option.slotIndex] = option.drawableRes;
                refreshMoodRow();
                if (selectedTiles[option.slotIndex] != null) {
                    selectedTiles[option.slotIndex].setBackgroundColor(Color.TRANSPARENT);
                }
                v.setBackgroundResource(R.drawable.bg_journal_mood_tile_selected);
                selectedTiles[option.slotIndex] = v;
            });
            grid.addView(tile);
        }
    }

    private MoodOption[] weatherMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_nang, R.string.journal_emo_nang, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_mua, R.string.journal_emo_mua, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_am_u, R.string.journal_emo_am_u, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_nhieu_may, R.string.journal_emo_nhieu_may, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_tuyet, R.string.journal_emo_tuyet, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_gio, R.string.journal_emo_gio, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_bao, R.string.journal_emo_bao, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_nong, R.string.journal_emo_nong, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_sam, R.string.journal_emo_sam, MOOD_SLOT_WEATHER),
                new MoodOption(R.drawable.journal_emo_suong, R.string.journal_emo_suong, MOOD_SLOT_WEATHER)
        };
    }

    private MoodOption[] dayMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_vui, R.string.journal_emo_vui, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_tu_hao, R.string.journal_emo_tu_hao, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_phan_khich, R.string.journal_emo_phan_khich, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_binh_yen, R.string.journal_emo_binh_yen, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_binh_thuong, R.string.journal_emo_binh_thuong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_lo_lang, R.string.journal_emo_lo_lang, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_met_moi, R.string.journal_emo_met_moi, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_buon_ba, R.string.journal_emo_buon, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_chan_nan, R.string.journal_emo_chan_nan, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_tuc_gian, R.string.journal_emo_tuc_gian, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ton_thuong, R.string.journal_emo_ton_thuong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_hi_vong, R.string.journal_emo_hi_vong, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_hoai_niem, R.string.journal_emo_hoai_niem, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_nang_suat, R.string.journal_emo_nang_suat, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ban_ron, R.string.journal_emo_ban_ron, MOOD_SLOT_DAY),
                new MoodOption(R.drawable.journal_emo_ham_bik_nua, R.string.journal_emo_ham_bik_nua, MOOD_SLOT_DAY)
        };
    }

    private MoodOption[] bodyMoodOptions() {
        return new MoodOption[]{
                new MoodOption(R.drawable.journal_emo_sang_khoai, R.string.journal_emo_sang_khoai, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_om, R.string.journal_emo_om, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_buon_ngu_clean, R.string.journal_emo_buon_ngu, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_tinh_than_on, R.string.journal_emo_tinh_than_on, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_cang_thang, R.string.journal_emo_cang_thang, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_doi_bung, R.string.journal_emo_doi_bung, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_dang_hoi_phuc, R.string.journal_emo_dang_hoi_phuc, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_kiet_suc, R.string.journal_emo_kiet_suc, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_chi_muon_nam, R.string.journal_emo_chi_muon_nam, MOOD_SLOT_BODY),
                new MoodOption(R.drawable.journal_emo_thu_gian, R.string.journal_emo_thu_gian, MOOD_SLOT_BODY)
        };
    }

    private void refreshMoodRow() {
        if (moodButtons == null) {
            return;
        }
        for (int index = 0; index < moodButtons.length; index++) {
            moodButtons[index].setImageResource(selectedMoodResources[index]);
        }
    }

    private void updateLayoutPreview(Dialog dialog, long layoutId) {
        ImageView previewPattern = dialog.findViewById(R.id.img_journal_layout_preview_pattern);
        View previewPage = dialog.findViewById(R.id.view_journal_layout_preview_page);
        if (previewPattern == null || previewPage == null) {
            return;
        }
        previewPattern.setImageResource(patternResource(layoutId));
        previewPage.setBackgroundResource(previewPageResource(layoutId));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) previewPage.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        if (layoutId == LAYOUT_PLAID) {
            params.width = dp(128);
            params.height = dp(232);
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            params.setMargins(0, 0, dp(36), 0);
        } else {
            params.width = dp(142);
            params.height = dp(254);
            params.gravity = Gravity.CENTER;
        }
        previewPage.setLayoutParams(params);
    }

    private long layoutAtOffset(long currentLayoutId, int offset) {
        int currentIndex = 0;
        for (int index = 0; index < JOURNAL_LAYOUTS.length; index++) {
            if (JOURNAL_LAYOUTS[index] == currentLayoutId) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = (currentIndex + offset + JOURNAL_LAYOUTS.length) % JOURNAL_LAYOUTS.length;
        return JOURNAL_LAYOUTS[nextIndex];
    }

    private void toggleFormatToolbar() {
        formatToolbar.setVisibility(formatToolbar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void prepareToolbarButton(View button) {
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
        button.setOnTouchListener((v, event) -> {
            rememberSelection();
            return false;
        });
    }

    private void setupFormatButton(View button, Runnable action) {
        prepareToolbarButton(button);
        button.setOnClickListener(v -> {
            rememberSelection();
            action.run();
        });
    }

    private void setupTextSelectionTracking(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            activeTextEdit = editText;
            editText.post(this::rememberSelection);
            return false;
        });
        editText.setOnLongClickListener(v -> {
            activeTextEdit = editText;
            editText.postDelayed(this::rememberSelection, 150);
            return false;
        });
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeTextEdit = editText;
            }
            editText.post(this::rememberSelection);
        });
    }

    private void rememberSelection() {
        EditText editText = targetEdit();
        int start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
        int end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());
        if (isValidRange(editText, start, end)) {
            lastSelectionEdit = editText;
            lastSelectionStart = start;
            lastSelectionEnd = end;
        }
    }

    private void toggleStyle(int style) {
        EditText editText = targetEdit();
        int[] range = selectedTextRange();
        if (range == null) {
            return;
        }
        Editable editable = editText.getText();
        boolean removed = false;
        StyleSpan[] spans = editable.getSpans(range[0], range[1], StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == style && overlaps(editable, span, range[0], range[1])) {
                editable.removeSpan(span);
                removed = true;
            }
        }
        if (!removed) {
            editable.setSpan(new StyleSpan(style), range[0], range[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        editText.setSelection(range[0], range[1]);
        lastSelectionEdit = editText;
        lastSelectionStart = range[0];
        lastSelectionEnd = range[1];
    }

    private void toggleUnderline() {
        EditText editText = targetEdit();
        int[] range = selectedTextRange();
        if (range == null) {
            return;
        }
        Editable editable = editText.getText();
        boolean removed = false;
        UnderlineSpan[] spans = editable.getSpans(range[0], range[1], UnderlineSpan.class);
        for (UnderlineSpan span : spans) {
            if (overlaps(editable, span, range[0], range[1])) {
                editable.removeSpan(span);
                removed = true;
            }
        }
        if (!removed) {
            editable.setSpan(new UnderlineSpan(), range[0], range[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        editText.setSelection(range[0], range[1]);
        lastSelectionEdit = editText;
        lastSelectionStart = range[0];
        lastSelectionEnd = range[1];
    }

    private void adjustTextSize(int deltaSp) {
        EditText editText = targetEdit();
        int start = Math.max(0, Math.min(editText.getSelectionStart(), editText.getText().length()));
        int end = Math.max(0, Math.min(editText.getSelectionEnd(), editText.getText().length()));
        if (editText == captionEdit) {
            captionTextSizeSp = clampTextSize(captionTextSizeSp + deltaSp);
            captionEdit.setTextSize(captionTextSizeSp);
        } else {
            contentTextSizeSp = clampTextSize(contentTextSizeSp + deltaSp);
            contentEdit.setTextSize(contentTextSizeSp);
        }
        editText.requestFocus();
        editText.setSelection(Math.min(start, end), Math.max(start, end));
        lastSelectionEdit = editText;
        lastSelectionStart = Math.min(start, end);
        lastSelectionEnd = Math.max(start, end);
    }

    private int clampTextSize(int textSizeSp) {
        return Math.max(MIN_TEXT_SIZE_SP, Math.min(MAX_TEXT_SIZE_SP, textSizeSp));
    }

    private int[] selectedTextRange() {
        EditText editText = targetEdit();
        int start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
        int end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());
        if (isValidRange(editText, start, end)) {
            lastSelectionEdit = editText;
            lastSelectionStart = start;
            lastSelectionEnd = end;
            return new int[]{start, end};
        }
        if (lastSelectionEdit == editText && isValidRange(editText, lastSelectionStart, lastSelectionEnd)) {
            return new int[]{lastSelectionStart, lastSelectionEnd};
        }
        if (start < 0 || end <= start) {
            Toast.makeText(requireContext(), R.string.journal_format_select_text, Toast.LENGTH_SHORT).show();
            return null;
        }
        return null;
    }

    private boolean isValidRange(EditText editText, int start, int end) {
        return start >= 0 && end > start && end <= editText.getText().length();
    }

    private void applyAlignment(Layout.Alignment alignment) {
        EditText editText = targetEdit();
        if (editText == captionEdit) {
            applyCaptionAlignment(alignment);
            return;
        }
        Editable editable = editText.getText();
        if (editable.length() == 0) {
            return;
        }
        int[] range = selectedOrWholeContentRange(editable);
        int start = range[0];
        int end = range[1];
        if (!isValidRange(editText, start, end)) {
            start = 0;
            end = editable.length();
        }
        int paragraphStart = paragraphStart(editable, start);
        int paragraphEnd = paragraphEnd(editable, end);
        AlignmentSpan[] spans = editable.getSpans(paragraphStart, paragraphEnd, AlignmentSpan.class);
        for (AlignmentSpan span : spans) {
            if (overlaps(editable, span, paragraphStart, paragraphEnd)) {
                editable.removeSpan(span);
            }
        }
        editable.setSpan(
                new AlignmentSpan.Standard(alignment),
                paragraphStart,
                paragraphEnd,
                Spanned.SPAN_PARAGRAPH
        );
        editText.setSelection(start, end);
        lastSelectionEdit = editText;
        lastSelectionStart = start;
        lastSelectionEnd = end;
    }

    private void applyCaptionAlignment(Layout.Alignment alignment) {
        captionAlignment = alignment;
        int start = Math.max(0, captionEdit.getSelectionStart());
        int end = Math.max(0, captionEdit.getSelectionEnd());
        applyCaptionGravity();
        captionEdit.requestFocus();
        if (start >= 0 && end >= 0 && start <= captionEdit.length() && end <= captionEdit.length()) {
            captionEdit.setSelection(Math.min(start, end), Math.max(start, end));
        }
        lastSelectionEdit = captionEdit;
        lastSelectionStart = Math.min(start, end);
        lastSelectionEnd = Math.max(start, end);
        captionEdit.postInvalidate();
    }

    private void applyCaptionGravity() {
        int horizontalGravity;
        if (captionAlignment == Layout.Alignment.ALIGN_CENTER) {
            horizontalGravity = Gravity.CENTER_HORIZONTAL;
        } else if (captionAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
            horizontalGravity = Gravity.END;
        } else {
            horizontalGravity = Gravity.START;
        }
        captionEdit.setGravity(Gravity.CENTER_VERTICAL | horizontalGravity);
    }

    private int[] selectedOrWholeContentRange(Editable editable) {
        int[] selectedRange = selectedTextRangeWithoutToast();
        if (selectedRange != null) {
            return selectedRange;
        }
        return new int[]{0, editable.length()};
    }

    private int[] selectedTextRangeWithoutToast() {
        EditText editText = targetEdit();
        int start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
        int end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());
        if (isValidRange(editText, start, end)) {
            lastSelectionEdit = editText;
            lastSelectionStart = start;
            lastSelectionEnd = end;
            return new int[]{start, end};
        }
        if (lastSelectionEdit == editText && isValidRange(editText, lastSelectionStart, lastSelectionEnd)) {
            return new int[]{lastSelectionStart, lastSelectionEnd};
        }
        return null;
    }

    private EditText targetEdit() {
        if (activeTextEdit == captionEdit || activeTextEdit == contentEdit) {
            return activeTextEdit;
        }
        if (captionEdit.hasFocus()) {
            return captionEdit;
        }
        return contentEdit;
    }

    private int paragraphStart(Editable editable, int offset) {
        int start = Math.max(0, Math.min(offset, editable.length()));
        while (start > 0 && editable.charAt(start - 1) != '\n') {
            start--;
        }
        return start;
    }

    private int paragraphEnd(Editable editable, int offset) {
        int end = Math.max(0, Math.min(offset, editable.length()));
        while (end < editable.length() && editable.charAt(end) != '\n') {
            end++;
        }
        if (end < editable.length()) {
            end++;
        }
        return end;
    }

    private boolean overlaps(Editable editable, Object span, int start, int end) {
        return editable.getSpanStart(span) < end && editable.getSpanEnd(span) > start;
    }

    private void setupSystemInsets(View view) {
        View bottomToolbar = view.findViewById(R.id.journal_bottom_toolbar);
        int baseHeight = bottomToolbar.getLayoutParams().height;
        int basePaddingBottom = bottomToolbar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomToolbar.getLayoutParams();
            int targetHeight = baseHeight + systemBars.bottom;
            int targetBottomMargin = Math.max(0, ime.bottom - systemBars.bottom);
            if (params.height != targetHeight || params.bottomMargin != targetBottomMargin) {
                params.height = targetHeight;
                params.bottomMargin = targetBottomMargin;
                bottomToolbar.setLayoutParams(params);
            }
            bottomToolbar.setPadding(
                    bottomToolbar.getPaddingLeft(),
                    bottomToolbar.getPaddingTop(),
                    bottomToolbar.getPaddingRight(),
                    basePaddingBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private void openFirstAvailableImageSlot() {
        int index = firstEmptyImageSlot();
        if (index < 0) {
            Toast.makeText(requireContext(), R.string.journal_photo_limit_error, Toast.LENGTH_SHORT).show();
            return;
        }
        openImagePicker(index);
    }

    private void openImagePicker(int index) {
        selectedImageSlot = index;
        imagePickerLauncher.launch(new String[]{"image/*"});
    }

    private void handlePickedImage(Uri uri) {
        if (selectedImageSlot < 0 || selectedImageSlot >= MAX_IMAGE_COUNT) {
            selectedImageSlot = firstEmptyImageSlot();
        }
        if (selectedImageSlot < 0) {
            Toast.makeText(requireContext(), R.string.journal_photo_limit_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            Toast.makeText(requireContext(), R.string.journal_photo_pick_error, Toast.LENGTH_SHORT).show();
            selectedImageSlot = -1;
            return;
        }
        ensureImageListSize();
        imageUris.set(selectedImageSlot, uri.toString());
        selectedImageSlot = -1;
        refreshImageSlots();
    }

    private void removeImage(int index) {
        ensureImageListSize();
        imageUris.set(index, "");
        refreshImageSlots();
    }

    private int firstEmptyImageSlot() {
        ensureImageListSize();
        for (int index = 0; index < MAX_IMAGE_COUNT; index++) {
            if (imageUris.get(index).trim().isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private void refreshImageSlots() {
        if (photoSlots == null || photoViews == null || photoAddViews == null || photoRemoveButtons == null) {
            return;
        }
        ensureImageListSize();
        int addSlot = firstEmptyImageSlot();
        for (int index = 0; index < MAX_IMAGE_COUNT; index++) {
            String imageUri = imageUris.get(index);
            boolean hasImage = !imageUri.trim().isEmpty();
            boolean isAddSlot = !hasImage && index == addSlot;
            photoSlots[index].setVisibility(hasImage || isAddSlot ? View.VISIBLE : View.INVISIBLE);
            photoSlots[index].setBackgroundResource(isAddSlot
                    ? R.drawable.bg_journal_image_slot
                    : R.drawable.bg_journal_image_photo);
            photoSlots[index].setClickable(isAddSlot);
            photoSlots[index].setFocusable(isAddSlot);
            photoViews[index].setVisibility(hasImage ? View.VISIBLE : View.GONE);
            photoAddViews[index].setVisibility(isAddSlot ? View.VISIBLE : View.GONE);
            photoRemoveButtons[index].setVisibility(hasImage ? View.VISIBLE : View.GONE);
            if (hasImage) {
                photoViews[index].setImageURI(Uri.parse(imageUri));
            } else {
                photoViews[index].setImageDrawable(null);
            }
        }
    }

    private void ensureImageListSize() {
        while (imageUris.size() < MAX_IMAGE_COUNT) {
            imageUris.add("");
        }
        while (imageUris.size() > MAX_IMAGE_COUNT) {
            imageUris.remove(imageUris.size() - 1);
        }
    }

    private void applyJournalLayout() {
        if (patternBackground == null || journalPage == null || moodRow == null
                || photoRow == null || captionEdit == null || contentEdit == null) {
            return;
        }
        patternBackground.setImageResource(patternResource(selectedLayoutId));

        if (selectedLayoutId == LAYOUT_PLAID) {
            setPageMargins(8, 0);
            journalPage.setPadding(0, 0, 0, 0);
            journalPage.setBackgroundColor(Color.TRANSPARENT);
            moodRow.setBackgroundResource(R.drawable.bg_journal_layout_red_mood_strip);
            captionEdit.setVisibility(View.VISIBLE);
            captionEdit.setBackgroundResource(R.drawable.bg_journal_page_red_note);
            captionEdit.setPadding(dp(8), 0, dp(8), 0);
            contentEdit.setBackgroundResource(R.drawable.bg_journal_page_red_note);
            contentEdit.setPadding(dp(8), dp(8), dp(8), dp(16));
            applyPlaidConstraints();
        } else {
            setPageMargins(32, 32);
            journalPage.setPadding(dp(8), dp(8), dp(8), dp(8));
            journalPage.setBackgroundResource(selectedLayoutId == LAYOUT_DENIM
                    ? R.drawable.bg_journal_page_graph
                    : R.drawable.bg_journal_editor_page);
            moodRow.setBackgroundColor(Color.TRANSPARENT);
            captionEdit.setVisibility(View.VISIBLE);
            captionEdit.setBackgroundColor(Color.TRANSPARENT);
            captionEdit.setPadding(0, 0, 0, 0);
            contentEdit.setBackgroundColor(Color.TRANSPARENT);
            contentEdit.setPadding(0, 0, 0, dp(16));
            applyStackedConstraints();
        }
        refreshImageSlots();
    }

    private void applyStackedConstraints() {
        ConstraintSet pageSet = new ConstraintSet();
        pageSet.clone(journalPage);
        pageSet.clear(R.id.journal_mood_row);
        pageSet.constrainWidth(R.id.journal_mood_row, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.journal_mood_row, dp(64));
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        pageSet.clear(R.id.edit_journal_caption);
        pageSet.constrainWidth(R.id.edit_journal_caption, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.edit_journal_caption, dp(40));
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.TOP, R.id.journal_mood_row, ConstraintSet.BOTTOM, dp(8));
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        pageSet.clear(R.id.journal_photo_row);
        pageSet.constrainWidth(R.id.journal_photo_row, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.journal_photo_row, dp(96));
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.TOP, R.id.edit_journal_caption, ConstraintSet.BOTTOM, dp(8));
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        pageSet.clear(R.id.edit_journal_content);
        pageSet.constrainWidth(R.id.edit_journal_content, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.edit_journal_content, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.TOP, R.id.journal_photo_row, ConstraintSet.BOTTOM, dp(8));
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        pageSet.applyTo(journalPage);

        ConstraintSet photoSet = new ConstraintSet();
        photoSet.clone(photoRow);
        int[] slots = {
                R.id.journal_photo_slot_1,
                R.id.journal_photo_slot_2,
                R.id.journal_photo_slot_3
        };
        for (int slotId : slots) {
            photoSet.clear(slotId);
            photoSet.constrainWidth(slotId, ConstraintSet.MATCH_CONSTRAINT);
            photoSet.constrainHeight(slotId, ConstraintSet.MATCH_CONSTRAINT);
            photoSet.connect(slotId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            photoSet.connect(slotId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            photoSet.setHorizontalWeight(slotId, 1f);
        }
        photoSet.connect(R.id.journal_photo_slot_1, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        photoSet.connect(R.id.journal_photo_slot_1, ConstraintSet.END, R.id.journal_photo_slot_2, ConstraintSet.START, dp(8));
        photoSet.connect(R.id.journal_photo_slot_2, ConstraintSet.START, R.id.journal_photo_slot_1, ConstraintSet.END, dp(8));
        photoSet.connect(R.id.journal_photo_slot_2, ConstraintSet.END, R.id.journal_photo_slot_3, ConstraintSet.START, dp(8));
        photoSet.connect(R.id.journal_photo_slot_3, ConstraintSet.START, R.id.journal_photo_slot_2, ConstraintSet.END, dp(8));
        photoSet.connect(R.id.journal_photo_slot_3, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        photoSet.applyTo(photoRow);
    }

    private void applyPlaidConstraints() {
        ConstraintSet pageSet = new ConstraintSet();
        pageSet.clone(journalPage);
        pageSet.clear(R.id.journal_mood_row);
        pageSet.constrainWidth(R.id.journal_mood_row, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.journal_mood_row, dp(64));
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dp(32));
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp(32));
        pageSet.connect(R.id.journal_mood_row, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp(32));

        pageSet.clear(R.id.edit_journal_caption);
        pageSet.constrainWidth(R.id.edit_journal_caption, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.edit_journal_caption, dp(40));
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dp(256));
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp(128));
        pageSet.connect(R.id.edit_journal_caption, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp(8));

        pageSet.clear(R.id.journal_photo_row);
        pageSet.constrainWidth(R.id.journal_photo_row, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.journal_photo_row, dp(270));
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.TOP, R.id.journal_mood_row, ConstraintSet.BOTTOM, dp(24));
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        pageSet.connect(R.id.journal_photo_row, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        pageSet.clear(R.id.edit_journal_content);
        pageSet.constrainWidth(R.id.edit_journal_content, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.constrainHeight(R.id.edit_journal_content, ConstraintSet.MATCH_CONSTRAINT);
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.TOP, R.id.edit_journal_caption, ConstraintSet.BOTTOM, dp(8));
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp(128));
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp(8));
        pageSet.connect(R.id.edit_journal_content, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp(8));
        pageSet.applyTo(journalPage);

        ConstraintSet photoSet = new ConstraintSet();
        photoSet.clone(photoRow);
        constrainPlaidPhotoSlot(photoSet, R.id.journal_photo_slot_1, 32, 0);
        constrainPlaidPhotoSlot(photoSet, R.id.journal_photo_slot_2, 136, 32);
        constrainPlaidPhotoSlot(photoSet, R.id.journal_photo_slot_3, 8, 132);
        photoSet.applyTo(photoRow);
    }

    private void constrainPlaidPhotoSlot(ConstraintSet set, int slotId, int startDp, int topDp) {
        set.clear(slotId);
        set.constrainWidth(slotId, dp(88));
        set.constrainHeight(slotId, dp(100));
        set.connect(slotId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dp(topDp));
        set.connect(slotId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp(startDp));
    }

    private void setPageMargins(int horizontalDp, int verticalDp) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) journalPage.getLayoutParams();
        int horizontal = dp(horizontalDp);
        int vertical = dp(verticalDp);
        if (params.leftMargin != horizontal || params.topMargin != vertical
                || params.rightMargin != horizontal || params.bottomMargin != vertical) {
            params.setMargins(horizontal, vertical, horizontal, vertical);
            journalPage.setLayoutParams(params);
        }
    }

    private int patternResource(long layoutId) {
        if (layoutId == LAYOUT_DENIM) {
            return R.drawable.journal_layout_write_2_pattern;
        }
        if (layoutId == LAYOUT_PLAID) {
            return R.drawable.journal_layout_2_pattern;
        }
        return R.drawable.journal_layout_1_pattern;
    }

    private int previewPageResource(long layoutId) {
        if (layoutId == LAYOUT_DENIM) {
            return R.drawable.bg_journal_page_graph;
        }
        if (layoutId == LAYOUT_PLAID) {
            return R.drawable.bg_journal_page_red_note;
        }
        return R.drawable.bg_journal_editor_page;
    }

    private long normalizeLayoutId(Long layoutId) {
        if (layoutId != null) {
            for (long availableLayoutId : JOURNAL_LAYOUTS) {
                if (layoutId == availableLayoutId) {
                    return availableLayoutId;
                }
            }
        }
        return LAYOUT_GINGHAM;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindValues() {
        if (captionEdit == null) {
            return;
        }
        captionAlignment = JournalTextUtils.alignmentFromStoredContent(caption);
        captionTextSizeSp = JournalTextUtils.textSizeFromStoredContent(caption, DEFAULT_CAPTION_TEXT_SIZE_SP);
        contentTextSizeSp = JournalTextUtils.textSizeFromStoredContent(content, DEFAULT_CONTENT_TEXT_SIZE_SP);
        captionEdit.setTextSize(captionTextSizeSp);
        captionEdit.setText(JournalTextUtils.fromStoredContent(caption), TextView.BufferType.SPANNABLE);
        applyCaptionGravity();
        contentEdit.setTextSize(contentTextSizeSp);
        contentEdit.setText(JournalTextUtils.fromStoredContent(content), TextView.BufferType.SPANNABLE);
        lastSelectionEdit = null;
        lastSelectionStart = -1;
        lastSelectionEnd = -1;
        dateText.setText(formatHeaderDate(journalDate));
        applyJournalLayout();
        refreshMoodRow();
        refreshImageSlots();
    }

    private void captureInput() {
        caption = JournalTextUtils.withAlignmentMarker(
                JournalTextUtils.withTextSizeMarker(
                        JournalTextUtils.toStoredContent(captionEdit.getText()),
                        captionTextSizeSp
                ),
                captionAlignment
        );
        content = JournalTextUtils.withTextSizeMarker(
                JournalTextUtils.toStoredContent(contentEdit.getText()),
                contentTextSizeSp
        );
    }

    private void moveDay(int amount) {
        captureInput();
        journalDate = journalDate.plusDays(amount);
        ((MainActivity) requireActivity()).setSelectedDate(journalDate);
        bindValues();
    }

    @Override
    public boolean onHandleBackPressed() {
        saveDraftIfNeeded();
        suppressAutoSave = true;
        ((MainActivity) requireActivity()).finishToHome();
        return true;
    }

    private void save() {
        captureInput();
        if (contentEdit.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.journal_content_error, Toast.LENGTH_SHORT).show();
            return;
        }
        saveCurrentEntry();
        suppressAutoSave = true;
        ((MainActivity) requireActivity()).setSelectedDate(journalDate);
        ((MainActivity) requireActivity()).finishToHome();
    }

    private boolean saveDraftIfNeeded() {
        captureInput();
        if (journalId <= 0 && !hasDraftData()) {
            return false;
        }
        saveCurrentEntry();
        ((MainActivity) requireActivity()).setSelectedDate(journalDate);
        return true;
    }

    private void saveCurrentEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(journalId);
        entry.setLayoutId(selectedLayoutId);
        entry.setJournalDate(journalDate);
        entry.setTitle(titleForEntry());
        entry.setCaption(caption);
        entry.setContent(content);
        entry.setImageUris(imageUris);
        entry.setMoodResourceNames(selectedMoodResourceNames());
        journalId = repository.saveJournalEntry(entry);
    }

    private boolean hasDraftData() {
        if (!JournalTextUtils.plainText(caption).isEmpty() || !JournalTextUtils.plainText(content).isEmpty()) {
            return true;
        }
        ensureImageListSize();
        for (String imageUri : imageUris) {
            if (imageUri != null && !imageUri.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void applyStoredMoods(List<String> moodResourceNames) {
        if (moodResourceNames == null) {
            return;
        }
        int count = Math.min(moodResourceNames.size(), selectedMoodResources.length);
        for (int index = 0; index < count; index++) {
            selectedMoodResources[index] = moodResource(moodResourceNames.get(index), selectedMoodResources[index]);
        }
    }

    private List<String> selectedMoodResourceNames() {
        List<String> moodResourceNames = new ArrayList<>();
        for (int moodResource : selectedMoodResources) {
            try {
                String resourceName = getResources().getResourceEntryName(moodResource);
                moodResourceNames.add("journal_emo_buon_ngu_clean".equals(resourceName) ? MOOD_BUON_NGU : resourceName);
            } catch (android.content.res.Resources.NotFoundException ignored) {
                moodResourceNames.add("");
            }
        }
        return moodResourceNames;
    }

    private int moodResource(String resourceName, int fallback) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            return fallback;
        }
        String resolvedName = MOOD_BUON_NGU.equals(resourceName.trim())
                ? "journal_emo_buon_ngu_clean"
                : resourceName.trim();
        int resourceId = getResources().getIdentifier(resolvedName, "drawable", requireContext().getPackageName());
        return resourceId == 0 ? fallback : resourceId;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String titleForEntry() {
        String plainCaption = JournalTextUtils.plainText(caption);
        if (!plainCaption.isEmpty()) {
            return plainCaption;
        }
        if (!title.isEmpty()) {
            return JournalTextUtils.plainText(title);
        }
        return getString(R.string.journal_untitled);
    }

    private String formatHeaderDate(LocalDate date) {
        return getString(R.string.journal_date_header, weekdayName(date), DateTimeUtils.formatVietnameseDate(date));
    }

    private String weekdayName(LocalDate date) {
        if (date == null) {
            return "";
        }
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return getString(R.string.weekday_full_monday);
            case TUESDAY:
                return getString(R.string.weekday_full_tuesday);
            case WEDNESDAY:
                return getString(R.string.weekday_full_wednesday);
            case THURSDAY:
                return getString(R.string.weekday_full_thursday);
            case FRIDAY:
                return getString(R.string.weekday_full_friday);
            case SATURDAY:
                return getString(R.string.weekday_full_saturday);
            case SUNDAY:
            default:
                return getString(R.string.weekday_full_sunday);
        }
    }
}
