package top.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class StatisticsResponse {
    @SerializedName("blindUsers")
    private int blindUsers;

    @SerializedName("volunteers")
    private int volunteers;

    public int getBlindUsers() {
        return blindUsers;
    }

    public void setBlindUsers(int blindUsers) {
        this.blindUsers = blindUsers;
    }

    public int getVolunteers() {
        return volunteers;
    }

    public void setVolunteers(int volunteers) {
        this.volunteers = volunteers;
    }
} 