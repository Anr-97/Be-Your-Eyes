package xyz.lanshive.beyoureyes.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;
import xyz.lanshive.beyoureyes.CallActivity;
import xyz.lanshive.beyoureyes.R;
import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.model.ResetStatusRequest;
import xyz.lanshive.beyoureyes.model.ResetStatusResponse;
import xyz.lanshive.beyoureyes.model.StatisticsResponse;
import xyz.lanshive.beyoureyes.model.RefreshTokenRequest;
import xyz.lanshive.beyoureyes.model.RefreshTokenResponse;
import xyz.lanshive.beyoureyes.model.HelpStatsResponse;
import xyz.lanshive.beyoureyes.HelpRecordsActivity;
import xyz.lanshive.beyoureyes.model.HelpRecordsResponse;
import xyz.lanshive.beyoureyes.model.HelpRecord;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final long POLLING_INTERVAL = 10000; // 10秒轮询间隔
    private static final String PREF_VOLUNTEER_ONLINE = "pref_volunteer_online";

    private boolean isBlind;
    private View rootView;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private Call<StatisticsResponse> currentCall;
    private Call<ResetStatusResponse> statusCall;

    // 平台统计视图
    private TextView volunteerCount;
    private TextView onlineVolunteerCount;
    private TextView blindUserCount;
    private TextView weeklyHelpCount;
    private TextView weeklyHelpIncrease;
    private View statsContainer;

    // 个人帮助统计视图
    private TextView totalHelpCount;
    private TextView monthlyHelpCount;
    private TextView totalHours;
    //刷新token
    private boolean isRefreshingToken = false;
    private int retryCount = 0;
    private static final int MAX_RETRY = 2;

    private TextView recentUserNameText;
    private TextView recentHelpInfoText;
    private TextView recentHelpTimeText;
    private View recentHelpLayout;
    private SwitchMaterial onlineSwitch;

    private SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isBlind = BeYourEyesApplication.getInstance().getCurrentUserRole()
                .equals(BeYourEyesApplication.ROLE_BLIND);
        preferences = getContext().getSharedPreferences("BeYourEyesPrefs", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        initViews();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 只有志愿者页面才需要轮询统计数据
        if (!isBlind) {
            startPolling();
        } else {
            // 盲人页面只在创建时获取一次统计数据
            loadStatistics();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    private void initViews() {
        LinearLayout blindLayout = rootView.findViewById(R.id.blindLayout);
        LinearLayout volunteerLayout = rootView.findViewById(R.id.volunteerLayout);

        if (isBlind) {
            blindLayout.setVisibility(View.VISIBLE);
            volunteerLayout.setVisibility(View.GONE);
            setupBlindViews();
        } else {
            blindLayout.setVisibility(View.GONE);
            volunteerLayout.setVisibility(View.VISIBLE);
            setupVolunteerViews();
        }
    }

    private void startPolling() {
        // 只有志愿者页面才需要轮询
        if (isBlind) {
            return;
        }

        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadStatistics();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void setupBlindViews() {
        Log.d(TAG, "设置盲人视图");
        
        View callVolunteerCard = rootView.findViewById(R.id.callVolunteerCard);
        if (callVolunteerCard != null) {
        callVolunteerCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在呼叫志愿者...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), CallActivity.class));
        });
        }
    }

    private void setupVolunteerViews() {
        // 初始化统计视图
        statsContainer = rootView.findViewById(R.id.statsContainer);
        volunteerCount = rootView.findViewById(R.id.volunteerCount);
        onlineVolunteerCount = rootView.findViewById(R.id.onlineVolunteerCount);
        blindUserCount = rootView.findViewById(R.id.blindUserCount);
        weeklyHelpCount = rootView.findViewById(R.id.weeklyHelpCount);
        weeklyHelpIncrease = rootView.findViewById(R.id.weeklyHelpIncrease);

        // 初始化个人帮助统计视图
        totalHelpCount = rootView.findViewById(R.id.totalHelpCount);
        monthlyHelpCount = rootView.findViewById(R.id.monthlyHelpCount);
        totalHours = rootView.findViewById(R.id.totalHours);

        // 初始化在线状态开关
initializeOnlineSwitch();

        // 设置查看全部按钮的点击事件
        TextView viewAllText = rootView.findViewById(R.id.viewAllHistory);
        if (viewAllText != null) {
            Log.d(TAG, "找到查看全部按钮");
            viewAllText.setClickable(true);
            viewAllText.setOnClickListener(v -> {
                Log.d(TAG, "查看全部按钮被点击");
                startActivity(new Intent(getActivity(), HelpRecordsActivity.class));
            });
        } else {
            Log.e(TAG, "未找到查看全部按钮");
        }

        // 初始化最近帮助记录视图
        recentUserNameText = rootView.findViewById(R.id.recentUserNameText);
        recentHelpInfoText = rootView.findViewById(R.id.recentHelpInfoText);
        recentHelpTimeText = rootView.findViewById(R.id.recentHelpTimeText);
        recentHelpLayout = rootView.findViewById(R.id.recentHelpLayout);

        // 加载统计数据和最近帮助记录
        loadStatistics();
    }

