package com.example.finalproject.ui.common;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("'tháng' M 'năm' yyyy", Locale.US);

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
        selectedDateLabel.setText(selectedDate.getDayOfMonth() + " Th" + selectedDate.getMonthValue() + ", " + selectedDate.getYear());
        monthLabel.setText(visibleMonth.format(monthFormatter));
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
            holder.textView.setText(String.valueOf(date.getDayOfMonth()));
            holder.textView.setTextColor(date.getMonth().equals(visibleMonth.getMonth())
                    ? holder.itemView.getContext().getColor(R.color.text_primary)
                    : holder.itemView.getContext().getColor(R.color.text_muted));
            holder.textView.setBackground(selected
                    ? UiUtils.rounded(holder.itemView.getContext().getColor(R.color.brand_orange), 20, holder.itemView.getContext())
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
