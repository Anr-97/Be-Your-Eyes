package xyz.lanshive.beyoureyes.model;

public class VerificationCodeRequest {
    private String email;

    public VerificationCodeRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 