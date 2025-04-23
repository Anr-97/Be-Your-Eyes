package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 