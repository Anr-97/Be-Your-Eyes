package top.lanshive.beyoureyes;

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

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private Button loginButton;
    private TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图
        emailInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);

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
                } else if (!isValidEmail(email)) {
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

        // 设置登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("请输入邮箱");
                emailLayout.setErrorEnabled(true);
                return;
            }

            if (!isValidEmail(email)) {
                emailLayout.setError("请输入有效的邮箱地址");
                emailLayout.setErrorEnabled(true);
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

            // TODO: 实现登录逻辑
            // 这里先模拟登录成功
            loginSuccess();
        });

        // 设置注册链接点击事件
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void loginSuccess() {
        try {
            // 跳转到主页
            Intent intent = new Intent(this, MainActivity.class);
            // 清除任务栈中的其他活动
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting MainActivity", e);
            Toast.makeText(this, "登录失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
} 