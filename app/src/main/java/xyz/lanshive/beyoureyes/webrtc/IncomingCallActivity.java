package xyz.lanshive.beyoureyes.webrtc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.SessionDescription;
import org.webrtc.SdpObserver;

import xyz.lanshive.beyoureyes.BeYourEyesApplication;
import xyz.lanshive.beyoureyes.R;
import xyz.lanshive.beyoureyes.utils.RingtonePlayer;
import xyz.lanshive.beyoureyes.socket.SignalingClient;
import xyz.lanshive.beyoureyes.socket.SignalingListener;

public class IncomingCallActivity extends AppCompatActivity implements WebRTCManager.WebRTCListener, SignalingListener {
    private static final String TAG = "IncomingCallActivity";
    
    private FrameLayout acceptButton;
    private FrameLayout rejectButton;
    private SurfaceViewRenderer remoteVideoView;
    private WebRTCManager webRTCManager;
    private String roomId;
    private String token;
    private boolean isCallActive = false;
    private boolean isDestroying = false;
    private EglBase rootEglBase;
    
    // 实现SdpObserver接口
    private class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "SDP创建成功: " + sessionDescription.type);
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "SDP设置成功");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "SDP创建失败: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "SDP设置失败: " + s);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 开始创建Activity");
        setContentView(R.layout.activity_incoming_call);
        
        // 初始化EGL上下文
        rootEglBase = EglBase.create();
        Log.d(TAG, "onCreate: EGL上下文已创建");
        
        // 初始化视图
        acceptButton = findViewById(R.id.btnContainerAccept);
        rejectButton = findViewById(R.id.btnContainerReject);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        
        // 初始化远程视频视图
        if (remoteVideoView != null) {
            remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
            remoteVideoView.setEnableHardwareScaler(true);
            remoteVideoView.setMirror(false);
            Log.d(TAG, "onCreate: 远程视频视图已初始化");
        } else {
            Log.e(TAG, "onCreate: remoteVideoView为空");
            finish();
            return;
        }
        
        // 获取来电信息
        String callerId = getIntent().getStringExtra("caller_id");
        String requirements = getIntent().getStringExtra("requirements");
        roomId = getIntent().getStringExtra("request_id");
        token = BeYourEyesApplication.getInstance().getAuthToken();
        
        Log.d(TAG, "onCreate: 获取到参数 - callerId=" + callerId + ", requirements=" + requirements + 
              ", roomId=" + roomId + ", token=" + (token != null ? "已设置" : "未设置"));
        
        // 初始化WebRTC
        SignalingClient signalingClient = BeYourEyesApplication.getInstance().getSignalingClient();
        if (signalingClient == null) {
            Log.e(TAG, "onCreate: 信令客户端为空");
            finish();
            return;
        }
        
        webRTCManager = new WebRTCManager(this, signalingClient, this);
        webRTCManager.initializeSurfaceViews(null, remoteVideoView);
        Log.d(TAG, "onCreate: WebRTC管理器初始化完成");
        
        // 在接听按钮的点击事件处理中
        acceptButton.setOnClickListener(v -> {
            Log.d(TAG, "接听按钮被点击");
            if (isDestroying) {
                Log.w(TAG, "接听按钮被点击时Activity正在销毁");
                return;
            }
            
            // 接受通话
            acceptCall();
        });
        
        // 在拒绝按钮的点击事件处理中
        rejectButton.setOnClickListener(v -> {
            Log.d(TAG, "拒绝按钮被点击");
            if (isDestroying) {
                Log.w(TAG, "拒绝按钮被点击时Activity正在销毁");
                return;
            }
            
            // 拒绝通话
            rejectCall();
        });
    }
    
    private void acceptCall() {
        Log.d(TAG, "开始接受通话");
        if (isCallActive) {
            Log.w(TAG, "通话已经激活，跳过接受操作");
            return;
        }
        if (isDestroying) {
            Log.w(TAG, "Activity正在销毁，跳过接受操作");
            return;
        }
        
        // 确保MediaPlayer完全释放
        try {
            RingtonePlayer.getInstance(this).stopRingtone();
        } catch (Exception e) {
            Log.e(TAG, "停止铃声时出错", e);
        }
        
        isCallActive = true;
        Log.d(TAG, "设置通话状态为激活");
        
        // 创建PeerConnection并初始化
        webRTCManager.createPeerConnection();
        Log.d(TAG, "已创建PeerConnection");
        
        // 通知SignalingClient接受通话
        BeYourEyesApplication.getInstance().getSignalingClient().acceptCall(roomId, null);
        Log.d(TAG, "已通知信令服务器接受通话");
        
        // 初始化WebRTC连接，禁用本地视频
        webRTCManager.initialize(roomId, token, false); // false表示是被呼叫方
        Log.d(TAG, "已初始化WebRTC连接");
        
        // 显示远程视频视图
        runOnUiThread(() -> {
            if (remoteVideoView != null) {
                remoteVideoView.setVisibility(View.VISIBLE);
                remoteVideoView.setZOrderMediaOverlay(true);
                Log.d(TAG, "已显示远程视频视图");
            }
        });
    }
    
    private void rejectCall() {
        Log.d(TAG, "开始拒绝通话");
        if (isDestroying) {
            Log.w(TAG, "Activity正在销毁，跳过拒绝操作");
            return;
        }
        
        // 通知SignalingClient拒绝通话
        if (roomId != null) {
            BeYourEyesApplication.getInstance().getSignalingClient().rejectCall(roomId, "用户拒绝通话");
            Log.d(TAG, "已通知信令服务器拒绝通话");
        } else {
            Log.w(TAG, "roomId为空，无法通知信令服务器");
        }
        
        // 结束当前界面
        finish();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity暂停，保持连接");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity停止，保持连接");
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: 开始销毁Activity");
        isDestroying = true;
        
        // 确保停止铃声和振动
        try {
            RingtonePlayer.getInstance(this).stopRingtone();
        } catch (Exception e) {
            Log.e(TAG, "停止铃声时出错", e);
        }
        Log.d(TAG, "已停止铃声和振动");
        
        // 清理视频视图
        if (remoteVideoView != null) {
            remoteVideoView.release();
            remoteVideoView = null;
            Log.d(TAG, "已释放远程视频视图");
        }
        
        // 只有在通话未激活时才清理WebRTC资源
        if (!isCallActive) {
            Log.d(TAG, "通话未激活，开始清理WebRTC资源");
            if (webRTCManager != null) {
                webRTCManager.disconnect();
                webRTCManager = null;
                Log.d(TAG, "WebRTC资源已清理");
            }
        } else {
            Log.d(TAG, "通话已激活，保持WebRTC连接");
        }
        
        // 释放EGL上下文
        if (rootEglBase != null) {
            rootEglBase.release();
            rootEglBase = null;
            Log.d(TAG, "已释放EGL上下文");
        }
        
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity销毁完成");
    }

    // WebRTCListener接口实现
    @Override
    public void onCallConnected() {
        Log.d(TAG, "onCallConnected: 通话已连接");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onCallConnected: Activity正在销毁，跳过UI更新");
                return;
            }
            
            // 通话连接成功后，隐藏接听和拒绝按钮
            acceptButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            Log.d(TAG, "onCallConnected: 已隐藏接听和拒绝按钮");
        });
    }

    @Override
    public void onCallDisconnected() {
        Log.d(TAG, "onCallDisconnected: 通话已断开");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onCallDisconnected: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onCallDisconnected: 结束Activity");
                finish();
            }
        });
    }

    @Override
    public void onCallFailed(String error) {
        Log.e(TAG, "onCallFailed: 通话失败 - " + error);
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onCallFailed: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onCallFailed: 结束Activity");
                finish();
            }
        });
    }

    @Override
    public void onRemoteStreamAdded() {
        Log.d(TAG, "onRemoteStreamAdded: 远程流已添加");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onRemoteStreamAdded: Activity正在销毁，跳过UI更新");
                return;
            }
            
            // 远程视频流添加后，显示远程视频视图
            if (remoteVideoView != null) {
                remoteVideoView.setVisibility(View.VISIBLE);
                remoteVideoView.setZOrderMediaOverlay(true);
                Log.d(TAG, "onRemoteStreamAdded: 已显示远程视频视图");
            } else {
                Log.w(TAG, "onRemoteStreamAdded: remoteVideoView为空");
            }
        });
    }

    @Override
    public void onRemoteStreamRemoved() {
        Log.d(TAG, "onRemoteStreamRemoved: 远程流已移除");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onRemoteStreamRemoved: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onRemoteStreamRemoved: 结束Activity");
                finish();
            }
        });
    }

    // SignalingListener接口实现
    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: 已连接到信令服务器");
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: 与信令服务器断开连接");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onDisconnected: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onDisconnected: 结束Activity");
                finish();
            }
        });
    }

    @Override
    public void onRemoteSdpReceived(SessionDescription sdp) {
        Log.d(TAG, "接收到远程SDP: " + sdp.type);
        if (sdp.type == SessionDescription.Type.OFFER) {
            Log.d(TAG, "收到Offer，准备创建Answer");
            if (webRTCManager != null) {
                // 设置远程描述
                webRTCManager.setRemoteDescription(sdp);
                // 创建Answer
                webRTCManager.createAnswer();
            }
        } else if (sdp.type == SessionDescription.Type.ANSWER) {
            Log.d(TAG, "收到Answer，准备建立连接");
            if (webRTCManager != null) {
                webRTCManager.setRemoteDescription(sdp);
            }
        }
    }

    @Override
    public void onRemoteIceCandidateReceived(org.webrtc.IceCandidate candidate) {
        Log.d(TAG, "onRemoteIceCandidateReceived: 收到远程ICE候选");
        if (isDestroying) {
            Log.w(TAG, "onRemoteIceCandidateReceived: Activity正在销毁，跳过处理");
            return;
        }
        
        if (webRTCManager != null) {
            webRTCManager.addIceCandidate(candidate);
        } else {
            Log.w(TAG, "onRemoteIceCandidateReceived: webRTCManager为空");
        }
    }

    @Override
    public void onCallAccepted(String roomId) {
        Log.d(TAG, "onCallAccepted: 通话已被接受 - " + roomId);
        if (isDestroying) {
            Log.w(TAG, "onCallAccepted: Activity正在销毁，跳过处理");
            return;
        }
        
        if (webRTCManager != null) {
            webRTCManager.onCallAccepted(roomId);
        } else {
            Log.w(TAG, "onCallAccepted: webRTCManager为空");
        }
    }

    @Override
    public void onCallRejected(String reason) {
        Log.d(TAG, "onCallRejected: 通话被拒绝 - " + reason);
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onCallRejected: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onCallRejected: 结束Activity");
                finish();
            }
        });
    }

    @Override
    public void onCallQueued(int position) {
        Log.d(TAG, "onCallQueued: 通话已排队 - 位置: " + position);
    }

    @Override
    public void onCallTimeout() {
        Log.d(TAG, "onCallTimeout: 通话超时");
        runOnUiThread(() -> {
            if (isDestroying) {
                Log.w(TAG, "onCallTimeout: Activity正在销毁，跳过处理");
                return;
            }
            
            isCallActive = false;
            if (!isFinishing()) {
                Log.d(TAG, "onCallTimeout: 结束Activity");
                finish();
            }
        });
    }
}