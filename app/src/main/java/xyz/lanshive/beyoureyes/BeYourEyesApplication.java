package xyz.lanshive.beyoureyes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import xyz.lanshive.beyoureyes.socket.SignalingClient;
import xyz.lanshive.beyoureyes.socket.SignalingListener;

public class BeYourEyesApplication extends Application implements SignalingListener {
    private static final String TAG = "BeYourEyesApplication";
    private static final String PREF_AUTH_TOKEN = "auth_token";
    private static final String PREF_REFRESH_TOKEN = "refresh_token";
    private static final String PREF_USER_EMAIL = "user_email";
    private static final String PREF_USER_ROLE = "user_role";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final String PREF_ONLINE_STATUS = "online_status";

    public static final String ROLE_BLIND = "HELPER";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";

    private static BeYourEyesApplication instance;
    private SharedPreferences preferences;
    private String currentUserRole;
    private boolean isVolunteerOnline = false;
    private SignalingClient signalingClient;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentUserRole = preferences.getString(PREF_USER_ROLE, null);
        Log.d(TAG, "Application onCreate - 当前用户角色: " + currentUserRole);
        initializeSignalingClient();
    }

    private void initializeSignalingClient() {
        if (signalingClient == null) {
            signalingClient = new SignalingClient(this);
            // 如果是志愿者，自动连接信令服务器
            if (isVolunteer()) {
                connectToSignalingServer();
            }
        }
    }

    private void connectToSignalingServer() {
        String email = getUserEmail();
        String token = getAuthToken();
        if (email != null && token != null) {
            signalingClient.connect("volunteer_" + email, token);
            //setVolunteerOnline(true);
        }
    }

    // SignalingListener接口实现
    // SignalingListener接口方法实现
    @Override
    public void onConnected() {
        Log.d(TAG, "信令服务器已连接");
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "信令服务器已断开连接");
    }

    @Override
    public void onRemoteSdpReceived(SessionDescription sdp) {
        Log.d(TAG, "收到远程SDP");
        // 应用层逻辑...
    }

    @Override
    public void onRemoteIceCandidateReceived(IceCandidate candidate) {
        Log.d(TAG, "收到远程ICE候选");
        // 应用层逻辑...
    }

    @Override
    public void onCallAccepted(String roomId) {
        Log.d(TAG, "通话请求已被接受，房间ID: " + roomId);
        // 处理通话接受逻辑...
    }

    // 新增的接口方法实现
    @Override
    public void onCallRejected(String reason) {
        Log.d(TAG, "通话请求被拒绝: " + reason);
        // 处理通话拒绝逻辑...
    }

    @Override
    public void onCallQueued(int position) {
        Log.d(TAG, "通话请求已进入队列，位置: " + position);
        // 处理通话排队逻辑...
    }

    @Override
    public void onCallTimeout() {
        Log.d(TAG, "通话请求超时");
        // 处理通话超时逻辑...
    }

    public static BeYourEyesApplication getInstance() {
        return instance;
    }

    public SignalingClient getSignalingClient() {
        return signalingClient;
    }

    public void setSignalingClient(SignalingClient client) {
        this.signalingClient = client;
    }

    public void setAuthToken(String token) {
        preferences.edit().putString(PREF_AUTH_TOKEN, token).apply();
        Log.d(TAG, "已保存Auth Token");
    }

    public String getAuthToken() {
        return preferences.getString(PREF_AUTH_TOKEN, null);
    }

    public void setRefreshToken(String refreshToken) {
        preferences.edit().putString(PREF_REFRESH_TOKEN, refreshToken).apply();
        Log.d(TAG, "已保存Refresh Token");
    }

    public String getRefreshToken() {
        return preferences.getString(PREF_REFRESH_TOKEN, null);
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(PREF_USER_EMAIL, email).apply();
        Log.d(TAG, "已保存用户邮箱: " + email);
    }

    public String getUserEmail() {
        return preferences.getString(PREF_USER_EMAIL, null);
    }

    public void setCurrentUserRole(String role) {
        currentUserRole = role;
        preferences.edit().putString(PREF_USER_ROLE, role).apply();
        Log.d(TAG, "已保存用户角色: " + role);
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public boolean isLoggedIn() {
        boolean isLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);
        Log.d(TAG, "检查登录状态: " + isLoggedIn);
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        preferences.edit().putBoolean(PREF_IS_LOGGED_IN, loggedIn).apply();
        Log.d(TAG, "设置登录状态: " + loggedIn);
    }

    public boolean isOnline() {
        boolean isOnline = preferences.getBoolean(PREF_ONLINE_STATUS, true);
        Log.d(TAG, "获取在线状态: " + isOnline);
        return isOnline;
    }

    public void setOnline(boolean online) {

    }

    public void logout() {
        preferences.edit()
                .remove(PREF_AUTH_TOKEN)
                .remove(PREF_REFRESH_TOKEN)
                .remove(PREF_USER_EMAIL)
                .remove(PREF_USER_ROLE)
                .putBoolean(PREF_IS_LOGGED_IN, false)
                .putBoolean(PREF_ONLINE_STATUS, false)
                .apply();
        currentUserRole = null;
        Log.d(TAG, "用户已登出，清除所有登录信息");
    }

    public void clearUserData() {
        preferences.edit()
                .remove(PREF_USER_ROLE)
                .remove(PREF_USER_EMAIL)
                .remove(PREF_IS_LOGGED_IN)
                .remove(PREF_AUTH_TOKEN)
                .remove(PREF_REFRESH_TOKEN)
                .apply();
        Log.d(TAG, "清除所有用户数据");
    }

    public boolean isBlindUser() {
        return ROLE_BLIND.equals(getCurrentUserRole());
    }

    public boolean isVolunteer() {
        return ROLE_VOLUNTEER.equals(getCurrentUserRole());
    }

    public boolean isVolunteerOnline() {
        return isVolunteerOnline;
    }

public void setVolunteerOnline(boolean online) {
    this.isVolunteerOnline = online;
    preferences.edit().putBoolean(PREF_ONLINE_STATUS, online).apply();
        Log.d(TAG, "设置在线状态: " + online);
    if (signalingClient != null && signalingClient.getSocket() != null && isVolunteer()) {
        try {
            JSONObject data = new JSONObject();
            String email = getUserEmail();
if (email != null) {
    data.put("userId", email);  // 改为userId，而不是email
    data.put("userType", "volunteer");  // 改为userType，而不是type

    // 添加详细日志，检查数据内容
    Log.d(TAG, "准备发送状态变更数据: " + data.toString());

    if (online) {
        signalingClient.getSocket().emit("user_online", data);
        Log.d(TAG, "已发送志愿者上线事件，数据: " + data.toString());
    } else {
        signalingClient.getSocket().emit("user_offline", data);
        Log.d(TAG, "已发送志愿者下线事件，数据: " + data.toString());
    }
    Log.d(TAG, "发送志愿者状态变更: " + (online ? "在线" : "离线"));
} else {
    Log.e(TAG, "无法设置志愿者状态：用户邮箱为空");
}
        } catch (JSONException e) {
            Log.e(TAG, "设置志愿者在线状态失败", e);
        }
    }
}
} 