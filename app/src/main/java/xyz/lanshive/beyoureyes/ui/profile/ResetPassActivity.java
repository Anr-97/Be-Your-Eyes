package xyz.lanshive.beyoureyes.ui.profile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;
import xyz.lanshive.beyoureyes.LoginActivity;
import xyz.lanshive.beyoureyes.R;
import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.model.AuthResponse;
import xyz.lanshive.beyoureyes.model.ResetPassRequest;
import xyz.lanshive.beyoureyes.model.ResetPasswordVerifyRequest;

import java.util.Objects;

public class ResetPassActivity extends AppCompatActivity {
    private static final String TAG = "ResetPassActivity";
    private static final int COUNTDOWN_TIME = 60000; // 60秒
    private TextInputLayout emailLayout;
    private TextInputLayout codeLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText codeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button sendCodeButton;
    private Button resetButton;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resetpass);

        initViews();
        setClickListeners();
        setInputListeners();
    }

    private void initViews() {
        codeLayout = findViewById(R.id.codeLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        codeInput = findViewById(R.id.codeInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        resetButton = findViewById(R.id.resetButton);
    }

    private void setClickListeners() {
        // 发送验证码按钮点击事件
        sendCodeButton.setOnClickListener(v -> {
            String email = BeYourEyesApplication.getInstance().getUserEmail();
            sendResetPasswordCode(email);
        });

        // 修改密码按钮点击事件
        resetButton.setOnClickListener(v -> {
            String email = BeYourEyesApplication.getInstance().getUserEmail();
            String code = Objects.requireNonNull(codeInput.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
            String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

            if (validateResetPasswordInput(code, password, confirmPassword)) {
                resetPassword(email, code, password);
            }
        });
    }

    private void setInputListeners() {
        // 验证码输入监听
        codeInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String code = s.toString().trim();
                if (TextUtils.isEmpty(code)) {
                    codeLayout.setError("请输入验证码");
                    codeLayout.setErrorEnabled(true);
                } else {
                    codeLayout.setErrorEnabled(false);
                }
            }
        });

        // 密码输入监听
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String password = s.toString().trim();
                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError("请输入新密码");
                    passwordLayout.setErrorEnabled(true);
                } else if (password.length() < 6) {
                    passwordLayout.setError("密码长度不能少于6个字符");
                    passwordLayout.setErrorEnabled(true);
                } else {
                    passwordLayout.setErrorEnabled(false);
                }
            }
        });

        // 确认密码输入监听
        confirmPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String confirmPassword = s.toString().trim();
                String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
                if (TextUtils.isEmpty(confirmPassword)) {
                    confirmPasswordLayout.setError("请确认新密码");
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

    private boolean validateResetPasswordInput(String code, String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(code)) {
            codeLayout.setError("请输入验证码");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("请输入新密码");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("密码长度不能少于6个字符");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("请确认新密码");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("两次输入的密码不一致");
            isValid = false;
        }

        return isValid;
    }

    private void startCountDown() {
        sendCodeButton.setEnabled(false);
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                sendCodeButton.setText(String.format("%ds后重新发送", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                sendCodeButton.setEnabled(true);
                sendCodeButton.setText("发送验证码");
            }
        }.start();
    }

    private void sendResetPasswordCode(String email) {
        Log.d(TAG, "开始发送修改密码验证码");
        Log.d(TAG, "邮箱: " + email);

        // 禁用发送按钮
        sendCodeButton.setEnabled(false);

        // 创建请求对象
        ResetPassRequest request = new ResetPassRequest(email);
        Log.d(TAG, "创建发送验证码请求对象");

        // 发送请求
        Call<AuthResponse> call = ApiClient.getApiService().sendResetPasswordCode(request);
        Log.d(TAG, "发送验证码请求");

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                Log.d(TAG, "收到发送验证码响应");
                Log.d(TAG, "响应状态码: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "发送验证码响应成功: " + authResponse.isSuccess());
                    Log.d(TAG, "响应消息: " + (authResponse.getMessage() != null ? authResponse.getMessage() : "无消息"));

                    if (authResponse.isSuccess()) {
                        Toast.makeText(ResetPassActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                        startCountDown();
                    } else {
                        String errorMessage = authResponse.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "发送验证码失败，请稍后重试";
                        }
                        Log.d(TAG, "发送验证码失败: " + errorMessage);
                        handleSendCodeError(errorMessage);
                        sendCodeButton.setEnabled(true);
                    }
                } else {
                    String errorMessage = "发送验证码失败";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应失败", e);
                    }
                    Log.e(TAG, "发送验证码请求失败: " + errorMessage);
                    handleSendCodeError(errorMessage);
                    sendCodeButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "发送验证码请求失败: " + t.getMessage(), t);
                sendCodeButton.setEnabled(true);
                Toast.makeText(ResetPassActivity.this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword(String email, String code, String newPassword) {
        Log.d(TAG, "开始修改密码");
        Log.d(TAG, "邮箱: " + email);

        // 禁用修改按钮
        resetButton.setEnabled(false);
        resetButton.setText("修改中...");

        // 创建请求对象
        ResetPasswordVerifyRequest request = new ResetPasswordVerifyRequest(email, code, newPassword);
        Log.d(TAG, "创建修改密码请求对象");

        // 发送请求
        Call<AuthResponse> call = ApiClient.getApiService().resetPassword(request);
        Log.d(TAG, "发送修改密码请求");

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                Log.d(TAG, "收到修改密码响应");
                Log.d(TAG, "响应状态码: " + response.code());

                // 恢复按钮状态
                resetButton.setEnabled(true);
                resetButton.setText("修改密码");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "修改密码响应成功: " + authResponse.isSuccess());
                    Log.d(TAG, "响应消息: " + (authResponse.getMessage() != null ? authResponse.getMessage() : "无消息"));

                    if (authResponse.isSuccess()) {
                        Toast.makeText(ResetPassActivity.this, "密码修改成功，请重新登录", Toast.LENGTH_SHORT).show();
                        // 清除用户数据
                        BeYourEyesApplication.getInstance().clearUserData();
                        Log.d(TAG, "用户数据已清除");
                        // 延迟跳转到登录页面
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(ResetPassActivity.this, LoginActivity.class);
                            intent.putExtra("email", email);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // 清除任务栈并启动新的任务栈
                            startActivity(intent);
                            finish();
                        }, 1500);
                    } else {
                        String errorMessage = authResponse.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "修改密码失败，请稍后重试";
                        }
                        Log.d(TAG, "修改密码失败: " + errorMessage);
                        handleResetPasswordError(errorMessage);
                    }
                } else {
                    String errorMessage = "修改密码失败";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应失败", e);
                    }
                    Log.e(TAG, "修改密码请求失败: " + errorMessage);
                    handleResetPasswordError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "修改密码请求失败: " + t.getMessage(), t);
                resetButton.setEnabled(true);
                resetButton.setText("修改密码");
                Toast.makeText(ResetPassActivity.this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSendCodeError(String errorMessage) {
        if (errorMessage == null) {
            Toast.makeText(this, "发送验证码失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清除所有输入框的错误状态
        clearAllErrors();

        if (errorMessage.contains("用户不存在") || errorMessage.contains("邮箱未注册")) {
            emailLayout.setError("该邮箱未注册，请先注册账号");
            emailLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("邮箱格式错误")) {
            emailLayout.setError("邮箱格式错误，请检查后重试");
            emailLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("发送过于频繁") || errorMessage.contains("请稍后再试")) {
            Toast.makeText(this, "验证码发送过于频繁，请1分钟后再试", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("服务器错误") || errorMessage.contains("服务器异常")) {
            Toast.makeText(this, "服务器暂时无法响应，请稍后重试", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("网络错误") || errorMessage.contains("连接失败")) {
            Toast.makeText(this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "发送验证码失败：" + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleResetPasswordError(String errorMessage) {
        if (errorMessage == null) {
            Toast.makeText(this, "修改密码失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清除所有输入框的错误状态
        clearAllErrors();

        if (errorMessage.contains("验证码错误") || errorMessage.contains("验证码无效")) {
            codeLayout.setError("验证码错误，请重新输入");
            codeLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("验证码已过期")) {
            codeLayout.setError("验证码已过期，请重新获取");
            codeLayout.setErrorEnabled(true);
            Toast.makeText(this, "验证码已过期，请重新获取", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("用户不存在") || errorMessage.contains("邮箱未注册")) {
            emailLayout.setError("该邮箱未注册，请先注册账号");
            emailLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("密码格式错误")) {
            passwordLayout.setError("密码格式错误，密码长度需在6-20位之间");
            passwordLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("密码过于简单")) {
            passwordLayout.setError("密码过于简单，请包含字母和数字");
            passwordLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("两次密码不一致")) {
            confirmPasswordLayout.setError("两次输入的密码不一致");
            confirmPasswordLayout.setErrorEnabled(true);
        } else if (errorMessage.contains("服务器错误") || errorMessage.contains("服务器异常")) {
            Toast.makeText(this, "服务器暂时无法响应，请稍后重试", Toast.LENGTH_SHORT).show();
        } else if (errorMessage.contains("网络错误") || errorMessage.contains("连接失败")) {
            Toast.makeText(this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "修改密码失败：" + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllErrors() {
        emailLayout.setErrorEnabled(false);
        codeLayout.setErrorEnabled(false);
        passwordLayout.setErrorEnabled(false);
        confirmPasswordLayout.setErrorEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}