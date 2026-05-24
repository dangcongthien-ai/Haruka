package com.example.finalproject.ui.journal;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.JournalEntry;
import com.example.finalproject.repository.JournalRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JournalStatsFragment extends Fragment {
    private static final String ARG_DATE = "date";
    private static final String MOOD_BUON_NGU = "journal_emo_buon_ngu";
    private static final int TOP_STICKER_LIMIT = 3;

    private JournalRepository repository;
    private LocalDate selectedDate;
    private boolean yearMode;

    private TextView monthButton;
    private TextView yearButton;
    private TextView periodLabel;
    private TextView recordTitle;
    private TextView streakLabel;
    private TextView recordLabel;
    private TextView writtenLabel;
    private TextView percentLabel;
    private GridLayout calendarGrid;
    private FrameLayout progressTrack;
    private View progressFill;
    private LinearLayout moodBars;
    private LinearLayout topStickers;
    private View topCard;

    private static class MoodCount {
        final String resourceName;
        final int count;

        MoodCount(String resourceName, int count) {
            this.resourceName = resourceName;
            this.count = count;
        }
    }

    public static JournalStatsFragment newInstance(LocalDate date) {
        JournalStatsFragment fragment = new JournalStatsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal_stats, container, false);
        repository = new JournalRepository(requireContext());
        selectedDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        if (selectedDate.isAfter(LocalDate.now())) {
            selectedDate = LocalDate.now();
        }
        bind(view);
        setupClicks(view);
        render();
        return view;
    }

    private void bind(View view) {
        monthButton = view.findViewById(R.id.btn_journal_stats_month);
        yearButton = view.findViewById(R.id.btn_journal_stats_year);
        periodLabel = view.findViewById(R.id.tv_journal_stats_period);
        recordTitle = view.findViewById(R.id.tv_journal_stats_record_title);
        streakLabel = view.findViewById(R.id.tv_journal_stats_streak);
        recordLabel = view.findViewById(R.id.tv_journal_stats_record);
        writtenLabel = view.findViewById(R.id.tv_journal_stats_written);
        percentLabel = view.findViewById(R.id.tv_journal_stats_percent);
        calendarGrid = view.findViewById(R.id.grid_journal_stats_calendar);
        progressTrack = view.findViewById(R.id.journal_stats_progress_track);
        progressFill = view.findViewById(R.id.journal_stats_progress_fill);
        moodBars = view.findViewById(R.id.journal_stats_mood_bars);
        topStickers = view.findViewById(R.id.journal_stats_top_stickers);
        topCard = view.findViewById(R.id.journal_stats_top_card);
    }

    private void setupClicks(View view) {
        ImageButton backButton = view.findViewById(R.id.btn_journal_stats_back);
        backButton.setOnClickListener(v -> ((MainActivity) requireActivity()).finishFullScreen());
        monthButton.setOnClickListener(v -> {
            yearMode = false;
            render();
        });
        yearButton.setOnClickListener(v -> {
            yearMode = true;
            render();
        });
        view.findViewById(R.id.btn_journal_stats_prev).setOnClickListener(v -> {
            selectedDate = yearMode ? selectedDate.minusYears(1) : selectedDate.minusMonths(1);
            render();
        });
        view.findViewById(R.id.btn_journal_stats_next).setOnClickListener(v -> {
            LocalDate nextDate = yearMode ? selectedDate.plusYears(1) : selectedDate.plusMonths(1);
            if (isFuturePeriod(nextDate)) {
                Toast.makeText(requireContext(), R.string.future_date_action_blocked, Toast.LENGTH_SHORT).show();
                return;
            }
            selectedDate = nextDate;
            render();
        });
    }

    private boolean isFuturePeriod(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (yearMode) {
            return date.getYear() > today.getYear();
        }
        return date.withDayOfMonth(1).isAfter(today.withDayOfMonth(1));
    }

    private void render() {
        setModeButtonState();
        periodLabel.setText(yearMode
                ? getString(R.string.journal_stats_year_period, selectedDate.getYear())
                : getString(R.string.journal_month_format, selectedDate.getMonthValue(), selectedDate.getYear()));

        List<JournalEntry> entries = yearMode
                ? repository.getEntriesForYear(selectedDate)
                : repository.getEntriesForMonth(selectedDate);
        Set<LocalDate> writtenDates = writtenDates(entries);
        int totalDays = yearMode ? totalDaysForYearProgress() : selectedDate.lengthOfMonth();
        int writtenDays = writtenDates.size();
        int percent = totalDays == 0 ? 0 : Math.min(100, Math.round((writtenDays * 100f) / totalDays));

        recordTitle.setText(yearMode
                ? getString(R.string.journal_stats_year_record, selectedDate.getYear())
                : getString(R.string.journal_stats_month_record, selectedDate.getMonthValue()));
        streakLabel.setText(getString(R.string.journal_stats_streak, currentStreak(writtenDates)));
        recordLabel.setText(getString(R.string.journal_stats_best_streak, longestStreak(writtenDates)));
        writtenLabel.setText(getString(R.string.journal_stats_days_written, writtenDays, totalDays));
        percentLabel.setText(getString(R.string.journal_stats_percent, percent));
        updateProgress(percent);
        renderCalendar(writtenDates);

        List<MoodCount> counts = moodCounts(entries);
        renderMoodBars(counts);
        topCard.setVisibility(yearMode ? View.VISIBLE : View.GONE);
        if (yearMode) {
            renderTopStickers(counts);
        } else {
            topStickers.removeAllViews();
        }
    }

    private int totalDaysForYearProgress() {
        LocalDate today = LocalDate.now();
        if (selectedDate.getYear() < today.getYear()) {
            return selectedDate.lengthOfYear();
        }
        if (selectedDate.getYear() == today.getYear()) {
            return today.getDayOfYear();
        }
        return selectedDate.getDayOfYear();
    }

    private void setModeButtonState() {
        monthButton.setBackgroundResource(yearMode
                ? R.drawable.bg_journal_stats_segment
                : R.drawable.bg_journal_stats_segment_selected);
        yearButton.setBackgroundResource(yearMode
                ? R.drawable.bg_journal_stats_segment_selected
                : R.drawable.bg_journal_stats_segment);
        monthButton.setTextColor(requireContext().getColor(yearMode ? R.color.text_primary : R.color.brand_orange));
        yearButton.setTextColor(requireContext().getColor(yearMode ? R.color.brand_orange : R.color.text_primary));
    }

    private Set<LocalDate> writtenDates(List<JournalEntry> entries) {
        Set<LocalDate> dates = new HashSet<>();
        for (JournalEntry entry : entries) {
            if (entry.getJournalDate() != null) {
                dates.add(entry.getJournalDate());
            }
        }
        return dates;
    }

    private int currentStreak(Set<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = dates.contains(selectedDate) ? selectedDate : Collections.max(dates);
        int count = 0;
        while (dates.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    private int longestStreak(Set<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);
        int best = 1;
        int current = 1;
        for (int index = 1; index < sorted.size(); index++) {
            if (sorted.get(index - 1).plusDays(1).equals(sorted.get(index))) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }
        return best;
    }

    private List<MoodCount> moodCounts(List<JournalEntry> entries) {
        Map<String, Integer> counts = new HashMap<>();
        for (JournalEntry entry : entries) {
            for (String moodName : entry.getMoodResourceNames()) {
                if (!isBlank(moodName)) {
                    String normalized = moodName.trim();
                    counts.put(normalized, counts.containsKey(normalized) ? counts.get(normalized) + 1 : 1);
                }
            }
        }
        List<MoodCount> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            result.add(new MoodCount(entry.getKey(), entry.getValue()));
        }
        Collections.sort(result, (left, right) -> {
            int byCount = right.count - left.count;
            if (byCount != 0) {
                return byCount;
            }
            return left.resourceName.compareTo(right.resourceName);
        });
        return result;
    }

    private void updateProgress(int percent) {
        progressTrack.post(() -> {
            int width = progressTrack.getWidth();
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = Math.round(width * (Math.max(0, Math.min(percent, 100)) / 100f));
            progressFill.setLayoutParams(params);
        });
    }

    private void renderCalendar(Set<LocalDate> writtenDates) {
        calendarGrid.removeAllViews();
        calendarGrid.setVisibility(yearMode ? View.GONE : View.VISIBLE);
        if (yearMode) {
            return;
        }
        int[] weekdayLabels = {
                R.string.weekday_sunday,
                R.string.weekday_monday,
                R.string.weekday_tuesday,
                R.string.weekday_wednesday,
                R.string.weekday_thursday,
                R.string.weekday_friday,
                R.string.weekday_saturday
        };
        for (int labelRes : weekdayLabels) {
            TextView label = new TextView(requireContext());
            label.setGravity(Gravity.CENTER);
            label.setText(labelRes);
            label.setTextColor(requireContext().getColor(R.color.text_secondary));
            label.setTextSize(10);
            calendarGrid.addView(label, calendarCellParams(dp(22)));
        }
        LocalDate firstDay = selectedDate.withDayOfMonth(1);
        int leadingBlankCount = firstDay.getDayOfWeek().getValue() % 7;
        for (int index = 0; index < leadingBlankCount; index++) {
            calendarGrid.addView(new View(requireContext()), calendarCellParams(dp(30)));
        }
        for (int day = 1; day <= selectedDate.lengthOfMonth(); day++) {
            LocalDate date = selectedDate.withDayOfMonth(day);
            calendarGrid.addView(dayCell(day, writtenDates.contains(date)), calendarCellParams(dp(34)));
        }
    }

    private GridLayout.LayoutParams calendarCellParams(int height) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private View dayCell(int day, boolean written) {
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setGravity(Gravity.CENTER);
        cell.setOrientation(LinearLayout.VERTICAL);

        TextView number = new TextView(requireContext());
        number.setGravity(Gravity.CENTER);
        number.setText(String.format(Locale.US, "%d", day));
        number.setTextColor(requireContext().getColor(written ? R.color.brand_orange_dark : R.color.text_secondary));
        number.setTextSize(10);
        cell.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(15)));

        View dot = new View(requireContext());
        dot.setBackgroundResource(written
                ? R.drawable.bg_journal_stats_written_day
                : R.drawable.bg_journal_stats_empty_day);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.topMargin = dp(2);
        cell.addView(dot, dotParams);
        return cell;
    }

    private void renderMoodBars(List<MoodCount> counts) {
        moodBars.removeAllViews();
        if (counts.isEmpty()) {
            addEmptyState(moodBars);
            return;
        }
        int max = Math.max(1, counts.get(0).count);
        for (MoodCount moodCount : counts) {
            moodBars.addView(moodBar(moodCount, max), moodBarParams());
        }
    }

    private View moodBar(MoodCount moodCount, int max) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        item.setOrientation(LinearLayout.VERTICAL);

        TextView count = new TextView(requireContext());
        count.setGravity(Gravity.CENTER);
        count.setText(String.format(Locale.US, "%d", moodCount.count));
        count.setTextColor(requireContext().getColor(R.color.text_secondary));
        count.setTextSize(10);
        item.addView(count, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));

        FrameLayout barSpace = new FrameLayout(requireContext());
        View bar = new View(requireContext());
        bar.setBackgroundResource(R.drawable.bg_journal_stats_bar);
        int barHeight = Math.max(dp(22), Math.round(dp(128) * (moodCount.count / (float) max)));
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(dp(32), barHeight);
        barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barSpace.addView(bar, barParams);
        item.addView(barSpace, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));

        ImageView icon = new ImageView(requireContext());
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(moodResource(moodCount.resourceName));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconParams.topMargin = dp(6);
        item.addView(icon, iconParams);
        return item;
    }

    private LinearLayout.LayoutParams moodBarParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(50), ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(dp(5), 0, dp(5), 0);
        return params;
    }

    private void renderTopStickers(List<MoodCount> counts) {
        topStickers.removeAllViews();
        if (counts.isEmpty()) {
            addEmptyState(topStickers);
            return;
        }
        int limit = Math.min(TOP_STICKER_LIMIT, counts.size());
        for (int index = 0; index < limit; index++) {
            topStickers.addView(topSticker(counts.get(index)), weightedParams(dp(5), dp(5)));
        }
    }

    private View topSticker(MoodCount moodCount) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setBackgroundResource(R.drawable.bg_journal_stats_inner_card);
        card.setElevation(dp(2));
        card.setGravity(Gravity.CENTER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(12));

        TextView count = new TextView(requireContext());
        count.setGravity(Gravity.CENTER);
        count.setText(getString(R.string.journal_stats_count_times, moodCount.count));
        count.setTextColor(requireContext().getColor(R.color.brand_orange));
        count.setTextSize(13);
        card.addView(count, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        ImageView icon = new ImageView(requireContext());
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(moodResource(moodCount.resourceName));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(88), dp(88));
        iconParams.topMargin = dp(10);
        card.addView(icon, iconParams);

        TextView label = new TextView(requireContext());
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setText(moodLabel(moodCount.resourceName));
        label.setTextColor(requireContext().getColor(R.color.brand_orange));
        label.setTextSize(13);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24));
        labelParams.topMargin = dp(8);
        card.addView(label, labelParams);
        return card;
    }

    private LinearLayout.LayoutParams weightedParams(int startMargin, int endMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(startMargin, 0, endMargin, 0);
        return params;
    }

    private void addEmptyState(LinearLayout container) {
        TextView empty = new TextView(requireContext());
        empty.setGravity(Gravity.CENTER);
        empty.setText(R.string.journal_stats_no_data);
        empty.setTextColor(requireContext().getColor(R.color.text_muted));
        empty.setTextSize(14);
        int width = container == moodBars
                ? Math.max(dp(280), getResources().getDisplayMetrics().widthPixels - dp(44))
                : ViewGroup.LayoutParams.MATCH_PARENT;
        container.addView(empty, new LinearLayout.LayoutParams(
                width,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private int moodResource(String resourceName) {
        int fallback = R.drawable.journal_emo_vui;
        if (isBlank(resourceName)) {
            return fallback;
        }
        String resolvedName = MOOD_BUON_NGU.equals(resourceName.trim())
                ? "journal_emo_buon_ngu_clean"
                : resourceName.trim();
        int resourceId = getResources().getIdentifier(resolvedName, "drawable", requireContext().getPackageName());
        return resourceId == 0 ? fallback : resourceId;
    }

    private String moodLabel(String resourceName) {
        if (isBlank(resourceName)) {
            return "";
        }
        String labelName = "journal_emo_buon_ba".equals(resourceName.trim())
                ? "journal_emo_buon"
                : resourceName.trim();
        int labelRes = getResources().getIdentifier(labelName, "string", requireContext().getPackageName());
        return labelRes == 0 ? "" : getString(labelRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
