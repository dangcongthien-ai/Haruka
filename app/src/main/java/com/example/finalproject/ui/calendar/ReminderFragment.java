package com.example.finalproject.ui.calendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.model.Reminder;
import com.example.finalproject.ui.common.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ReminderFragment extends Fragment {
    public static final String ARG_RESULT_KEY = "result_key";
    public static final String ARG_REMINDER = "reminder";
    public static final String RESULT_REMINDER = "reminder";

    private Reminder reminder;
    private String resultKey;
    private final List<TextView> checks = new ArrayList<>();

    public static ReminderFragment newInstance(String resultKey, Reminder reminder) {
        ReminderFragment fragment = new ReminderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESULT_KEY, resultKey);
        args.putSerializable(ARG_REMINDER, reminder == null ? Reminder.none() : reminder);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);
        resultKey = requireArguments().getString(ARG_RESULT_KEY);
        reminder = (Reminder) requireArguments().getSerializable(ARG_REMINDER);
        if (reminder == null) {
            reminder = Reminder.none();
        }
        bind(view);
        setupClicks(view);
        refresh();
        return view;
    }

    private void bind(View view) {
        checks.add(view.findViewById(R.id.check_event_time));
        checks.add(view.findViewById(R.id.check_1m));
        checks.add(view.findViewById(R.id.check_5m));
        checks.add(view.findViewById(R.id.check_15m));
        checks.add(view.findViewById(R.id.check_30m));
        checks.add(view.findViewById(R.id.check_1h));
        checks.add(view.findViewById(R.id.check_1d));
        checks.add(view.findViewById(R.id.check_custom));
    }

    private void setupClicks(View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> ((MainActivity) requireActivity()).finishFullScreen());
        view.findViewById(R.id.btn_save_reminder).setOnClickListener(v -> saveResult());
        view.findViewById(R.id.reminder_event_time).setOnClickListener(v -> setAtTime());
        view.findViewById(R.id.reminder_1m).setOnClickListener(v -> setBefore(1, "MINUTE"));
        view.findViewById(R.id.reminder_5m).setOnClickListener(v -> setBefore(5, "MINUTE"));
        view.findViewById(R.id.reminder_15m).setOnClickListener(v -> setBefore(15, "MINUTE"));
        view.findViewById(R.id.reminder_30m).setOnClickListener(v -> setBefore(30, "MINUTE"));
        view.findViewById(R.id.reminder_1h).setOnClickListener(v -> setBefore(1, "HOUR"));
        view.findViewById(R.id.reminder_1d).setOnClickListener(v -> setBefore(1, "DAY"));
        view.findViewById(R.id.reminder_custom).setOnClickListener(v -> showCustomDialog());
    }

    private void setAtTime() {
        reminder.setEnabled(true);
        reminder.setReminderType(Reminder.TYPE_AT_TIME);
        reminder.setOffsetValue(null);
        reminder.setOffsetUnit(null);
        refresh();
    }

    private void setBefore(int value, String unit) {
        reminder.setEnabled(true);
        reminder.setReminderType(Reminder.TYPE_BEFORE);
        reminder.setOffsetValue(value);
        reminder.setOffsetUnit(unit);
        refresh();
    }

    private void saveResult() {
        Bundle result = new Bundle();
        result.putSerializable(RESULT_REMINDER, reminder);
        getParentFragmentManager().setFragmentResult(resultKey, result);
        ((MainActivity) requireActivity()).finishFullScreen();
    }

    private void refresh() {
        UiUtils.setCheck(checks.get(0), Reminder.TYPE_AT_TIME.equals(reminder.getReminderType()) && reminder.isEnabled());
        UiUtils.setCheck(checks.get(1), matches(1, "MINUTE"));
        UiUtils.setCheck(checks.get(2), matches(5, "MINUTE"));
        UiUtils.setCheck(checks.get(3), matches(15, "MINUTE"));
        UiUtils.setCheck(checks.get(4), matches(30, "MINUTE"));
        UiUtils.setCheck(checks.get(5), matches(1, "HOUR"));
        UiUtils.setCheck(checks.get(6), matches(1, "DAY"));
        boolean custom = reminder.isEnabled()
                && Reminder.TYPE_BEFORE.equals(reminder.getReminderType())
                && !matches(1, "MINUTE")
                && !matches(5, "MINUTE")
                && !matches(15, "MINUTE")
                && !matches(30, "MINUTE")
                && !matches(1, "HOUR")
                && !matches(1, "DAY");
        UiUtils.setCheck(checks.get(7), custom);
    }

    private boolean matches(int value, String unit) {
        return reminder.isEnabled()
                && Reminder.TYPE_BEFORE.equals(reminder.getReminderType())
                && reminder.getOffsetValue() != null
                && reminder.getOffsetValue() == value
                && unit.equals(reminder.getOffsetUnit());
    }

    private void showCustomDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_reminder, null, false);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        EditText amount = view.findViewById(R.id.edit_reminder_amount);
        RadioGroup unitGroup = view.findViewById(R.id.reminder_unit_group);
        if (reminder.getOffsetValue() != null) {
            amount.setText(String.valueOf(reminder.getOffsetValue()));
        }
        view.findViewById(R.id.btn_custom_reminder_done).setOnClickListener(v -> {
            int value = parseAmount(amount);
            String unit = "MINUTE";
            int checkedId = unitGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radio_hours) {
                unit = "HOUR";
            } else if (checkedId == R.id.radio_days) {
                unit = "DAY";
            } else if (checkedId == R.id.radio_weeks) {
                unit = "WEEK";
            }
            setBefore(value, unit);
            dialog.dismiss();
        });
        dialog.show();
    }

    private int parseAmount(EditText editText) {
        try {
            return Math.max(1, Integer.parseInt(editText.getText().toString()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
