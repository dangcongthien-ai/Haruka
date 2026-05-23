package com.example.finalproject.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalEntry {
    private long id;
    private long userId;
    private Long layoutId;
    private LocalDate journalDate;
    private String title;
    private String caption;
    private String content;
    private String createdAt;
    private String updatedAt;
    private List<String> imageUris = new ArrayList<>();
    private List<String> moodResourceNames = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public void setLayoutId(Long layoutId) {
        this.layoutId = layoutId;
    }

    public LocalDate getJournalDate() {
        return journalDate;
    }

    public void setJournalDate(LocalDate journalDate) {
        this.journalDate = journalDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getImageUris() {
        return imageUris;
    }

    public void setImageUris(List<String> imageUris) {
        this.imageUris = imageUris == null ? new ArrayList<>() : new ArrayList<>(imageUris);
    }

    public List<String> getMoodResourceNames() {
        return moodResourceNames;
    }

    public void setMoodResourceNames(List<String> moodResourceNames) {
        this.moodResourceNames = moodResourceNames == null ? new ArrayList<>() : new ArrayList<>(moodResourceNames);
    }
}
