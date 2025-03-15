package com.example.beyoureyes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beyoureyes.api.ApiClient;
import com.example.beyoureyes.model.StatisticsResponse;
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
    private Button needHelpButton;
    private Button volunteerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 初始化视图
        blindUserCount = findViewById(R.id.blindUserCount);
        volunteerCount = findViewById(R.id.volunteerCount);
        statsContainer = findViewById(R.id.statsContainer);
        needHelpButton = findViewById(R.id.needHelpButton);
        volunteerButton = findViewById(R.id.volunteerButton);

        // 设置按钮点击事件
        needHelpButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.putExtra("userType", "HELPER");
            startActivity(intent);
        });

        volunteerButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.putExtra("userType", "VOLUNTEER");
            startActivity(intent);
        });

        // 加载统计数据
        loadStatistics();
    }

    private void loadStatistics() {
        // 显示加载状态
        statsContainer.setAlpha(0.5f);

        ApiClient.getApiService().getStatistics().enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(Call<StatisticsResponse> call, Response<StatisticsResponse> response) {
                // 恢复正常显示状态
                statsContainer.setAlpha(1.0f);

                if (response.isSuccessful() && response.body() != null) {
                    StatisticsResponse stats = response.body();
                    updateStatistics(stats);
                } else {
                    Log.e(TAG, "Response not successful: " + response.code());
                    showError("获取数据失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<StatisticsResponse> call, Throwable t) {
                // 恢复正常显示状态
                statsContainer.setAlpha(1.0f);
                Log.e(TAG, "Network call failed", t);
                showError("网络连接失败，请检查网络设置");
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
} 