//    private void setupOnlineSwitch() {
//        if (onlineSwitch != null) {
//            // 从SharedPreferences中恢复在线状态
//            boolean isOnline = preferences.getBoolean(PREF_VOLUNTEER_ONLINE, false);
//            onlineSwitch.setChecked(isOnline);
//
//            // 更新应用状态
//            BeYourEyesApplication.getInstance().setVolunteerOnline(isOnline);
//
//        onlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                // 保存在线状态
//                preferences.edit().putBoolean(PREF_VOLUNTEER_ONLINE, isChecked).apply();
//
//                // 更新应用状态
//                BeYourEyesApplication.getInstance().setVolunteerOnline(isChecked);
//
//                if (isChecked) {
//                    Toast.makeText(getContext(), "已上线", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(getContext(), "已下线", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    }

private void initializeOnlineSwitch() {
    SwitchMaterial onlineSwitch = rootView.findViewById(R.id.onlineSwitch);

    if (onlineSwitch != null) {
        // 从SharedPreferences中恢复在线状态
        boolean isOnline = preferences.getBoolean(PREF_VOLUNTEER_ONLINE, false);
        onlineSwitch.setChecked(isOnline);

        // 确保应用状态与SharedPreferences一致
        BeYourEyesApplication.getInstance().setVolunteerOnline(isOnline);

        // 添加单一的状态变化监听器
        onlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "在线状态开关状态改变: " + isChecked);

            // 保存在线状态到SharedPreferences
            preferences.edit().putBoolean(PREF_VOLUNTEER_ONLINE, isChecked).apply();

            // 更新应用程序状态
            BeYourEyesApplication.getInstance().setVolunteerOnline(isChecked);

            // 调用updateOnlineStatus方法
            updateOnlineStatus(isChecked);

            // 显示状态变化提示
            Toast.makeText(getContext(), isChecked ? "已上线" : "已下线", Toast.LENGTH_SHORT).show();
        });
    }
}

    private void loadStatistics() {
        // 盲人用户不需要加载任何统计数据
        if (isBlind) {
            return;
        }

        if (statsContainer != null) {
            statsContainer.setAlpha(0.5f);
        }

        if (currentCall != null) {
            currentCall.cancel();
        }

        // 获取平台统计数据
        currentCall = ApiClient.getApiService().getStatistics();
        currentCall.enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatisticsResponse> call,
                                   @NonNull Response<StatisticsResponse> response) {
                if (statsContainer != null) {
                    statsContainer.setAlpha(1.0f);
                }

                if (response.isSuccessful() && response.body() != null) {
                    updateStatistics(response.body());
                } else {
                    showError("数据加载失败: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatisticsResponse> call,
                                  @NonNull Throwable t) {
                if (statsContainer != null) {
                    statsContainer.setAlpha(1.0f);
                }
                if (!call.isCanceled()) {
                    showError("网络连接失败");
                }
            }
        });

        // 获取个人帮助统计数据
        String email = BeYourEyesApplication.getInstance().getUserEmail();
        String token = BeYourEyesApplication.getInstance().getAuthToken();
        if (email != null && token != null) {
            ApiClient.getApiService().getHelpStats(email, token).enqueue(new Callback<HelpStatsResponse>() {
                @Override
                public void onResponse(@NonNull Call<HelpStatsResponse> call,
                                     @NonNull Response<HelpStatsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        updateHelpStats(response.body());
                    } else if (response.code() == 401) {
                        refreshTokenAndRetry(false);
                    } else {
                        showError("个人统计数据加载失败: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<HelpStatsResponse> call,
                                    @NonNull Throwable t) {
                    if (!call.isCanceled()) {
                        showError("网络连接失败");
                    }
                }
            });
        } else {
            showError("用户信息不完整，请重新登录");
        }

        // 加载最近帮助记录
        loadHelpRecords();
    }

    private void updateStatistics(StatisticsResponse stats) {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            volunteerCount.setText(nf.format(stats.getVolunteers()));
            onlineVolunteerCount.setText(nf.format(stats.getOnlineVolunteers()));
            blindUserCount.setText(nf.format(stats.getBlindUsers()));
        } catch (Exception e) {
            Log.e(TAG, "数据格式化错误", e);
        }
    }

    private void updateHelpStats(HelpStatsResponse stats) {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            totalHelpCount.setText(nf.format(stats.getTotalHelpCount()));
            monthlyHelpCount.setText(nf.format(stats.getMonthlyHelpCount()));

// 获取本周帮助次数和上周帮助次数
            int currentWeekCount = stats.getWeeklyHelpCount();
            int lastWeekCount = stats.getLastWeeklyHelpCount();

// 设置本周帮助次数，添加"本周已完成"前缀
            @SuppressLint("DefaultLocale") String weeklyText = String.format(Locale.getDefault(), "本周已完成 %s 次帮助",
                    String.format("%,d", currentWeekCount));
            weeklyHelpCount.setText(weeklyText);

// 计算周帮助增长百分比
            double weeklyIncrease;
            if (lastWeekCount > 0) {
                weeklyIncrease = ((double) currentWeekCount - lastWeekCount) / lastWeekCount * 100;
                // 如果增长为负，设置为 0
                weeklyIncrease = Math.max(0, weeklyIncrease);
            } else {
                // 如果上周次数为 0，设置增长为 0
                weeklyIncrease = 0;
            }

// 设置周帮助增长百分比，添加"+"前缀
            String increaseText = String.format(Locale.US, "+%.1f%%", weeklyIncrease);
            weeklyHelpIncrease.setText(increaseText);
            // 将总时长从分钟转换为小时，保留一位小数
            double totalHours = stats.getTotalHelpDuration() / 60.0;
            this.totalHours.setText(String.format(Locale.US, "%.1f", totalHours));
        } catch (Exception e) {
            Log.e(TAG, "个人统计数据格式化错误", e);
        }
    }

    private void updateOnlineStatus(boolean isOnline) {
        Log.d(TAG, "开始更新在线状态: " + isOnline);

        // 取消之前的请求
        if (statusCall != null) {
            statusCall.cancel();
            statusCall = null;
        }

        String email = BeYourEyesApplication.getInstance().getUserEmail();
        String token = BeYourEyesApplication.getInstance().getAuthToken();

        if (email == null || token == null) {
            Log.e(TAG, "用户信息不完整，无法更新状态");
            showError("用户信息不完整，请重新登录");
            restoreSwitchState(!isOnline);
            return;
        }

        ResetStatusRequest request = new ResetStatusRequest(email, isOnline, token);
        statusCall = ApiClient.getApiService().resetUserStatus(request);

        statusCall.enqueue(new Callback<ResetStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResetStatusResponse> call,
                                   @NonNull Response<ResetStatusResponse> response) {
                if (call.isCanceled()) {
                    Log.d(TAG, "状态更新请求已取消");
                    return;
                }

                if (response.code() == 401) {
                    Log.d(TAG, "Token过期，尝试刷新");
                    restoreSwitchState(!isOnline);
                    refreshTokenAndRetry(isOnline);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    ResetStatusResponse statusResponse = response.body();
                    if ("success".equals(statusResponse.getStatus())) {
                        Log.d(TAG, "用户状态更新成功: " + isOnline);
                    } else {
                        Log.e(TAG, "状态更新失败: " + statusResponse.getMessage());
                        showError(statusResponse.getMessage());
                        restoreSwitchState(!isOnline);
                    }
                } else {
                    Log.e(TAG, "状态更新失败: " + response.code());
                    showError("状态更新失败: " + response.code());
                    restoreSwitchState(!isOnline);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResetStatusResponse> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    Log.e(TAG, "网络请求失败", t);
                    showError("网络连接失败，请检查网络设置");
                    restoreSwitchState(!isOnline);
                }
            }
        });
    }

    private void refreshTokenAndRetry(boolean isOnline) {
        if (isRefreshingToken) {
            Log.d(TAG, "Token 正在刷新中，忽略重复请求");
            return;
        }

        if (retryCount >= MAX_RETRY) {
            Log.e(TAG, "刷新 Token 重试次数过多，终止重试");
            showError("登录状态异常，请重新登录");
            restoreSwitchState(!isOnline);
            return;
        }

        String refreshToken = BeYourEyesApplication.getInstance().getRefreshToken();
        if (refreshToken == null) {
            Log.e(TAG, "Refresh Token 不存在，需要重新登录");
            showError("登录已过期，请重新登录");
            restoreSwitchState(!isOnline);
            return;
        }

        isRefreshingToken = true;
        retryCount++;

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        Call<RefreshTokenResponse> refreshCall = ApiClient.getApiService().refreshToken(request);

        refreshCall.enqueue(new Callback<RefreshTokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<RefreshTokenResponse> call,
                                   @NonNull Response<RefreshTokenResponse> response) {
                isRefreshingToken = false;

                if (response.isSuccessful() && response.body() != null) {
                    RefreshTokenResponse refreshResponse = response.body();
                    if ("success".equals(refreshResponse.getStatus())) {
                        BeYourEyesApplication.getInstance().setAuthToken(refreshResponse.getAccessToken());
                        Log.d(TAG, "Token刷新成功");
                        updateOnlineStatus(isOnline); // retry
                    } else {
                        Log.e(TAG, "Token刷新失败: " + refreshResponse.getMessage());
                        showError("登录已过期，请重新登录");
                        restoreSwitchState(!isOnline);
                    }
                } else {
                    Log.e(TAG, "Token刷新失败: " + response.code());
                    showError("登录已过期，请重新登录");
                    restoreSwitchState(!isOnline);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RefreshTokenResponse> call,
                                  @NonNull Throwable t) {
                isRefreshingToken = false;

                if (!call.isCanceled()) {
                    Log.e(TAG, "Token刷新请求失败", t);
                    showError("网络连接失败，请检查网络设置");
                    restoreSwitchState(!isOnline);
                }
            }
        });
    }

    private void restoreSwitchState(boolean state) {
        if (getView() != null) {
            SwitchMaterial onlineSwitch = getView().findViewById(R.id.onlineSwitch);
            if (onlineSwitch != null) {
                // 移除监听器，避免触发 updateOnlineStatus()
                onlineSwitch.setOnCheckedChangeListener(null);
                onlineSwitch.setChecked(state);
                // 恢复监听器
                onlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    updateOnlineStatus(isChecked);
                });
            }
        }
    }

    private void showError(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHelpRecords() {
        String email = BeYourEyesApplication.getInstance().getUserEmail();
        String token = BeYourEyesApplication.getInstance().getAuthToken();

        if (email != null && token != null) {
            ApiClient.getApiService().getHelpRecords(email, token).enqueue(new Callback<List<HelpRecord>>() {
                @Override
                public void onResponse(@NonNull Call<List<HelpRecord>> call,
                                     @NonNull Response<List<HelpRecord>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        // 显示最近的一条记录
                        HelpRecord latestRecord = response.body().get(0);
                        updateRecentHelpRecord(latestRecord);
                    } else if (response.code() == 401) {
                        refreshTokenAndRetry(false);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<HelpRecord>> call,
                                    @NonNull Throwable t) {
                    Log.e(TAG, "加载帮助记录失败", t);
                }
            });
        }
    }

    private void updateRecentHelpRecord(HelpRecord record) {
        if (recentHelpLayout != null) {
            recentHelpLayout.setVisibility(View.VISIBLE);
            recentUserNameText.setText(String.format("帮助用户：%s", record.getUserName()));
            recentHelpInfoText.setText(String.format("%s · %d分钟", 
                record.getFormattedStatus(), record.getDuration()));
            recentHelpTimeText.setText(record.getFormattedTime());
        }
    }
}