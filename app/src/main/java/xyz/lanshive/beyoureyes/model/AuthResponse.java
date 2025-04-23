package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("token")
    private String token;
    
    @SerializedName("refreshToken")
    private String refreshToken;
    
    @SerializedName("user")
    private User user;
    
    @SerializedName("message")
    private String message;

    @SerializedName("role")
    private String role;

    public AuthResponse(boolean success, String message, String role) {
        this.status = success ? "success" : "failure";
        this.message = message;
        this.role = role;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public String getRole() {
        return role;
    }

    public static class User {
        @SerializedName("id")
        private String id;
        
        @SerializedName("email")
        private String email;
        
        @SerializedName("userType")
        private String userType;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getUserType() {
            return userType;
        }
    }
} 