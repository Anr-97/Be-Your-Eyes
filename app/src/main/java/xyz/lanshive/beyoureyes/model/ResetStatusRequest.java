package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class ResetStatusRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("newStatus")
    private boolean newStatus;

    @SerializedName("token")
    private String token;

    public ResetStatusRequest(String email, boolean newStatus, String token) {
        this.email = email;
        this.newStatus = newStatus;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public boolean getNewStatus() {
        return newStatus;
    }

    public String getToken() {
        return token;
    }
}
