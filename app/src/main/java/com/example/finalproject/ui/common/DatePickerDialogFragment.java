package com.example.finalproject.ui.common;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DatePickerDialogFragment extends DialogFragment {
    public static final String ARG_RESULT_KEY = "result_key";
    public static final String ARG_INITIAL_DATE = "initial_date";
    public static final String RESULT_DATE = "date";

    private TextView selectedDateLabel;
    private TextView monthLabel;
    private PickerDayAdapter adapter;
    private LocalDate selectedDate;
    private LocalDate visibleMonth;
    private String resultKey;

    public static DatePickerDialogFragment newInstance(String resultKey, LocalDate initialDate) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESULT_KEY, resultKey);
        args.putString(ARG_INITIAL_DATE, DateTimeUtils.dateToIso(initialDate));
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_picker, null, false);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(UiUtils.dp(requireContext(), 344), ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
            attributes.dimAmount = 0.28f;
            dialog.getWindow().setAttributes(attributes);
        }
        setup(view);
        return dialog;
    }

    private void setup(View view) {
        Bundle args = requireArguments();
        resultKey = args.getString(ARG_RESULT_KEY);
        selectedDate = DateTimeUtils.isoToDate(args.getString(ARG_INITIAL_DATE));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        visibleMonth = selectedDate.withDayOfMonth(1);
        selectedDateLabel = view.findViewById(R.id.tv_dialog_selected_date);
        monthLabel = view.findViewById(R.id.tv_dialog_month);
        adapter = new PickerDayAdapter(date -> {
            selectedDate = date;
            visibleMonth = date.withDayOfMonth(1);
            refresh();
        });
        RecyclerView recyclerView = view.findViewById(R.id.dialog_date_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        ImageButton previous = view.findViewById(R.id.btn_dialog_prev);
        ImageButton next = view.findViewById(R.id.btn_dialog_next);
        previous.setOnClickListener(v -> {
            visibleMonth = visibleMonth.minusMonths(1);
            refresh();
        });
        next.setOnClickListener(v -> {
            visibleMonth = visibleMonth.plusMonths(1);
            refresh();
        });
        monthLabel.setOnClickListener(v -> showMonthPicker());
        view.findViewById(R.id.btn_dialog_edit).setOnClickListener(v -> showMonthPicker());
        view.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_dialog_ok).setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_DATE, DateTimeUtils.dateToIso(selectedDate));
            getParentFragmentManager().setFragmentResult(resultKey, result);
            dismiss();
        });
        refresh();
    }

    private void refresh() {
        selectedDateLabel.setText(getString(
                R.string.date_picker_selected_format,
                selectedDate.getDayOfMonth(),
                selectedDate.getMonthValue(),
                selectedDate.getYear()
        ));
        monthLabel.setText(getString(
                R.string.date_picker_month_year,
                visibleMonth.getMonthValue(),
                visibleMonth.getYear()
        ));
        adapter.submit(buildDays(), selectedDate, visibleMonth);
    }

    private List<LocalDate> buildDays() {
        LocalDate first = YearMonth.from(visibleMonth).atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            days.add(start.plusDays(i));
        }
        return days;
    }

    private void showMonthPicker() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView yearLabel = content.findViewById(R.id.tv_picker_year);
        GridLayout monthGrid = content.findViewById(R.id.month_picker_grid);
        YearMonth[] draft = {YearMonth.from(selectedDate)};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            yearLabel.setText(String.valueOf(draft[0].getYear()));
            monthGrid.removeAllViews();
            for (int month = 1; month <= 12; month++) {
                TextView chip = new TextView(requireContext());
                chip.setText(getString(R.string.month_chip_format, month));
                chip.setGravity(android.view.Gravity.CENTER);
                chip.setSingleLine(true);
                chip.setTextSize(14);
                boolean selected = draft[0].getMonthValue() == month;
                chip.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.text_primary));
                chip.setBackground(selected
                        ? UiUtils.rounded(requireContext().getColor(R.color.brand_orange), 8, requireContext())
                        : UiUtils.roundedStroke(
                                requireContext().getColor(R.color.surface),
                                requireContext().getColor(R.color.line),
                                8,
                                requireContext()
                        ));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec((month - 1) / 3),
                        GridLayout.spec((month - 1) % 3)
                );
                params.width = UiUtils.dp(requireContext(), 88);
                params.height = UiUtils.dp(requireContext(), 40);
                int margin = UiUtils.dp(requireContext(), 3);
                params.setMargins(margin, UiUtils.dp(requireContext(), 4), margin, UiUtils.dp(requireContext(), 4));
                chip.setLayoutParams(params);
                int pickedMonth = month;
                chip.setOnClickListener(v -> {
                    draft[0] = YearMonth.of(draft[0].getYear(), pickedMonth);
                    render[0].run();
                });
                monthGrid.addView(chip);
            }
        };

        content.findViewById(R.id.btn_picker_prev_year).setOnClickListener(v -> {
            draft[0] = draft[0].minusYears(1);
            render[0].run();
        });
        content.findViewById(R.id.btn_picker_next_year).setOnClickListener(v -> {
            draft[0] = draft[0].plusYears(1);
            render[0].run();
        });
        yearLabel.setOnClickListener(v -> showYearPicker(draft[0].getYear(), year -> {
            draft[0] = YearMonth.of(year, draft[0].getMonthValue());
            render[0].run();
        }));
        content.findViewById(R.id.btn_month_picker_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_month_picker_ok).setOnClickListener(v -> {
            visibleMonth = draft[0].atDay(1);
            selectedDate = visibleMonth.withDayOfMonth(Math.min(selectedDate.getDayOfMonth(), draft[0].lengthOfMonth()));
            refresh();
            dialog.dismiss();
        });
        render[0].run();
        dialog.show();
    }

    private void showYearPicker(int selectedYear, YearPickerListener listener) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_year_picker, null, false);
        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        NumberPicker picker = content.findViewById(R.id.picker_year);
        int minYear = Math.max(1900, selectedYear - 100);
        int maxYear = Math.min(2200, selectedYear + 100);
        picker.setMinValue(minYear);
        picker.setMaxValue(maxYear);
        picker.setValue(selectedYear);
        picker.setWrapSelectorWheel(false);
        UiUtils.styleNumberPicker(picker, requireContext());

        content.findViewById(R.id.btn_year_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btn_year_ok).setOnClickListener(v -> {
            listener.onYearPicked(picker.getValue());
            dialog.dismiss();
        });
        dialog.show();
    }

    private interface YearPickerListener {
        void onYearPicked(int year);
    }

    private static class PickerDayAdapter extends RecyclerView.Adapter<PickerDayAdapter.DayViewHolder> {
        interface Listener {
            void onClick(LocalDate date);
        }

        private final List<LocalDate> days = new ArrayList<>();
        private final Listener listener;
        private LocalDate selectedDate;
        private LocalDate visibleMonth;

        PickerDayAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<LocalDate> newDays, LocalDate selectedDate, LocalDate visibleMonth) {
            days.clear();
            days.addAll(newDays);
            this.selectedDate = selectedDate;
            this.visibleMonth = visibleMonth;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_picker_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            LocalDate date = days.get(position);
            boolean selected = date.equals(selectedDate);
            boolean inVisibleMonth = date.getMonth().equals(visibleMonth.getMonth())
                    && date.getYear() == visibleMonth.getYear();
            holder.textView.setText(String.valueOf(date.getDayOfMonth()));
            holder.textView.setTextColor(inVisibleMonth
                    ? holder.itemView.getContext().getColor(R.color.text_primary)
                    : holder.itemView.getContext().getColor(R.color.text_muted));
            holder.textView.setBackground(selected
                    ? UiUtils.rounded(holder.itemView.getContext().getColor(R.color.brand_orange), 18, holder.itemView.getContext())
                    : null);
            if (selected) {
                holder.textView.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            }
            holder.itemView.setOnClickListener(v -> listener.onClick(date));
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        static class DayViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_picker_day);
            }
        }
    }
}
