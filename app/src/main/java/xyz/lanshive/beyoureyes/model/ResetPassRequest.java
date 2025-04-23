package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class ResetPassRequest {
    @SerializedName("email")
    private String email;

    public ResetPassRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 