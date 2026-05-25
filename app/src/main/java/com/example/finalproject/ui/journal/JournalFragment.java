package com.example.finalproject.ui.journal;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.JournalEntryAdapter;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.JournalEntry;
import com.example.finalproject.repository.JournalRepository;
import com.example.finalproject.ui.common.DatePickerDialogFragment;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.JournalTextUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class JournalFragment extends Fragment {
    private static final String ARG_DATE = "date";
    private static final String DATE_RESULT_KEY = "journal_date_result";
    private static final String MOOD_BUON_NGU = "journal_emo_buon_ngu";
    private static final long LAYOUT_DENIM = 2L;
    private static final long LAYOUT_PLAID = 3L;
    private static final int DETAIL_CAPTION_TEXT_SIZE_SP = 20;
    private static final int DETAIL_CONTENT_TEXT_SIZE_SP = 16;

    private JournalRepository repository;
    private JournalEntryAdapter adapter;
    private LocalDate selectedDate;
    private TextView monthLabel;
    private TextView emptyLabel;
    private RecyclerView recyclerView;
    private ImageButton nextMonthButton;
    private final List<JournalEntry> currentEntries = new ArrayList<>();
    private boolean newestFirst;

    public static JournalFragment newInstance(LocalDate date) {
        JournalFragment fragment = new JournalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);
        repository = new JournalRepository(requireContext());
        selectedDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        selectedDate = clampFutureDate(selectedDate);
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
        monthLabel = view.findViewById(R.id.tv_journal_date);
        emptyLabel = view.findViewById(R.id.tv_journal_empty);
        recyclerView = view.findViewById(R.id.journal_recycler);
    }

    private void setupAdapter() {
        adapter = new JournalEntryAdapter(new JournalEntryAdapter.Listener() {
            @Override
            public void onClick(JournalEntry entry) {
                showEntryDetail(entry);
            }

            @Override
            public void onEdit(JournalEntry entry) {
                if (isFutureDate(entry.getJournalDate())) {
                    Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
                    return;
                }
                openEditor(entry.getId());
            }

            @Override
            public void onDelete(JournalEntry entry) {
                if (isFutureDate(entry.getJournalDate())) {
                    Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
                    return;
                }
                UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_journal), () -> {
                    repository.deleteJournalEntry(entry.getId());
                    refresh();
                });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupClicks(View view) {
        ImageButton previous = view.findViewById(R.id.btn_journal_prev_day);
        nextMonthButton = view.findViewById(R.id.btn_journal_next_day);
        previous.setOnClickListener(v -> moveMonth(-1));
        nextMonthButton.setOnClickListener(v -> moveMonth(1));
        monthLabel.setOnClickListener(v -> DatePickerDialogFragment
                .newInstance(DATE_RESULT_KEY, selectedDate)
                .show(getParentFragmentManager(), DATE_RESULT_KEY));
        view.findViewById(R.id.btn_journal_sort).setOnClickListener(v -> {
            newestFirst = !newestFirst;
            refresh();
        });
        view.findViewById(R.id.btn_journal_chart).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openJournalStats(selectedDate));
    }

    private void setupDateResult() {
        getParentFragmentManager().setFragmentResultListener(DATE_RESULT_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            LocalDate picked = DateTimeUtils.isoToDate(result.getString(DatePickerDialogFragment.RESULT_DATE));
            if (picked != null) {
                selectedDate = clampFutureDate(picked);
                ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
                refresh();
            }
        });
    }

    private void moveMonth(int amount) {
        LocalDate nextDate = selectedDate.plusMonths(amount);
        if (isFutureMonth(nextDate)) {
            Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        selectedDate = nextDate;
        ((MainActivity) requireActivity()).setSelectedDate(selectedDate);
        refresh();
    }

    private void refresh() {
        if (!isAdded()) {
            return;
        }
        updateDateNavigation();
        monthLabel.setText(getString(R.string.journal_month_format, selectedDate.getMonthValue(), selectedDate.getYear()));
        List<JournalEntry> entries = repository.getEntriesForMonth(selectedDate);
        if (newestFirst) {
            Collections.reverse(entries);
        }
        currentEntries.clear();
        currentEntries.addAll(entries);
        adapter.submit(entries);
        recyclerView.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
        emptyLabel.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEditor(long journalId) {
        ((MainActivity) requireActivity()).openJournalEditor(journalId, selectedDate);
    }

    private LocalDate clampFutureDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date != null && date.isAfter(today)) {
            Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
            return today;
        }
        return date == null ? today : date;
    }

    private boolean isFutureDate(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    private boolean isFutureMonth(LocalDate date) {
        return date != null && date.withDayOfMonth(1).isAfter(LocalDate.now().withDayOfMonth(1));
    }

    private void updateDateNavigation() {
        if (nextMonthButton == null) {
            return;
        }
        boolean canMoveForward = selectedDate.withDayOfMonth(1).isBefore(LocalDate.now().withDayOfMonth(1));
        nextMonthButton.setEnabled(canMoveForward);
        nextMonthButton.setAlpha(canMoveForward ? 1f : 0.35f);
    }

    private void showEntryDetail(JournalEntry initialEntry) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View detailView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_journal_entry_detail, null);
        dialog.setContentView(detailView);
        JournalEntry[] visibleEntry = {initialEntry};
        bindEntryDetail(detailView, visibleEntry[0]);
        setupDetailPhotoSizing(detailView);

        ImageButton previous = detailView.findViewById(R.id.btn_journal_detail_prev);
        ImageButton next = detailView.findViewById(R.id.btn_journal_detail_next);
        boolean canNavigate = currentEntries.size() > 1;
        previous.setEnabled(canNavigate);
        next.setEnabled(canNavigate);
        previous.setAlpha(canNavigate ? 1f : 0.35f);
        next.setAlpha(canNavigate ? 1f : 0.35f);
        previous.setOnClickListener(v -> {
            visibleEntry[0] = adjacentEntry(visibleEntry[0], -1);
            bindEntryDetail(detailView, visibleEntry[0]);
        });
        next.setOnClickListener(v -> {
            visibleEntry[0] = adjacentEntry(visibleEntry[0], 1);
            bindEntryDetail(detailView, visibleEntry[0]);
        });
        detailView.findViewById(R.id.btn_journal_detail_edit).setOnClickListener(v -> {
            if (isFutureDate(visibleEntry[0].getJournalDate())) {
                Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            openEditor(visibleEntry[0].getId());
        });
        detailView.findViewById(R.id.btn_journal_detail_delete).setOnClickListener(v ->
                {
                    if (isFutureDate(visibleEntry[0].getJournalDate())) {
                        Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UiUtils.showDeleteDialog(requireContext(), getString(R.string.delete_confirm_journal), () -> {
                    repository.deleteJournalEntry(visibleEntry[0].getId());
                    dialog.dismiss();
                    refresh();
                    });
                });
        dialog.setOnShowListener(dialogInterface -> expandDetailSheet(dialog));
        dialog.show();
    }

    private void expandDetailSheet(BottomSheetDialog dialog) {
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int height = Math.min(screenHeight - dp(64), Math.max(dp(440), Math.round(screenHeight * 0.65f)));
        params.height = height;
        bottomSheet.setLayoutParams(params);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight(height);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private JournalEntry adjacentEntry(JournalEntry current, int offset) {
        if (currentEntries.isEmpty()) {
            return current;
        }
        int currentIndex = 0;
        for (int index = 0; index < currentEntries.size(); index++) {
            if (currentEntries.get(index).getId() == current.getId()) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = (currentIndex + offset + currentEntries.size()) % currentEntries.size();
        return currentEntries.get(nextIndex);
    }

    private void bindEntryDetail(View detailView, JournalEntry entry) {
        ((TextView) detailView.findViewById(R.id.tv_journal_detail_date)).setText(formatFullDate(entry.getJournalDate()));
        ((ImageView) detailView.findViewById(R.id.img_journal_detail_pattern)).setImageResource(patternResource(entry.getLayoutId()));
        View page = detailView.findViewById(R.id.journal_detail_page);
        page.setBackgroundResource(pageResource(entry.getLayoutId()));
        View moodRow = detailView.findViewById(R.id.journal_detail_mood_row);
        moodRow.setBackgroundResource(isLayout(entry.getLayoutId(), LAYOUT_PLAID)
                ? R.drawable.bg_journal_layout_red_mood_strip
                : 0);
        TextView caption = detailView.findViewById(R.id.tv_journal_detail_caption);
        caption.setTextSize(JournalTextUtils.textSizeFromStoredContent(entry.getCaption(), DETAIL_CAPTION_TEXT_SIZE_SP));
        caption.setText(displayTitle(entry), TextView.BufferType.SPANNABLE);
        caption.setGravity(titleGravity(entry));

        TextView content = detailView.findViewById(R.id.tv_journal_detail_content);
        content.setTextSize(JournalTextUtils.textSizeFromStoredContent(entry.getContent(), DETAIL_CONTENT_TEXT_SIZE_SP));
        content.setText(displayContent(entry), TextView.BufferType.SPANNABLE);
        content.setGravity(contentGravity(entry));
        bindDetailMoods(detailView, entry.getMoodResourceNames());
        bindDetailImages(detailView, entry.getImageUris());
    }

    private void setupDetailPhotoSizing(View detailView) {
        LinearLayout row = detailView.findViewById(R.id.journal_detail_photo_row);
        FrameLayout[] slots = detailPhotoSlots(detailView);
        row.post(() -> {
            int gap = dp(16);
            int available = row.getWidth();
            if (available <= 0) {
                return;
            }
            int slotWidth = Math.min(dp(92), Math.max(dp(64), (available - gap * 2) / 3));
            int slotHeight = Math.round(slotWidth * 1.12f);
            ViewGroup.LayoutParams rowParams = row.getLayoutParams();
            if (rowParams.height != slotHeight + dp(4)) {
                rowParams.height = slotHeight + dp(4);
                row.setLayoutParams(rowParams);
            }
            for (int index = 0; index < slots.length; index++) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) slots[index].getLayoutParams();
                params.width = slotWidth;
                params.height = slotHeight;
                params.leftMargin = index == 0 ? 0 : gap;
                slots[index].setLayoutParams(params);
            }
        });
    }

    private CharSequence displayTitle(JournalEntry entry) {
        if (!isBlank(JournalTextUtils.plainText(entry.getCaption()))) {
            return JournalTextUtils.fromStoredContent(entry.getCaption());
        }
        String plainTitle = JournalTextUtils.plainText(entry.getTitle());
        return isBlank(plainTitle) ? getString(R.string.journal_untitled) : plainTitle;
    }

    private CharSequence displayContent(JournalEntry entry) {
        if (isBlank(JournalTextUtils.plainText(entry.getContent()))) {
            return getString(R.string.journal_empty_preview);
        }
        return JournalTextUtils.fromStoredContent(entry.getContent());
    }

    private int titleGravity(JournalEntry entry) {
        if (isBlank(JournalTextUtils.plainText(entry.getCaption()))) {
            return Gravity.CENTER;
        }
        return gravityFromAlignment(JournalTextUtils.alignmentFromStoredContent(entry.getCaption()), true);
    }

    private int contentGravity(JournalEntry entry) {
        return gravityFromAlignment(firstAlignment(JournalTextUtils.fromStoredContent(entry.getContent())), false);
    }

    private int gravityFromAlignment(Layout.Alignment alignment, boolean centerVertically) {
        int vertical = centerVertically ? Gravity.CENTER_VERTICAL : Gravity.TOP;
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            return Gravity.CENTER_HORIZONTAL | vertical;
        }
        if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
            return Gravity.END | vertical;
        }
        return Gravity.START | vertical;
    }

    private Layout.Alignment firstAlignment(CharSequence value) {
        if (!(value instanceof Spanned)) {
            return Layout.Alignment.ALIGN_NORMAL;
        }
        Spanned spanned = (Spanned) value;
        AlignmentSpan[] spans = spanned.getSpans(0, spanned.length(), AlignmentSpan.class);
        if (spans.length == 0) {
            return Layout.Alignment.ALIGN_NORMAL;
        }
        return spans[0].getAlignment();
    }

    private void bindDetailMoods(View detailView, List<String> moodResourceNames) {
        int[] fallback = {
                R.drawable.journal_emo_nang,
                R.drawable.journal_emo_vui,
                R.drawable.journal_emo_sang_khoai
        };
        ImageView[] moodViews = {
                detailView.findViewById(R.id.img_journal_detail_mood_weather),
                detailView.findViewById(R.id.img_journal_detail_mood_day),
                detailView.findViewById(R.id.img_journal_detail_mood_body)
        };
        for (int index = 0; index < moodViews.length; index++) {
            String resourceName = moodResourceNames != null && index < moodResourceNames.size()
                    ? moodResourceNames.get(index)
                    : "";
            moodViews[index].setImageResource(moodResource(resourceName, fallback[index]));
        }
    }

    private int moodResource(String resourceName, int fallback) {
        if (isBlank(resourceName)) {
            return fallback;
        }
        String resolvedName = MOOD_BUON_NGU.equals(resourceName.trim())
                ? "journal_emo_buon_ngu_clean"
                : resourceName.trim();
        int resourceId = getResources().getIdentifier(resolvedName, "drawable", requireContext().getPackageName());
        return resourceId == 0 ? fallback : resourceId;
    }

    private void bindDetailImages(View detailView, List<String> imageUris) {
        FrameLayout[] photoSlots = detailPhotoSlots(detailView);
        ImageView[] photoViews = {
                detailView.findViewById(R.id.img_journal_detail_photo_1),
                detailView.findViewById(R.id.img_journal_detail_photo_2),
                detailView.findViewById(R.id.img_journal_detail_photo_3)
        };
        for (int index = 0; index < photoSlots.length; index++) {
            String imageUri = imageUris != null && index < imageUris.size() ? imageUris.get(index) : "";
            boolean hasImage = !isBlank(imageUri);
            photoSlots[index].setVisibility(hasImage ? View.VISIBLE : View.GONE);
            photoViews[index].setImageDrawable(null);
            if (hasImage) {
                photoViews[index].setImageURI(Uri.parse(imageUri));
            }
        }
    }

    private FrameLayout[] detailPhotoSlots(View detailView) {
        return new FrameLayout[]{
                detailView.findViewById(R.id.journal_detail_photo_slot_1),
                detailView.findViewById(R.id.journal_detail_photo_slot_2),
                detailView.findViewById(R.id.journal_detail_photo_slot_3)
        };
    }

    private int patternResource(Long layoutId) {
        if (isLayout(layoutId, LAYOUT_DENIM)) {
            return R.drawable.journal_layout_write_2_pattern;
        }
        if (isLayout(layoutId, LAYOUT_PLAID)) {
            return R.drawable.journal_layout_2_pattern;
        }
        return R.drawable.journal_layout_1_pattern;
    }

    private int pageResource(Long layoutId) {
        if (isLayout(layoutId, LAYOUT_DENIM)) {
            return R.drawable.bg_journal_page_graph;
        }
        if (isLayout(layoutId, LAYOUT_PLAID)) {
            return R.drawable.bg_journal_page_red_note;
        }
        return R.drawable.bg_journal_editor_page;
    }

    private boolean isLayout(Long layoutId, long expected) {
        return layoutId != null && layoutId == expected;
    }

    private String formatFullDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return String.format(
                Locale.US,
                "%s, %s",
                weekdayName(date),
                DateTimeUtils.formatVietnameseDate(date)
        );
    }

    private String weekdayName(LocalDate date) {
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
