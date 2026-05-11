package com.example.finalproject.model;

import java.io.Serializable;
import java.time.LocalTime;

public class Reminder implements Serializable {
    public static final String TYPE_NONE = "NONE";
    public static final String TYPE_AT_TIME = "AT_TIME";
    public static final String TYPE_BEFORE = "BEFORE";

    private long id;
    private String reminderType = TYPE_NONE;
    private Integer offsetValue;
    private String offsetUnit;
    private LocalTime timeOfDay;
    private boolean enabled;

    public static Reminder none() {
        return new Reminder();
    }

    public boolean isNone() {
        return !enabled || reminderType == null || TYPE_NONE.equals(reminderType);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public Integer getOffsetValue() {
        return offsetValue;
    }

    public void setOffsetValue(Integer offsetValue) {
        this.offsetValue = offsetValue;
    }

    public String getOffsetUnit() {
        return offsetUnit;
    }

    public void setOffsetUnit(String offsetUnit) {
        this.offsetUnit = offsetUnit;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDisplayText() {
        if (isNone()) {
            return "Không";
        }
        if (TYPE_AT_TIME.equals(reminderType)) {
            return "Thời gian sự kiện";
        }
        String unit = "phút";
        if ("HOUR".equals(offsetUnit)) {
            unit = "giờ";
        } else if ("DAY".equals(offsetUnit)) {
            unit = "ngày";
        } else if ("WEEK".equals(offsetUnit)) {
            unit = "tuần";
        }
        return "Trước " + (offsetValue == null ? 0 : offsetValue) + " " + unit;
    }
}
