package xyz.lanshive.beyoureyes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.model.StatisticsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.NumberFormat;
import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";
    private TextView blindUserCount;
    private TextView volunteerCount;
    private View statsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Log.d(TAG, "WelcomeActivity onCreate - 检查登录状态");
        // 检查是否已登录
        if (BeYourEyesApplication.getInstance().isLoggedIn()) {
            Log.d(TAG, "WelcomeActivity onCreate - 用户已登录，直接进入主页");
            startMainActivity();
            return;
        }
        Log.d(TAG, "WelcomeActivity onCreate - 用户未登录，显示欢迎页面");

        // 初始化视图
        blindUserCount = findViewById(R.id.blindUserCount);
        volunteerCount = findViewById(R.id.volunteerCount);
        statsContainer = findViewById(R.id.statsContainer);
        Button needHelpButton = findViewById(R.id.needHelpButton);
        Button volunteerButton = findViewById(R.id.volunteerButton);

        // 设置按钮点击事件
        needHelpButton.setOnClickListener(v -> {
            Log.d(TAG, "用户选择视障用户身份");
            // 设置为视障用户身份
            BeYourEyesApplication.getInstance().setCurrentUserRole(BeYourEyesApplication.ROLE_BLIND);
            startPrivacyActivity();
        });

        volunteerButton.setOnClickListener(v -> {
            Log.d(TAG, "用户选择志愿者身份");
            // 设置为志愿者身份
            BeYourEyesApplication.getInstance().setCurrentUserRole(BeYourEyesApplication.ROLE_VOLUNTEER);
            startPrivacyActivity();
        });

        // 加载统计数据
        loadStatistics();
    }

    private void loadStatistics() {
        // 显示加载状态
        statsContainer.setAlpha(0.5f);

        Log.d(TAG, "开始加载统计数据...");
        Call<StatisticsResponse> call = ApiClient.getApiService().getStatistics();
        Log.d(TAG, "请求URL: " + call.request().url());

        call.enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatisticsResponse> call, @NonNull Response<StatisticsResponse> response) {
                // 恢复正常显示状态
                statsContainer.setAlpha(1.0f);

                if (response.isSuccessful() && response.body() != null) {
                    StatisticsResponse stats = response.body();
                    Log.d(TAG, "成功获取统计数据: 视障用户=" + stats.getBlindUsers() + ", 志愿者=" + stats.getVolunteers());
                    updateStatistics(stats);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应失败", e);
                    }

                    Log.e(TAG, "请求失败: 状态码=" + response.code() + ", 错误=" + errorBody);
                    showError("获取数据失败: " + response.code() + (errorBody.isEmpty() ? "" : " - " + errorBody));
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatisticsResponse> call, @NonNull Throwable t) {
                // 恢复正常显示状态
                statsContainer.setAlpha(1.0f);
                Log.e(TAG, "网络请求失败", t);
                showError("服务器维护中，请稍后再试 " );
            }
        });
    }

    private void updateStatistics(StatisticsResponse stats) {
        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            blindUserCount.setText(numberFormat.format(stats.getBlindUsers()));
            volunteerCount.setText(numberFormat.format(stats.getVolunteers()));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting numbers", e);
            showError("数据格式错误");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startPrivacyActivity() {
        Log.d(TAG, "跳转到隐私政策页面");
        Intent intent = new Intent(this, PrivacyActivity.class);
        startActivity(intent);
        finish();
    }

    private void startMainActivity() {
        Log.d(TAG, "跳转到主页");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}