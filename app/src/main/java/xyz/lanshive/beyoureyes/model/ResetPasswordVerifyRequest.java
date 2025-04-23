package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordVerifyRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("code")
    private String code;

    @SerializedName("newPassword")
    private String newPassword;

    public ResetPasswordVerifyRequest(String email, String code, String newPassword) {
        this.email = email;
        this.code = code;
        this.newPassword = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
} 