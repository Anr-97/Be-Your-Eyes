package xyz.lanshive.beyoureyes.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import xyz.lanshive.beyoureyes.R;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;
import xyz.lanshive.beyoureyes.LoginActivity;
import xyz.lanshive.beyoureyes.ResetPasswordActivity;
import xyz.lanshive.beyoureyes.ui.profile.PrivacyPolicyActivity;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextView userEmail, userRole, versionText;
    private View changePasswordLayout, logoutLayout, notificationLayout, privacyLayout, versionInfoLayout;
    private SwitchMaterial notificationSwitch;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化权限请求启动器
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                notificationSwitch.setChecked(isGranted);
                if (isGranted) {
                    Toast.makeText(requireContext(), "已开启通知权限", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "通知权限已关闭", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "通知权限请求结果: " + isGranted);
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 初始化控件
        userEmail = view.findViewById(R.id.userEmail);
        userRole = view.findViewById(R.id.userRole);
        versionText = view.findViewById(R.id.versionText);
        changePasswordLayout = view.findViewById(R.id.changePasswordLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
        notificationLayout = view.findViewById(R.id.notificationLayout);
        privacyLayout = view.findViewById(R.id.privacyLayout);
        versionInfoLayout = view.findViewById(R.id.versionInfoLayout);
        notificationSwitch = view.findViewById(R.id.notificationSwitch);

        // 设置点击事件
        setupClickListeners();

        // 加载用户信息
        loadUserInfo();

        // 检查通知权限状态
        checkNotificationPermission();

        return view;
    }

    private void setupClickListeners() {
        // 修改密码
        changePasswordLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ResetPasswordActivity.class);
            startActivity(intent);
        });

        // 退出登录
        logoutLayout.setOnClickListener(v -> {
            logout();
        });

        // 推送通知设置
        notificationLayout.setOnClickListener(v -> {
            toggleNotificationPermission();
        });

        // 隐私条款
        privacyLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        // 版本信息
        versionInfoLayout.setOnClickListener(v -> {
            showVersionInfoDialog();
        });
    }

    private void checkNotificationPermission() {
        boolean isEnabled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isEnabled = ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            isEnabled = isNotificationEnabled();
        }
        notificationSwitch.setChecked(isEnabled);
        Log.d(TAG, "通知权限状态: " + isEnabled);
    }

    private boolean isNotificationEnabled() {
        String packageName = requireContext().getPackageName();
        String flat = Settings.Secure.getString(requireContext().getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(packageName);
    }

    private void toggleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 使用新的权限请求API
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // 打开应用设置页面
                openAppSettings();
            }
        } else {
            // 打开应用设置页面
            openAppSettings();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        Toast.makeText(requireContext(), "请在系统设置中修改通知权限", Toast.LENGTH_SHORT).show();
    }

private void showVersionInfoDialog() {
    // 创建对话框
    Dialog dialog = new Dialog(requireContext());
    dialog.setContentView(R.layout.dialog_version_info);

    // 设置对话框宽度为屏幕宽度的80%
    Window window = dialog.getWindow();
    if (window != null) {
        // 移除对话框默认背景
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
        // 重新应用修改后的布局参数
        window.setAttributes(params);
    }

    // 设置关闭按钮点击事件
    ImageView closeButton = dialog.findViewById(R.id.closeButton);
    closeButton.setOnClickListener(v -> dialog.dismiss());

    // 设置版本信息
    TextView versionValue = dialog.findViewById(R.id.versionValue);
    TextView buildValue = dialog.findViewById(R.id.buildValue);
    TextView dateValue = dialog.findViewById(R.id.dateValue);

    try {
        PackageInfo packageInfo = requireContext().getPackageManager()
               .getPackageInfo(requireContext().getPackageName(), 0);

        versionValue.setText(packageInfo.versionName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            buildValue.setText(String.valueOf(packageInfo.getLongVersionCode()));
        }
        dateValue.setText("2024-03-22"); // TODO: 从服务器获取实际更新日期

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "显示版本信息 - 版本号: " + packageInfo.versionName
                    + ", 构建号: " + packageInfo.getLongVersionCode());
        }
    } catch (PackageManager.NameNotFoundException e) {
        Log.e(TAG, "获取版本信息失败", e);
    }

    // 显示对话框
    dialog.show();
}

    private void loadUserInfo() {
        BeYourEyesApplication app = BeYourEyesApplication.getInstance();
        
        // 获取并显示用户邮箱
        String email = app.getUserEmail();
        if (email != null && !email.isEmpty()) {
            userEmail.setText(email);
        } else {
            userEmail.setText("未设置");
        }
        
        // 获取并显示用户角色
        String role = app.getCurrentUserRole();
        if (role != null && !role.isEmpty()) {
            String roleText = role.equals(BeYourEyesApplication.ROLE_BLIND) ? "视障用户" : "志愿者";
            userRole.setText(roleText);
        } else {
            userRole.setText("未设置");
        }
        
        // 设置版本信息
        String versionName = "v1.0.0"; // TODO: 从BuildConfig获取实际版本号
        versionText.setText(versionName);
        
        Log.d(TAG, "加载用户信息 - 邮箱: " + email + ", 角色: " + role);
    }

    private void logout() {
        Log.d(TAG, "开始执行退出登录");
        
        // 清除用户数据
        BeYourEyesApplication.getInstance().clearUserData();
        Log.d(TAG, "用户数据已清除");

        // 跳转到登录页面
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Log.d(TAG, "已跳转到登录页面");
    }
} 