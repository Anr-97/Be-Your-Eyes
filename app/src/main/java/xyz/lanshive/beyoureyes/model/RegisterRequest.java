package xyz.lanshive.beyoureyes.model;

public class RegisterRequest {
    private String email;
    private String password;
    private String userType;
    private String code;

    public RegisterRequest(String email, String password, String userType, String code) {
        this.email = email;
        this.password = password;
        this.userType = userType;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
} 