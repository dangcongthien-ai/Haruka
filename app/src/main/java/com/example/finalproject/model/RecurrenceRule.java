package com.example.finalproject.model;

import java.io.Serializable;
import java.time.LocalDate;

public class RecurrenceRule implements Serializable {
    public static final String FREQ_NONE = "NONE";
    public static final String FREQ_DAILY = "DAILY";
    public static final String FREQ_WEEKLY = "WEEKLY";
    public static final String FREQ_MONTHLY = "MONTHLY";
    public static final String FREQ_YEARLY = "YEARLY";

    public static final String END_NONE = "NONE";
    public static final String END_DATE = "DATE";
    public static final String END_COUNT = "COUNT";

    private long id;
    private String freq = FREQ_NONE;
    private int intervalValue = 1;
    private String byDay;
    private Integer byMonthDay;
    private Integer byMonth;
    private Integer bySetPos;
    private String monthlyPatternType;
    private String endType = END_NONE;
    private LocalDate endDate;
    private Integer occurrenceCount;

    public static RecurrenceRule none() {
        return new RecurrenceRule();
    }

    public boolean isNone() {
        return freq == null || FREQ_NONE.equals(freq);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFreq() {
        return freq;
    }

    public void setFreq(String freq) {
        this.freq = freq;
    }

    public int getIntervalValue() {
        return intervalValue;
    }

    public void setIntervalValue(int intervalValue) {
        this.intervalValue = Math.max(1, intervalValue);
    }

    public String getByDay() {
        return byDay;
    }

    public void setByDay(String byDay) {
        this.byDay = byDay;
    }

    public Integer getByMonthDay() {
        return byMonthDay;
    }

    public void setByMonthDay(Integer byMonthDay) {
        this.byMonthDay = byMonthDay;
    }

    public Integer getByMonth() {
        return byMonth;
    }

    public void setByMonth(Integer byMonth) {
        this.byMonth = byMonth;
    }

    public Integer getBySetPos() {
        return bySetPos;
    }

    public void setBySetPos(Integer bySetPos) {
        this.bySetPos = bySetPos;
    }

    public String getMonthlyPatternType() {
        return monthlyPatternType;
    }

    public void setMonthlyPatternType(String monthlyPatternType) {
        this.monthlyPatternType = monthlyPatternType;
    }

    public String getEndType() {
        return endType;
    }

    public void setEndType(String endType) {
        this.endType = endType;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getDisplayText() {
        if (isNone()) {
            return "Không";
        }
        if (FREQ_DAILY.equals(freq) && intervalValue == 1) {
            return "Hằng ngày";
        }
        if (FREQ_WEEKLY.equals(freq) && intervalValue == 1) {
            return "Hằng tuần";
        }
        if (FREQ_MONTHLY.equals(freq) && intervalValue == 1) {
            return "Hằng tháng";
        }
        if (FREQ_YEARLY.equals(freq) && intervalValue == 1) {
            return "Hằng năm";
        }
        return "Tùy chỉnh";
    }
}
