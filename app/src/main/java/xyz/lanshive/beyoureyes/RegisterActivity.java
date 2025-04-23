package xyz.lanshive.beyoureyes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import xyz.lanshive.beyoureyes.model.RegisterRequest;
import xyz.lanshive.beyoureyes.model.VerificationCodeRequest;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private TextInputEditText emailInput;
    private TextInputEditText verificationCodeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputLayout emailLayout;
    private TextInputLayout verificationCodeLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private Button sendCodeButton;
    private Button registerButton;
    private TextView loginLink;
    private int countdown = 60;
    private boolean isCountingDown = false;
    private String userRole;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 从BeYourEyesApplication获取用户角色
        userRole = BeYourEyesApplication.getInstance().getCurrentUserRole();
        Log.d(TAG, "注册用户角色: " + userRole);

        // 初始化视图
        initViews();
        // 设置点击事件
        setClickListeners();
        // 设置输入监听
        setInputListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.phoneInput);
        verificationCodeInput = findViewById(R.id.verificationCodeInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        emailLayout = findViewById(R.id.emailLayout);
        verificationCodeLayout = findViewById(R.id.verificationCodeLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
    }

    private void setClickListeners() {
        // 发送验证码按钮点击事件
        sendCodeButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailInput.getText()).toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("请输入邮箱");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("请输入有效的邮箱地址");
                return;
            }

            // 发送验证码请求
            sendVerificationCode(email);
        });

        // 注册按钮点击事件
        registerButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
            String code = Objects.requireNonNull(verificationCodeInput.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
            String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

            if (!isValidEmail(email)) {
                emailLayout.setError("请输入有效的邮箱地址");
                return;
            }

            if (TextUtils.isEmpty(code)) {
                verificationCodeLayout.setError("请输入验证码");
                return;
            }

            if (!isValidVerificationCode(code)) {
                verificationCodeLayout.setError("验证码必须为6位数字");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError("请输入密码");
                return;
            }

            if (password.length() < 8) {
                passwordLayout.setError("密码长度不能少于8个字符");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordLayout.setError("两次输入的密码不一致");
                return;
            }

            // 发送注册请求
            register(email, code, password);
        });

        // 返回登录页面
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void setInputListeners() {
        // 邮箱输入监听
        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
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

        // 验证码输入监听
        verificationCodeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String code = s.toString().trim();
                if (TextUtils.isEmpty(code)) {
                    verificationCodeLayout.setError("请输入验证码");
                    verificationCodeLayout.setErrorEnabled(true);
                } else if (!isValidVerificationCode(code)) {
                    verificationCodeLayout.setError("验证码必须为6位数字");
                    verificationCodeLayout.setErrorEnabled(true);
                } else {
                    verificationCodeLayout.setErrorEnabled(false);
                }
            }
        });

        // 密码输入监听
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString().trim();
                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError("请输入密码");
                    passwordLayout.setErrorEnabled(true);
                } else if (password.length() < 8) {
                    passwordLayout.setError("密码长度不能少于8个字符");
                    passwordLayout.setErrorEnabled(true);
                } else {
                    passwordLayout.setErrorEnabled(false);
                }
            }
        });

        // 确认密码输入监听
        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String confirmPassword = s.toString().trim();
                String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

                if (TextUtils.isEmpty(confirmPassword)) {
                    confirmPasswordLayout.setError("请确认密码");
                    confirmPasswordLayout.setErrorEnabled(true);
                } else if (!confirmPassword.equals(password)) {
                    confirmPasswordLayout.setError("两次输入的密码不一致");
                    confirmPasswordLayout.setErrorEnabled(true);
                } else {
                    confirmPasswordLayout.setErrorEnabled(false);
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidVerificationCode(String code) {
        // 验证码为6位数字
        return code.matches("\\d{6}");
    }

    @SuppressLint("SetTextI18n")
    private void startCountdown() {
        if (isCountingDown) return;
        isCountingDown = true;
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText(countdown + "秒后重试");

        new Thread(() -> {
            while (countdown > 0) {
                try {
                    Thread.sleep(1000);
                    countdown--;
                    runOnUiThread(() -> sendCodeButton.setText(countdown + "秒后重试"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> {
                isCountingDown = false;
                countdown = 60;
                sendCodeButton.setEnabled(true);
                sendCodeButton.setText("获取验证码");
            });
        }).start();
    }

    private void register(String email, String code, String password) {
        Log.d(TAG, "开始注册流程");
        Log.d(TAG, "邮箱: " + email);
        Log.d(TAG, "用户角色: " + userRole);

        // 显示加载状态
        registerButton.setEnabled(false);
        registerButton.setText("注册中...");

        // 创建请求对象
        RegisterRequest request = new RegisterRequest(email, password, userRole, code);
        Log.d(TAG, "创建注册请求对象");

        // 发送请求
        Call<AuthResponse> call = ApiClient.getApiService().register(request);
        Log.d(TAG, "发送注册请求");
        
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                Log.d(TAG, "收到注册响应");
                Log.d(TAG, "响应状态码: " + response.code());
                
                // 恢复按钮状态
                registerButton.setEnabled(true);
                registerButton.setText("注册");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "注册响应成功: " + authResponse.isSuccess());
                    Log.d(TAG, "响应消息: " + (authResponse.getMessage() != null ? authResponse.getMessage() : "无消息"));
                    
                    if (authResponse.isSuccess()) {
                        try {
                            Log.d(TAG, "注册成功，开始保存用户信息");
                            // 保存用户信息
                            SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putString("user_role", userRole)
                                .putString("user_email", email)
                                .putBoolean("is_logged_in", true)
                                .apply();
                            Log.d(TAG, "用户信息保存成功，用户角色: " + userRole + ", 邮箱: " + email);

                            // 显示注册成功提示
                            String roleText = userRole.equals(BeYourEyesApplication.ROLE_BLIND) ? "视障用户" : "志愿者";
                            Toast.makeText(RegisterActivity.this, "注册成功，欢迎" + roleText, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Toast显示成功，准备跳转到主页");
                            
                            // 延迟一段时间后跳转到主页，确保Toast消息能够显示
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    Log.d(TAG, "开始创建Intent跳转到MainActivity");
                                    // 跳转到主页
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    // 清除任务栈中的其他活动
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    Log.d(TAG, "Intent创建成功，准备startActivity");
                                    startActivity(intent);
                                    Log.d(TAG, "startActivity调用成功，准备finish当前Activity");
                                    finish();
                                    Log.d(TAG, "注册流程全部完成");
                                } catch (Exception e) {
                                    Log.e(TAG, "跳转到MainActivity时发生异常", e);
                                    Toast.makeText(RegisterActivity.this, "跳转失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }, 1500); // 延迟1.5秒
                        } catch (Exception e) {
                            Log.e(TAG, "注册成功后的操作失败", e);
                            Toast.makeText(RegisterActivity.this, "注册成功，但跳转失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMessage = authResponse.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "注册失败，请稍后重试";
                        }
                        Log.d(TAG, "注册失败: " + errorMessage);
                        handleRegistrationError(errorMessage);
                    }
                } else {
                    String errorMessage = "注册失败";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                            Log.e(TAG, "注册请求失败: " + errorMessage);
                            handleRegistrationError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应失败", e);
                        Toast.makeText(RegisterActivity.this, "注册失败，请稍后重试", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "注册请求失败: " + t.getMessage(), t);
                // 记录更详细的错误信息
                Log.e(TAG, "错误类型: " + t.getClass().getName());
                Log.e(TAG, "错误堆栈: ", t);
                
                // 恢复按钮状态
                registerButton.setEnabled(true);
                registerButton.setText("注册");
                
                // 显示更详细的错误信息
                String errorMsg = "服务器维护中，请稍后再试";
                Log.e(TAG, "显示错误Toast: " + errorMsg);
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRegistrationError(String errorMessage) {
        if (errorMessage == null) {
            Toast.makeText(this, "注册失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        if (errorMessage.contains("邮箱已注册")) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("邮箱已注册")
                .setMessage("该邮箱已被注册，是否直接登录？")
                .setPositiveButton("登录", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("email", Objects.requireNonNull(emailInput.getText()).toString().trim());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
        } else if (errorMessage.contains("验证码错误")) {
            verificationCodeLayout.setError("验证码错误，请检查或重新获取");
            verificationCodeLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("验证码已过期")) {
            verificationCodeLayout.setError("验证码已过期，请检查或重新获取");
            verificationCodeLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("密码格式不正确")) {
            passwordLayout.setError("密码格式不正确，请重新输入");
            passwordLayout.setErrorEnabled(true);
        } else {
            Toast.makeText(this, "注册失败：" + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVerificationCode(String email) {
        // 显示加载状态
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText("发送中...");

        // 创建请求对象
        VerificationCodeRequest request = new VerificationCodeRequest(email);

        // 发送请求
        Call<AuthResponse> call = ApiClient.getApiService().sendVerificationCode(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                // 恢复按钮状态
                sendCodeButton.setEnabled(true);
                sendCodeButton.setText("发送验证码");

                if (response.isSuccessful() && response.body() != null) {
                    userEmail = email;
                    Toast.makeText(RegisterActivity.this, "验证码已发送到您的邮箱", Toast.LENGTH_SHORT).show();
                    startCountdown(); // 开始倒计时
                } else {
                    String errorMessage = "发送验证码失败";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                            handleVerificationCodeError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应失败", e);
                        Toast.makeText(RegisterActivity.this, "发送验证码失败，请稍后重试", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                // 恢复按钮状态
                sendCodeButton.setEnabled(true);
                sendCodeButton.setText("发送验证码");

                Log.e(TAG, "发送验证码请求失败", t);
                Toast.makeText(RegisterActivity.this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleVerificationCodeError(String errorMessage) {
        if (errorMessage.contains("邮箱已注册")) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("邮箱已注册")
                .setMessage("该邮箱已被注册，是否直接登录？")
                .setPositiveButton("登录", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("email", Objects.requireNonNull(emailInput.getText()).toString().trim());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
        } else if (errorMessage.contains("发送过于频繁")) {
            Toast.makeText(this, "发送验证码过于频繁，请稍后再试", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("550")) {
            Toast.makeText(this, "发送失败，请检查邮箱是否正确或稍后再试", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "发送验证码失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }
}