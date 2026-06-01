package com.example.finalproject.ui.habit;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.HabitCategory;
import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitLog;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.HabitDefaults;
import com.example.finalproject.util.HabitScheduleUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HabitStatsFragment extends Fragment {
    private static final String ARG_DATE = "date";
    private static final int FILTER_ALL_ID = -1;
    private static final int PAGE_SIZE = 30;
    private static final int PAGE_GRID_COLUMNS = 7;

    private HabitRepository repository;
    private LocalDate selectedDate;
    private Long filterCategoryId;

    private TextView filterLabel;
    private TextView periodLabel;
    private TextView emptyView;
    private LinearLayout cardsContainer;

    public static HabitStatsFragment newInstance(LocalDate date) {
        HabitStatsFragment fragment = new HabitStatsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, DateTimeUtils.dateToIso(date));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit_stats, container, false);
        repository = new HabitRepository(requireContext());
        selectedDate = DateTimeUtils.isoToDate(requireArguments().getString(ARG_DATE));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        bind(view);
        setupClicks(view);
        render();
        return view;
    }

    private void bind(View view) {
        filterLabel = view.findViewById(R.id.tv_habit_stats_filter);
        periodLabel = view.findViewById(R.id.tv_habit_stats_period);
        emptyView = view.findViewById(R.id.tv_habit_stats_empty);
        cardsContainer = view.findViewById(R.id.layout_habit_stats_cards);
    }

    private void setupClicks(View view) {
        ImageButton backButton = view.findViewById(R.id.btn_habit_stats_back);
        backButton.setOnClickListener(v -> ((MainActivity) requireActivity()).finishFullScreen());
        view.findViewById(R.id.layout_habit_stats_filter).setOnClickListener(this::showFilterMenu);
    }

    private void showFilterMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(
                new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Light),
                anchor
        );
        popupMenu.getMenu().add(Menu.NONE, FILTER_ALL_ID, Menu.NONE, getString(R.string.habit_report_filter_all));
        List<HabitCategory> categories = repository.getCategories();
        for (HabitCategory category : categories) {
            popupMenu.getMenu().add(Menu.NONE, (int) category.getId(), Menu.NONE, category.getName());
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            filterCategoryId = item.getItemId() == FILTER_ALL_ID ? null : (long) item.getItemId();
            render();
            return true;
        });
        popupMenu.show();
    }

    private void render() {
        periodLabel.setText(getString(
                R.string.habit_report_period,
                selectedDate.getMonthValue(),
                selectedDate.getYear()
        ));

        HabitCategory selectedCategory = filterCategoryId == null ? null : repository.getCategory(filterCategoryId);
        if (filterCategoryId != null && selectedCategory == null) {
            filterCategoryId = null;
        }
        filterLabel.setText(selectedCategory == null
                ? getString(R.string.habit_report_filter_all)
                : selectedCategory.getName());

        List<HabitItem> habits = repository.getHabits();
        List<HabitItem> visibleHabits = new ArrayList<>();
        for (HabitItem habit : habits) {
            if (filterCategoryId != null && (habit.getCategoryId() == null || !filterCategoryId.equals(habit.getCategoryId()))) {
                continue;
            }
            if (!HabitScheduleUtils.overlapsMonth(habit, selectedDate)) {
                continue;
            }
            if (buildDueDates(habit).isEmpty()) {
                continue;
            }
            visibleHabits.add(habit);
        }
        Collections.sort(visibleHabits, reportComparator());

        Map<Long, Map<LocalDate, HabitLog>> logsByHabit = loadLogsForHabits(visibleHabits);
        cardsContainer.removeAllViews();
        if (visibleHabits.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        for (HabitItem habit : visibleHabits) {
            cardsContainer.addView(createReportCard(habit, logsByHabit.get(habit.getId())));
        }
    }

    private Comparator<HabitItem> reportComparator() {
        return (left, right) -> {
            String leftCategory = left.getCategory() == null ? "" : left.getCategory().getName();
            String rightCategory = right.getCategory() == null ? "" : right.getCategory().getName();
            int byCategory = leftCategory.compareToIgnoreCase(rightCategory);
            if (byCategory != 0) {
                return byCategory;
            }
            int leftPriority = left.getPriority() == null ? Integer.MAX_VALUE : left.getPriority().getPriorityOrder();
            int rightPriority = right.getPriority() == null ? Integer.MAX_VALUE : right.getPriority().getPriorityOrder();
            if (leftPriority != rightPriority) {
                return leftPriority - rightPriority;
            }
            return safeText(left.getTitle()).compareToIgnoreCase(safeText(right.getTitle()));
        };
    }

    private Map<Long, Map<LocalDate, HabitLog>> loadLogsForHabits(List<HabitItem> habits) {
        Map<Long, Map<LocalDate, HabitLog>> grouped = new HashMap<>();
        if (habits.isEmpty()) {
            return grouped;
        }
        LocalDate start = null;
        LocalDate end = selectedDate;
        for (HabitItem habit : habits) {
            if (habit.getStartDate() == null) {
                continue;
            }
            if (start == null || habit.getStartDate().isBefore(start)) {
                start = habit.getStartDate();
            }
            LocalDate reportEnd = habit.hasEndDate() && habit.getEndDate() != null
                    ? habit.getEndDate()
                    : selectedDate;
            if (reportEnd.isAfter(end)) {
                end = reportEnd;
            }
        }
        if (start == null) {
            return grouped;
        }
        for (HabitLog log : repository.getHabitLogsInRange(start, end)) {
            Map<LocalDate, HabitLog> byDate = grouped.get(log.getHabitId());
            if (byDate == null) {
                byDate = new HashMap<>();
                grouped.put(log.getHabitId(), byDate);
            }
            byDate.put(log.getLogDate(), log);
        }
        return grouped;
    }

    private View createReportCard(HabitItem habit, @Nullable Map<LocalDate, HabitLog> logsByDate) {
        int accentColor = UiUtils.safeColor(
                habit.getColor(),
                requireContext().getColor(R.color.brand_orange_light)
        );
        List<LocalDate> dueDates = buildDueDates(habit);
        List<LocalDate> streakDates = collectStreakDates(habit, dueDates, logsByDate);
        int pageCount = resolvePageCount(dueDates.size());
        int initialPage = resolveInitialPage(dueDates, pageCount);

        FrameLayout card = new FrameLayout(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.bg_habit_report_card);

        LinearLayout content = new LinearLayout(requireContext());
        content.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.setOrientation(LinearLayout.VERTICAL);
        card.addView(content);

        content.addView(createReportHeader(habit, dueDates, logsByDate, accentColor));

        GridLayout grid = new GridLayout(requireContext());
        grid.setColumnCount(PAGE_GRID_COLUMNS);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        grid.setPadding(0, dp(14), 0, dp(12));
        grid.setUseDefaultMargins(false);

        bindPageGrid(grid, habit, dueDates, logsByDate, streakDates, initialPage);
        if (pageCount > 1) {
            content.addView(createPagedReportBody(grid, habit, dueDates, logsByDate, streakDates, pageCount, initialPage));
        } else {
            content.addView(createGridHost(grid, false));
        }
        return card;
    }

    private View createReportHeader(HabitItem habit, List<LocalDate> dueDates, @Nullable Map<LocalDate, HabitLog> logsByDate, int accentColor) {
        LinearLayout header = new LinearLayout(requireContext());
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(12), 0, dp(12), 0);
        header.setBackground(headerDrawable(accentColor));

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(habit.getCategory() == null
                ? R.drawable.habit_category_other
                : HabitDefaults.resolveIconRes(requireContext(), habit.getCategory().getIconUri()));
        header.addView(icon);

        TextView title = new TextView(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(10), 0, dp(10), 0);
        title.setLayoutParams(titleParams);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setMaxLines(1);
        title.setGravity(Gravity.CENTER);
        title.setText(habit.getTitle());
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(17);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title);

        TextView progress = new TextView(requireContext());
        progress.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progress.setText(completionText(habit, dueDates, logsByDate));
        progress.setTextColor(requireContext().getColor(R.color.text_primary));
        progress.setTextSize(15);
        progress.setTypeface(progress.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(progress);
        return header;
    }

    private View createPagedReportBody(
            GridLayout grid,
            HabitItem habit,
            List<LocalDate> dueDates,
            @Nullable Map<LocalDate, HabitLog> logsByDate,
            List<LocalDate> streakDates,
            int pageCount,
            int initialPage
    ) {
        LinearLayout body = new LinearLayout(requireContext());
        body.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(8), 0, dp(8), 0);

        int[] selectedPage = {initialPage};
        TextView previous = createPageArrow("<", R.string.previous);
        TextView next = createPageArrow(">", R.string.next);
        body.addView(previous);
        body.addView(createGridHost(grid, true));
        body.addView(next);

        previous.setOnClickListener(v -> {
            if (selectedPage[0] <= 0) {
                return;
            }
            selectedPage[0]--;
            bindPageGrid(grid, habit, dueDates, logsByDate, streakDates, selectedPage[0]);
            updatePageArrows(previous, next, selectedPage[0], pageCount);
        });
        next.setOnClickListener(v -> {
            if (selectedPage[0] >= pageCount - 1) {
                return;
            }
            selectedPage[0]++;
            bindPageGrid(grid, habit, dueDates, logsByDate, streakDates, selectedPage[0]);
            updatePageArrows(previous, next, selectedPage[0], pageCount);
        });
        updatePageArrows(previous, next, selectedPage[0], pageCount);
        return body;
    }

    private FrameLayout createGridHost(GridLayout grid, boolean weighted) {
        FrameLayout host = new FrameLayout(requireContext());
        LinearLayout.LayoutParams params = weighted
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        host.setLayoutParams(params);
        host.addView(grid);
        return host;
    }

    private TextView createPageArrow(String text, int contentDescriptionRes) {
        TextView arrow = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(26), dp(56));
        arrow.setLayoutParams(params);
        arrow.setGravity(Gravity.CENTER);
        arrow.setIncludeFontPadding(false);
        arrow.setText(text);
        arrow.setTextSize(22);
        arrow.setTypeface(arrow.getTypeface(), android.graphics.Typeface.BOLD);
        arrow.setContentDescription(getString(contentDescriptionRes));
        arrow.setClickable(true);
        arrow.setFocusable(true);
        return arrow;
    }

    private void updatePageArrows(TextView previous, TextView next, int selectedPage, int pageCount) {
        updatePageArrow(previous, selectedPage > 0);
        updatePageArrow(next, selectedPage < pageCount - 1);
    }

    private void updatePageArrow(TextView arrow, boolean enabled) {
        arrow.setEnabled(enabled);
        arrow.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        arrow.setTextColor(requireContext().getColor(R.color.brand_orange_dark));
    }

    private void bindPageGrid(
            GridLayout grid,
            HabitItem habit,
            List<LocalDate> dueDates,
            @Nullable Map<LocalDate, HabitLog> logsByDate,
            List<LocalDate> streakDates,
            int pageIndex
    ) {
        grid.removeAllViews();
        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, dueDates.size());
        if (start >= dueDates.size()) {
            return;
        }
        for (int index = start; index < end; index++) {
            LocalDate date = dueDates.get(index);
            boolean streak = streakDates.contains(date);
            grid.addView(dayCell(habit, date, logsByDate, streak), cellParams());
        }
    }

    private View dayCell(HabitItem habit, LocalDate date, @Nullable Map<LocalDate, HabitLog> logsByDate, boolean streak) {
        int drawableRes;
        if (date.isAfter(selectedDate)) {
            drawableRes = R.drawable.habit_report_snowman_empty;
        } else if (streak) {
            drawableRes = R.drawable.habit_report_snowman_hot;
        } else {
            HabitLog log = logsByDate == null ? null : logsByDate.get(date);
            drawableRes = HabitScheduleUtils.evaluateCompletion(habit, log)
                    ? R.drawable.habit_report_snowman_blue
                    : R.drawable.habit_report_snowman_gray;
        }
        return snowmanCell(drawableRes, date);
    }

    private View snowmanCell(int drawableRes, LocalDate date) {
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setGravity(Gravity.CENTER);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(0, dp(2), 0, 0);

        ImageView imageView = new ImageView(requireContext());
        imageView.setAdjustViewBounds(true);
        imageView.setImageResource(drawableRes);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cell.addView(imageView, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView dateLabel = new TextView(requireContext());
        dateLabel.setGravity(Gravity.CENTER);
        dateLabel.setIncludeFontPadding(false);
        dateLabel.setText(formatSnowmanDate(date));
        dateLabel.setTextColor(requireContext().getColor(R.color.text_secondary));
        dateLabel.setTextSize(10);
        cell.addView(dateLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return cell;
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(38);
        params.height = dp(56);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
        params.setMargins(dp(1), dp(4), dp(1), dp(4));
        return params;
    }

    private GradientDrawable headerDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(adjustAlpha(color, 0.82f));
        float radius = dp(22);
        drawable.setCornerRadii(new float[]{
                radius, radius,
                radius, radius,
                0f, 0f,
                0f, 0f
        });
        return drawable;
    }

    private List<LocalDate> buildDueDates(HabitItem habit) {
        List<LocalDate> dueDates = new ArrayList<>();
        if (habit == null || habit.getStartDate() == null) {
            return dueDates;
        }
        LocalDate reportEnd = habit.hasEndDate() && habit.getEndDate() != null
                ? habit.getEndDate()
                : selectedDate;
        for (LocalDate cursor = habit.getStartDate(); !cursor.isAfter(reportEnd); cursor = cursor.plusDays(1)) {
            dueDates.add(cursor);
        }
        return dueDates;
    }

    private int resolvePageCount(int dueCount) {
        return Math.max(1, (int) Math.ceil(dueCount / (float) PAGE_SIZE));
    }

    private int resolveInitialPage(List<LocalDate> dueDates, int pageCount) {
        int selectedIndex = -1;
        for (int index = 0; index < dueDates.size(); index++) {
            LocalDate date = dueDates.get(index);
            if (date.isAfter(selectedDate)) {
                break;
            }
            selectedIndex = index;
        }
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        return Math.max(0, Math.min(pageCount - 1, selectedIndex / PAGE_SIZE));
    }

    private List<LocalDate> collectStreakDates(
            HabitItem habit,
            List<LocalDate> dueDates,
            @Nullable Map<LocalDate, HabitLog> logsByDate
    ) {
        List<LocalDate> streakDates = new ArrayList<>();
        int streakCount = 0;
        for (LocalDate date : dueDates) {
            if (date.isAfter(selectedDate)) {
                break;
            }
            HabitLog log = logsByDate == null ? null : logsByDate.get(date);
            if (!HabitScheduleUtils.evaluateCompletion(habit, log)) {
                streakCount = 0;
                continue;
            }
            streakCount++;
            if (streakCount >= 2) {
                streakDates.add(date);
            }
        }
        return streakDates;
    }

    private String formatSnowmanDate(LocalDate date) {
        return String.format(Locale.US, "%d/%d", date.getDayOfMonth(), date.getMonthValue());
    }

    private String completionText(HabitItem habit, List<LocalDate> dueDates, @Nullable Map<LocalDate, HabitLog> logsByDate) {
        int trackedCount = 0;
        int completedCount = 0;
        for (LocalDate date : dueDates) {
            if (date.isAfter(selectedDate)) {
                continue;
            }
            trackedCount++;
            HabitLog log = logsByDate == null ? null : logsByDate.get(date);
            if (HabitScheduleUtils.evaluateCompletion(habit, log)) {
                completedCount++;
            }
        }
        return String.format(Locale.US, "%d/%d", completedCount, trackedCount);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);
        return android.graphics.Color.argb(alpha, red, green, blue);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
