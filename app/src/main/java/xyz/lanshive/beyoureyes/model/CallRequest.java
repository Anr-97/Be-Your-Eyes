package xyz.lanshive.beyoureyes.model;

public class CallRequest {
    private String email;
    private String token;
    private String type; // "blind" or "volunteer"

    public CallRequest(String email, String token, String type) {
        this.email = email;
        this.token = token;
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
} 