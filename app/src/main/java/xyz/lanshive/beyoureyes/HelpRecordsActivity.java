package xyz.lanshive.beyoureyes;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.lanshive.beyoureyes.adapter.HelpRecordsAdapter;
import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.model.HelpRecord;

public class HelpRecordsActivity extends AppCompatActivity {
    private static final String TAG = "HelpRecordsActivity";
    private RecyclerView recyclerView;
    private HelpRecordsAdapter adapter;
    private List<HelpRecord> helpRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_records);

        // 设置返回按钮点击事件
        findViewById(R.id.headerBackground).setOnClickListener(v -> finish());

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.helpRecordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HelpRecordsAdapter(helpRecords);
        recyclerView.setAdapter(adapter);

        // 加载帮助记录数据
        loadHelpRecords();
    }

    private void loadHelpRecords() {
        String email = BeYourEyesApplication.getInstance().getUserEmail();
        String token = BeYourEyesApplication.getInstance().getAuthToken();

        if (email != null && token != null) {
            ApiClient.getApiService().getHelpRecords(email, token).enqueue(new Callback<List<HelpRecord>>() {
                @Override
                public void onResponse(@NonNull Call<List<HelpRecord>> call,
                                       @NonNull Response<List<HelpRecord>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        helpRecords.clear();
                        helpRecords.addAll(response.body());
                        adapter.notifyDataSetChanged();
                    } else if (response.code() == 401) {
                        // 处理token过期
                        handleTokenExpired();
                    } else {
                        Log.e(TAG, "加载帮助记录失败: " + response.code());
                        Toast.makeText(HelpRecordsActivity.this, "加载帮助记录失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<HelpRecord>> call,
                                      @NonNull Throwable t) {
                    Log.e(TAG, "网络请求失败", t);
                    Toast.makeText(HelpRecordsActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleTokenExpired() {
        // 实现处理token过期的逻辑，例如跳转到登录页面
        Toast.makeText(this, "Token已过期，请重新登录", Toast.LENGTH_SHORT).show();
        // 这里可以添加跳转到登录页面的代码
    }
}