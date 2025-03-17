package top.lanshive.beyoureyes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private TextInputEditText emailInput;
    private TextInputEditText verificationCodeInput;
    private TextInputEditText passwordInput;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private Button sendCodeButton;
    private Button registerButton;
    private TextView loginLink;
    private int countdown = 60;
    private boolean isCountingDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化视图
        emailInput = findViewById(R.id.phoneInput);
        verificationCodeInput = findViewById(R.id.verificationCodeInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        // 设置邮箱输入监听
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
                } else if (isValidEmail(email)) {
                    emailLayout.setError("请输入有效的邮箱地址");
                    emailLayout.setErrorEnabled(true);
                } else {
                    emailLayout.setErrorEnabled(false);
                }
            }
        });

        // 设置密码输入监听
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

        // 设置发送验证码按钮点击事件
        sendCodeButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("请输入邮箱");
                emailLayout.setErrorEnabled(true);
                return;
            }

            if (isValidEmail(email)) {
                emailLayout.setError("请输入有效的邮箱地址");
                emailLayout.setErrorEnabled(true);
                return;
            }

            // TODO: 实现发送验证码逻辑
            startCountdown();
        });

        // 设置注册按钮点击事件
        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String code = verificationCodeInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("请输入邮箱");
                emailLayout.setErrorEnabled(true);
                return;
            }

            if (isValidEmail(email)) {
                emailLayout.setError("请输入有效的邮箱地址");
                emailLayout.setErrorEnabled(true);
                return;
            }

            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidVerificationCode(code)) {
                Toast.makeText(this, "验证码格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError("请输入密码");
                passwordLayout.setErrorEnabled(true);
                return;
            }

            if (password.length() < 8) {
                passwordLayout.setError("密码长度不能少于8个字符");
                passwordLayout.setErrorEnabled(true);
                return;
            }

            // TODO: 实现注册逻辑
            // 这里先模拟注册成功
            registerSuccess();
        });

        // 设置登录链接点击事件
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isValidEmail(String email) {
        return TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches();
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

    private void registerSuccess() {
        try {
            // 跳转到主页
            Intent intent = new Intent(this, MainActivity.class);
            // 清除任务栈中的其他活动
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting MainActivity", e);
            Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
} 