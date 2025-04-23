package xyz.lanshive.beyoureyes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.model.AuthResponse;
import xyz.lanshive.beyoureyes.model.LoginRequest;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private Button loginButton;
    private TextView registerLink;
    private TextView forgotPasswordLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "LoginActivity onCreate - 检查登录状态");
        // 检查是否已登录
        if (BeYourEyesApplication.getInstance().isLoggedIn()) {
            Log.d(TAG, "LoginActivity onCreate - 用户已登录，直接进入主页");
            loginSuccess();
            return;
        }
        Log.d(TAG, "LoginActivity onCreate - 用户未登录，显示登录界面");

        // 初始化视图
        initViews();
        // 设置点击事件
        setClickListeners();
        // 设置输入监听
        setInputListeners();

        // 检查是否有传入的邮箱
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            emailInput.setText(email);
        }
    }

    private void initViews() {
        emailInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
    }

    private void setClickListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("请输入邮箱");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("请输入有效的邮箱地址");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError("请输入密码");
                return;
            }

            // 发送登录请求
            login(email, password);
        });

        // 注册链接点击事件
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        // 忘记密码链接点击事件
        forgotPasswordLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void setInputListeners() {
        // 邮箱输入监听
        emailInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String email = s.toString().trim();
                if (TextUtils.isEmpty(email)) {
                    emailLayout.setError("请输入邮箱");
                    emailLayout.setErrorEnabled(true);
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailLayout.setError("请输入有效的邮箱地址");
                    emailLayout.setErrorEnabled(true);
                } else {
                    emailLayout.setErrorEnabled(false);
                }
            }
        });

        // 密码输入监听
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String password = s.toString().trim();
                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError("请输入密码");
                    passwordLayout.setErrorEnabled(true);
                } else {
                    passwordLayout.setErrorEnabled(false);
                }
            }
        });
    }

    private void login(String email, String password) {
        Log.d(TAG, "开始登录流程");
        Log.d(TAG, "邮箱: " + email);

        // 显示加载状态
        loginButton.setEnabled(false);
        loginButton.setText("登录中...");

        // 创建请求对象
        LoginRequest request = new LoginRequest(email, password);
        Log.d(TAG, "创建登录请求对象");

        // 发送请求
        Call<AuthResponse> call = ApiClient.getApiService().login(request);
        Log.d(TAG, "发送登录请求");
        
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                Log.d(TAG, "收到登录响应");
                Log.d(TAG, "响应状态码: " + response.code());
                
                // 恢复按钮状态
                loginButton.setEnabled(true);
                loginButton.setText("登录");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.isSuccess()) {
                        // 保存用户信息
                        BeYourEyesApplication.getInstance().setUserEmail(authResponse.getUser().getEmail());
                        BeYourEyesApplication.getInstance().setAuthToken(authResponse.getToken());
                        BeYourEyesApplication.getInstance().setRefreshToken(authResponse.getRefreshToken());
                        BeYourEyesApplication.getInstance().setLoggedIn(true);

                        // 设置用户角色
                        if (authResponse.getRole().equals(BeYourEyesApplication.ROLE_BLIND)) {
                            BeYourEyesApplication.getInstance().setCurrentUserRole(BeYourEyesApplication.ROLE_BLIND);
                        } else {
                            BeYourEyesApplication.getInstance().setCurrentUserRole(BeYourEyesApplication.ROLE_VOLUNTEER);
                        }

                        // 显示登录成功提示
                        String roleText = authResponse.getRole().equals(BeYourEyesApplication.ROLE_BLIND) ? "视障用户" : "志愿者";
                        Toast.makeText(LoginActivity.this, "登录成功，欢迎" + roleText, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Toast显示成功，准备跳转到主页");

                        // 延迟一段时间后跳转到主页，确保Toast消息能够显示
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Log.d(TAG, "开始跳转到主页");
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    } else {
                        showError(authResponse.getMessage());
                    }
                } else {
                    showError("登录失败，请检查网络连接");
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "登录请求失败: " + t.getMessage(), t);
                // 记录更详细的错误信息
                Log.e(TAG, "错误类型: " + t.getClass().getName());
                Log.e(TAG, "错误堆栈: ", t);
                
                // 恢复按钮状态
                loginButton.setEnabled(true);
                loginButton.setText("登录");
                
                // 显示更详细的错误信息
                String errorMsg = "服务器维护中，请稍后再试";
                Log.e(TAG, "显示错误Toast: " + errorMsg);
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showError(String errorMessage) {
        if (errorMessage == null) {
            Toast.makeText(this, "登录失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据服务器返回的错误信息显示不同的提示
        if (errorMessage.contains("用户不存在")) {
            emailLayout.setError("该邮箱未注册");
            emailLayout.setErrorEnabled(true);
            passwordLayout.setErrorEnabled(false);
        } else if (errorMessage.contains("密码错误")) {
            passwordLayout.setError("密码错误");
            passwordLayout.setErrorEnabled(true);
            emailLayout.setErrorEnabled(false);
        } else if (errorMessage.contains("账号已被禁用")) {
            Toast.makeText(this, "账号已被禁用，请联系管理员", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("邮箱格式错误")) {
            emailLayout.setError("邮箱格式错误");
            emailLayout.setErrorEnabled(true);
            passwordLayout.setErrorEnabled(false);
        } else if (errorMessage.contains("密码不能为空")) {
            passwordLayout.setError("请输入密码");
            passwordLayout.setErrorEnabled(true);
            emailLayout.setErrorEnabled(false);
        } else if (errorMessage.contains("邮箱不能为空")) {
            emailLayout.setError("请输入邮箱");
            emailLayout.setErrorEnabled(true);
            passwordLayout.setErrorEnabled(false);
        } else if (errorMessage.contains("网络连接失败")) {
            Toast.makeText(this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("服务器错误")) {
            Toast.makeText(this, "服务器暂时无法响应，请稍后重试", Toast.LENGTH_SHORT).show();
        } else {
            // 其他未知错误
            Toast.makeText(this, "登录失败：" + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void loginSuccess() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
} 