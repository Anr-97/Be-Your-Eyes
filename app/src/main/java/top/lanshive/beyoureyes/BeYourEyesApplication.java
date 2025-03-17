package top.lanshive.beyoureyes;

import android.app.Application;
import android.content.SharedPreferences;

public class BeYourEyesApplication extends Application {
    public static final String ROLE_BLIND = "HELPER";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";
    
    private static BeYourEyesApplication instance;
    private SharedPreferences preferences;
    private String currentUserRole;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        // 从 SharedPreferences 加载用户身份，默认为志愿者
        currentUserRole = preferences.getString("user_role", ROLE_VOLUNTEER);
    }

    public static BeYourEyesApplication getInstance() {
        return instance;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
        // 保存到 SharedPreferences
        preferences.edit().putString("user_role", role).apply();
    }

    public boolean isBlindUser() {
        return ROLE_BLIND.equals(currentUserRole);
    }

    public boolean isVolunteer() {
        return ROLE_VOLUNTEER.equals(currentUserRole);
    }
} 