package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class HelpStatsResponse {
    @SerializedName("totalHelpCount")
    private int totalHelpCount;

    @SerializedName("weeklyHelpCount")
    private int weeklyHelpCount;

    @SerializedName("lastWeeklyHelpCount")
    private int lastWeeklyHelpCount;

    @SerializedName("monthlyHelpCount")
    private int monthlyHelpCount;

    @SerializedName("totalHelpDuration")
    private int totalHelpDuration;

    public int getTotalHelpCount() {
        return totalHelpCount;
    }

    public void setTotalHelpCount(int totalHelpCount) {
        this.totalHelpCount = totalHelpCount;
    }

    public int getWeeklyHelpCount() {
        return weeklyHelpCount;
    }

    public void setWeeklyHelpCount(int weeklyHelpCount) {
        this.weeklyHelpCount = weeklyHelpCount;
    }

    public int getLastWeeklyHelpCount() {
        return lastWeeklyHelpCount;
    }

    public void setLastWeeklyHelpCount(int lastWeeklyHelpCount) {
        this.lastWeeklyHelpCount = lastWeeklyHelpCount;
    }

    public int getMonthlyHelpCount() {
        return monthlyHelpCount;
    }

    public void setMonthlyHelpCount(int monthlyHelpCount) {
        this.monthlyHelpCount = monthlyHelpCount;
    }

    public int getTotalHelpDuration() {
        return totalHelpDuration;
    }

    public void setTotalHelpDuration(int totalHelpDuration) {
        this.totalHelpDuration = totalHelpDuration;
    }
} 