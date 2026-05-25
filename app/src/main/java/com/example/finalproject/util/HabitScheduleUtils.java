package com.example.finalproject.util;

import com.example.finalproject.model.HabitItem;
import com.example.finalproject.model.HabitLog;
import com.example.finalproject.model.RecurrenceRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public final class HabitScheduleUtils {
    private HabitScheduleUtils() {
    }

    public static boolean isArchivedOnDate(HabitItem habit, LocalDate date) {
        if (habit == null || date == null) {
            return false;
        }
        if (HabitItem.STATUS_PAUSED.equals(habit.getStatus())) {
            return true;
        }
        return habit.hasEndDate()
                && habit.getEndDate() != null
                && habit.getEndDate().isBefore(date);
    }

    public static boolean overlapsMonth(HabitItem habit, LocalDate monthDate) {
        if (habit == null || monthDate == null || habit.getStartDate() == null) {
            return false;
        }
        LocalDate monthStart = monthDate.withDayOfMonth(1);
        LocalDate monthEnd = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
        if (habit.getStartDate().isAfter(monthEnd)) {
            return false;
        }
        if (habit.hasEndDate() && habit.getEndDate() != null && habit.getEndDate().isBefore(monthStart)) {
            return false;
        }
        return !HabitItem.STATUS_PAUSED.equals(habit.getStatus());
    }

    public static boolean isDueOnDate(HabitItem habit, LocalDate date) {
        if (habit == null || date == null || habit.getStartDate() == null) {
            return false;
        }
        if (date.isBefore(habit.getStartDate())) {
            return false;
        }
        if (habit.hasEndDate() && habit.getEndDate() != null && date.isAfter(habit.getEndDate())) {
            return false;
        }
        RecurrenceRule rule = habit.getRecurrenceRule();
        if (rule == null || rule.isNone()) {
            return habit.getStartDate().equals(date);
        }
        if (RecurrenceRule.END_DATE.equals(rule.getEndType())
                && rule.getEndDate() != null
                && date.isAfter(rule.getEndDate())) {
            return false;
        }
        if (!matchesPattern(habit, date)) {
            return false;
        }
        if (RecurrenceRule.END_COUNT.equals(rule.getEndType())
                && rule.getOccurrenceCount() != null
                && rule.getOccurrenceCount() > 0) {
            return occurrenceIndex(habit, date) <= rule.getOccurrenceCount();
        }
        return true;
    }

    public static boolean evaluateCompletion(HabitItem habit, HabitLog log) {
        if (habit == null || log == null) {
            return false;
        }
        if (!habit.isNumberEvaluation()) {
            return log.isCompleted();
        }
        Double actualValue = log.getActualValue();
        Double targetValue = habit.getTargetValue();
        if (actualValue == null || targetValue == null) {
            return false;
        }
        String operator = habit.getTargetOperator();
        if (HabitItem.OPERATOR_AT_MOST.equals(operator)) {
            return actualValue <= targetValue;
        }
        if (HabitItem.OPERATOR_EXACTLY.equals(operator)) {
            return Math.abs(actualValue - targetValue) < 0.0001d;
        }
        return actualValue >= targetValue;
    }

    public static float progressFraction(HabitItem habit, HabitLog log) {
        if (habit == null) {
            return 0f;
        }
        if (!habit.isNumberEvaluation()) {
            return evaluateCompletion(habit, log) ? 1f : 0f;
        }
        if (log == null || log.getActualValue() == null || habit.getTargetValue() == null || habit.getTargetValue() <= 0d) {
            return 0f;
        }
        double actualValue = log.getActualValue();
        double targetValue = habit.getTargetValue();
        if (HabitItem.OPERATOR_AT_MOST.equals(habit.getTargetOperator())) {
            if (actualValue <= targetValue) {
                return 1f;
            }
            double overflow = actualValue - targetValue;
            double fraction = 1d - (overflow / targetValue);
            return clampFraction(fraction);
        }
        double fraction = actualValue / targetValue;
        return clampFraction(fraction);
    }

    public static String formatNumber(Double value) {
        if (value == null) {
            return "";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001d) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static boolean matchesPattern(HabitItem habit, LocalDate date) {
        RecurrenceRule rule = habit.getRecurrenceRule();
        LocalDate start = habit.getStartDate();
        int interval = Math.max(1, rule.getIntervalValue());
        if (RecurrenceRule.FREQ_DAILY.equals(rule.getFreq())) {
            long days = ChronoUnit.DAYS.between(start, date);
            return days >= 0 && days % interval == 0;
        }
        if (RecurrenceRule.FREQ_WEEKLY.equals(rule.getFreq())) {
            List<String> days = weeklyDays(rule, start.getDayOfWeek());
            int weekOffset = (int) ChronoUnit.WEEKS.between(start.with(DayOfWeek.MONDAY), date.with(DayOfWeek.MONDAY));
            return weekOffset >= 0
                    && weekOffset % interval == 0
                    && days.contains(dayCode(date.getDayOfWeek()));
        }
        if (RecurrenceRule.FREQ_MONTHLY.equals(rule.getFreq())) {
            long months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), date.withDayOfMonth(1));
            int dayOfMonth = rule.getByMonthDay() == null ? start.getDayOfMonth() : rule.getByMonthDay();
            dayOfMonth = Math.min(dayOfMonth, date.lengthOfMonth());
            return months >= 0 && months % interval == 0 && date.getDayOfMonth() == dayOfMonth;
        }
        if (RecurrenceRule.FREQ_YEARLY.equals(rule.getFreq())) {
            long years = ChronoUnit.YEARS.between(start.withDayOfYear(1), date.withDayOfYear(1));
            int monthValue = rule.getByMonth() == null ? start.getMonthValue() : rule.getByMonth();
            int monthDay = rule.getByMonthDay() == null ? start.getDayOfMonth() : rule.getByMonthDay();
            return years >= 0
                    && years % interval == 0
                    && date.getMonthValue() == monthValue
                    && date.getDayOfMonth() == Math.min(monthDay, date.lengthOfMonth());
        }
        return false;
    }

    private static int occurrenceIndex(HabitItem habit, LocalDate targetDate) {
        int count = 0;
        for (LocalDate cursor = habit.getStartDate(); !cursor.isAfter(targetDate); cursor = cursor.plusDays(1)) {
            if (matchesPattern(habit, cursor)) {
                count++;
            }
        }
        return count;
    }

    private static List<String> weeklyDays(RecurrenceRule rule, DayOfWeek fallbackDay) {
        if (rule.getByDay() == null || rule.getByDay().trim().isEmpty()) {
            return Arrays.asList(dayCode(fallbackDay));
        }
        String[] split = rule.getByDay().split(",");
        for (int index = 0; index < split.length; index++) {
            split[index] = split[index].trim();
        }
        return Arrays.asList(split);
    }

    private static float clampFraction(double value) {
        return (float) Math.max(0d, Math.min(1d, value));
    }

    private static String dayCode(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "MO";
            case TUESDAY:
                return "TU";
            case WEDNESDAY:
                return "WE";
            case THURSDAY:
                return "TH";
            case FRIDAY:
                return "FR";
            case SATURDAY:
                return "SA";
            case SUNDAY:
            default:
                return "SU";
        }
    }
}
