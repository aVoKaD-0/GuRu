package com.ruege.mobile.ui.exercises;

public class ExerciseItem {
    private String title;
    private String description;
    private String difficulty;
    private int maxPoints;
    private String taskId;
    private String contentId;

    public ExerciseItem(String title, String description, String difficulty, int maxPoints, String taskId) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.maxPoints = maxPoints;
        this.taskId = taskId;
    }

    public ExerciseItem(String title, String description, String difficulty, int maxPoints, String taskId, String contentId) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.maxPoints = maxPoints;
        this.taskId = taskId;
        this.contentId = contentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
} 