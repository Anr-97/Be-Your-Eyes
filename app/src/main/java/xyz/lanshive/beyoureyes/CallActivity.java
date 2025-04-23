package xyz.lanshive.beyoureyes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;

import org.webrtc.EglBase;
import org.webrtc.SessionDescription;
import org.webrtc.IceCandidate;

import xyz.lanshive.beyoureyes.api.ApiClient;
import xyz.lanshive.beyoureyes.databinding.ActivityCallBinding;
import xyz.lanshive.beyoureyes.model.CallRequest;
import xyz.lanshive.beyoureyes.model.CallResponse;
import xyz.lanshive.beyoureyes.socket.SignalingClient;
import xyz.lanshive.beyoureyes.socket.SignalingListener;
import xyz.lanshive.beyoureyes.webrtc.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.UUID;

public class CallActivity extends AppCompatActivity implements WebRTCManager.WebRTCListener, SignalingListener {
    private static final String TAG = "CallActivity";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private ActivityCallBinding binding;
    private String roomId;
    private String callToken;
    private boolean isCallActive = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private WebRTCManager webRTCManager;
    private SignalingClient signalingClient;
    private EglBase rootEglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化EGL上下文
        rootEglBase = EglBase.create();

        // 设置返回键处理
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmEndCall();
            }
        });

        setupUI();
        checkPermissions();
    }

    private void setupUI() {
        // 设置挂断按钮点击事件
        binding.endCallButton.setOnClickListener(v -> confirmEndCall());

        // 初始化加载进度视图
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.callStatusText.setText("正在准备通话...");
        binding.endCallButton.setVisibility(View.GONE);

        // 隐藏远程视图（盲人用户不需要看到对方视频）
        if (binding.remoteVideoView != null) {
            binding.remoteVideoView.setVisibility(View.GONE);
        }
    }

    private void checkPermissions() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            startCall();
        } else {
            // 检查是否需要显示权限说明
            boolean shouldShowRationale = false;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                showPermissionExplanationDialog();
            } else {
                ActivityCompat.requestPermissions(this,
                        REQUIRED_PERMISSIONS,
                        REQUEST_PERMISSIONS);
            }
        }
    }

    private void startCall() {
        Log.d(TAG, "开始通话初始化");
        
        // 检查是否已经在通话中
        if (isCallActive) {
            Log.w(TAG, "通话已经激活，跳过初始化");
            return;
        }
        
        binding.callStatusText.setText("正在连接服务器...");

        String email = BeYourEyesApplication.getInstance().getUserEmail();
        String token = BeYourEyesApplication.getInstance().getAuthToken();

        if (email == null || token == null) {
            showErrorAndFinish("用户未登录，请重新登录后再试");
            return;
        }

        // 使用BeYourEyesApplication中的信令客户端
        signalingClient = BeYourEyesApplication.getInstance().getSignalingClient();
        if (signalingClient == null) {
            showErrorAndFinish("信令客户端未初始化");
            return;
        }

        // 获取房间ID和token
        roomId = getIntent().getStringExtra("room_id");
        callToken = getIntent().getStringExtra("token");
        if (roomId == null || callToken == null) {
            // 如果没有从Intent获取到，则生成新的
            roomId = UUID.randomUUID().toString();
            callToken = token;
        }

        // 初始化WebRTC管理器
        Log.d(TAG, "初始化WebRTC管理器");
        webRTCManager = new WebRTCManager(this, signalingClient, this);
        
        // 只初始化本地视图，远程视图传null
        Log.d(TAG, "初始化视频视图");
        webRTCManager.initializeSurfaceViews(binding.localVideoView, null);
        
        // 延迟初始化WebRTC连接，等待视图初始化完成
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "初始化WebRTC连接");
            webRTCManager.initialize(roomId, callToken, true); // true表示是呼叫方
            
            // 设置通话状态为激活
            isCallActive = true;
            Log.d(TAG, "通话初始化完成");
        }, 500); // 延迟500ms确保视图初始化完成
    }

    private Handler handler = new Handler(Looper.getMainLooper());

    private void handleStartCallError() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            binding.callStatusText.setText("连接失败，正在重试(" + retryCount + "/" + MAX_RETRY_COUNT + ")...");
            // 使用Handler延迟2秒后重试
            handler.postDelayed(() -> {
                if (!isFinishing() && !isCallActive) {
                    startCall();
                }
            }, 2000);
        } else {
            showErrorAndFinish("无法连接到服务器，请检查网络后重试");
        }
    }

    private void showErrorAndFinish(String message) {
        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage(message)
                    .setPositiveButton("确定", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    private void confirmEndCall() {
        new AlertDialog.Builder(this)
                .setTitle("结束通话")
                .setMessage("确定要结束当前通话吗？")
                .setPositiveButton("结束", (dialog, which) -> endCall())
                .setNegativeButton("取消", null)
                .show();
    }

    private void endCall() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.callStatusText.setText("正在结束通话...");

        // 如果通话未开始，直接结束Activity
        if (!isCallActive) {
            finish();
            return;
        }

        // 直接断开信令连接
        if (signalingClient != null) {
            signalingClient.disconnect();
        }

        cleanupAndFinish();
    }

    private void cleanupAndFinish() {
        isCallActive = false;
        // 清理WebRTC资源
        if (webRTCManager != null) {
            webRTCManager.disconnect();
            webRTCManager = null;
        }
        // 不需要手动释放rootEglBase，因为WebRTCManager会在disconnect中处理
        // 结束Activity
        finish();
    }

    // WebRTCListener接口实现
    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.callStatusText.setText("通话中");
            binding.endCallButton.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onCallDisconnected() {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                showErrorAndFinish("通话已断开");
            }
        });
    }

    @Override
    public void onCallFailed(String error) {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                showErrorAndFinish("通话失败: " + error);
            }
        });
    }

    @Override
    public void onRemoteStreamAdded() {
        runOnUiThread(() -> {
            Log.d(TAG, "远程流已添加");
            // 虽然我们不显示远程视频，但连接成功了
            binding.callStatusText.setText("通话中");
        });
    }

    @Override
    public void onRemoteStreamRemoved() {
        runOnUiThread(() -> {
            Log.d(TAG, "远程流已移除");
            if (!isFinishing()) {
                showErrorAndFinish("对方已断开连接");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startCall();
            } else {
                // 检查是否所有权限都被永久拒绝
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (!shouldShowRationale) {
                    showPermissionExplanationDialog();
                } else {
                    showErrorAndFinish("需要相机和麦克风权限才能进行视频通话");
                }
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要权限")
                .setMessage("视频通话需要相机和麦克风权限才能正常工作。请在设置中启用这些权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    // 跳转到应用设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果通话仍在进行中，结束通话
        if (isCallActive) {
            endCall();
        }
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "已连接到信令服务器");
        runOnUiThread(() -> {
            binding.callStatusText.setText("正在等待志愿者接听...");
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "与信令服务器断开连接");
        runOnUiThread(() -> {
            if (!isFinishing()) {
                showErrorAndFinish("与服务器断开连接");
            }
        });
    }

    @Override
    public void onRemoteSdpReceived(SessionDescription sdp) {
        Log.d(TAG, "收到远程SDP: " + sdp.type);
        if (webRTCManager != null) {
            webRTCManager.onRemoteSdpReceived(sdp);
        }
    }

    @Override
    public void onRemoteIceCandidateReceived(IceCandidate candidate) {
        // 转发给WebRTCManager处理
        if (webRTCManager != null) {
            webRTCManager.onRemoteIceCandidateReceived(candidate);
        }
    }

    @Override
    public void onCallAccepted(String roomId) {
        Log.d(TAG, "收到通话接受事件，房间ID: " + roomId);
        runOnUiThread(() -> {
            binding.callStatusText.setText("通话已被接受，正在建立连接...");
        });
        
        // 转发给WebRTCManager处理
        if (webRTCManager != null) {
            webRTCManager.onCallAccepted(roomId);
        }
    }

    @Override
    public void onCallTimeout() {
        // 处理通话超时的逻辑
        runOnUiThread(() -> {
            // 显示通话超时提示
            Toast.makeText(this, "通话请求超时", Toast.LENGTH_SHORT).show();
            // 结束当前Activity
            finish();
        });
    }

    @Override
    public void onCallQueued(int position) {
        // 处理通话排队的逻辑
        runOnUiThread(() -> {
            binding.callStatusText.setText("您的通话请求已排队，当前位置: " + position);
            Toast.makeText(this, "通话请求已排队，请耐心等待", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onCallRejected(String reason) {
        // 处理通话被拒绝的逻辑
        runOnUiThread(() -> {
            Log.d(TAG, "通话请求被拒绝: " + reason);
            Toast.makeText(this, "通话请求被拒绝: " + reason, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}