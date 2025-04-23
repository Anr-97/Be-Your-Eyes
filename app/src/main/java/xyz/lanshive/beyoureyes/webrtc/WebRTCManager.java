package xyz.lanshive.beyoureyes.webrtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import xyz.lanshive.beyoureyes.socket.SignalingClient;
import xyz.lanshive.beyoureyes.socket.SignalingListener;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;

/**
 * WebRTC核心管理类，负责管理WebRTC连接、媒体流和信令
 */
public class WebRTCManager implements SignalingListener {
    private static final String TAG = "WebRTCManager";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String LOCAL_MEDIA_STREAM_ID = "ARDAMS";

    // 视频优化的Field Trial配置
    private static final String VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    private static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    private static final String DISABLE_WEBRTC_AGC_FIELDTRIAL = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";

    private final Context context;
    private final SignalingClient signalingClient;
    private final WebRTCListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EglBase rootEglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private MediaStream localMediaStream;

    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;

    private String roomId;
    private String token;
    private boolean isCaller;
    private List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
    private boolean isPeerConnectionReady = false;

    /**
     * WebRTC事件监听器接口
     */
    public interface WebRTCListener {
        void onCallConnected();
        void onCallDisconnected();
        void onCallFailed(String error);
        void onRemoteStreamAdded();
        void onRemoteStreamRemoved();
    }

    /**
     * 构造函数
     *
     * @param context 应用上下文
     * @param signalingClient 信令客户端
     * @param listener WebRTC事件监听器
     */
    public WebRTCManager(Context context, SignalingClient signalingClient, WebRTCListener listener) {
        this.context = context;
        this.signalingClient = signalingClient;
        this.listener = listener;
        this.rootEglBase = EglBase.create();
        
        // 设置signalingClient到BeYourEyesApplication
        if (signalingClient != null) {
            BeYourEyesApplication.getInstance().setSignalingClient(signalingClient);
            Log.d(TAG, "已设置signalingClient到BeYourEyesApplication");
        } else {
            Log.e(TAG, "signalingClient为null，无法设置到BeYourEyesApplication");
        }
        
        initializePeerConnectionFactory();
    }

    /**
     * 初始化PeerConnectionFactory
     */
    private void initializePeerConnectionFactory() {
        // 创建EglBase实例
        rootEglBase = EglBase.create();

        // 配置WebRTC初始化参数
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setFieldTrials(VIDEO_FLEXFEC_FIELDTRIAL +
                                       VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL +
                                       DISABLE_WEBRTC_AGC_FIELDTRIAL)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        // 创建PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(
                rootEglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    /**
     * 初始化视频渲染视图
     *
     * @param localView 本地视频视图
     * @param remoteView 远程视频视图，可以为null
     */
    public void initializeSurfaceViews(SurfaceViewRenderer localView, SurfaceViewRenderer remoteView) {
        Log.d(TAG, "开始初始化视频视图");
        this.localRenderer = localView;
        this.remoteRenderer = remoteView;

        // 初始化本地视图
        if (localRenderer != null) {
            try {
                localRenderer.init(rootEglBase.getEglBaseContext(), null);
                localRenderer.setMirror(false); // 盲人用户不需要镜像
                localRenderer.setEnableHardwareScaler(true);
                localRenderer.setZOrderMediaOverlay(true);
                Log.d(TAG, "本地视图初始化成功");
            } catch (IllegalStateException e) {
                Log.w(TAG, "本地视图已经初始化，跳过初始化步骤");
            }
        }

        // 初始化远程视图（如果有的话）
        if (remoteRenderer != null) {
            try {
                remoteRenderer.init(rootEglBase.getEglBaseContext(), null);
                remoteRenderer.setMirror(false);
                remoteRenderer.setEnableHardwareScaler(true);
                Log.d(TAG, "远程视图初始化成功");
            } catch (IllegalStateException e) {
                Log.w(TAG, "远程视图已经初始化，跳过初始化步骤");
            }
        }

        // 延迟创建本地媒体流，确保视图和EGL上下文完全初始化
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "开始创建本地媒体流");
            createLocalMediaStream();
        }, 1000); // 延迟1秒等待初始化完成
    }

    /**
     * 初始化WebRTC会话
     *
     * @param roomId 房间ID
     * @param token 用户令牌
     * @param isCaller 是否为呼叫发起方
     */
    public void initialize(String roomId, String token, boolean isCaller) {
        // 检查是否已经初始化
        if (this.roomId != null && this.token != null) {
            Log.w(TAG, "WebRTC已经初始化，跳过重复初始化");
            return;
        }

        this.roomId = roomId;
        this.token = token;
        this.isCaller = isCaller;

        // 连接信令服务器
        signalingClient.connect(roomId, token);
    }

    /**
     * 创建对等连接
     */
    public void createPeerConnection() {
        // 配置ICE服务器
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        // 添加TURN服务器以提高NAT穿透能力
        iceServers.add(PeerConnection.IceServer.builder("turn:www.lanshive.xyz:3478")
                .setUsername("lanshive")
                .setPassword("2382021041zl")
                .createIceServer());

        // 配置RTC连接参数
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN; // 明确设置使用Unified Plan
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;

        // 创建PeerConnection
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "信令状态变化: " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "ICE连接状态变化: " + iceConnectionState);
                executor.execute(() -> {
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED ||
                            iceConnectionState == PeerConnection.IceConnectionState.COMPLETED) {
                        Log.d(TAG, "ICE连接已建立，通话可以开始");
                        if (listener != null) {
                            listener.onCallConnected();
                        }
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.CHECKING) {
                        Log.d(TAG, "ICE正在检查连接...");
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED ||
                            iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                            iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                        Log.e(TAG, "ICE连接失败或断开: " + iceConnectionState);
                        if (listener != null) {
                            if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                                listener.onCallFailed("ICE连接失败");
                            } else {
                                listener.onCallDisconnected();
                            }
                        }
                    }
                });
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                Log.d(TAG, "连接状态变化: " + newState);
                executor.execute(() -> {
                    if (listener != null) {
                        if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                            Log.d(TAG, "WebRTC连接已建立，通话可以开始");
                            listener.onCallConnected();
                        } else if (newState == PeerConnection.PeerConnectionState.CONNECTING) {
                            Log.d(TAG, "WebRTC连接正在建立中...");
                        } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                                newState == PeerConnection.PeerConnectionState.FAILED ||
                                newState == PeerConnection.PeerConnectionState.CLOSED) {
                            Log.e(TAG, "WebRTC连接失败或断开: " + newState);
                            if (newState == PeerConnection.PeerConnectionState.FAILED) {
                                listener.onCallFailed("连接失败");
                            } else {
                                listener.onCallDisconnected();
                            }
                        }
                    }
                });
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {
                Log.d(TAG, "ICE连接接收状态变化: " + receiving);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "ICE收集状态变化: " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "新的ICE候选者");
                executor.execute(() -> signalingClient.sendIceCandidate(iceCandidate));
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "ICE候选者移除");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "添加远程媒体流");
                executor.execute(() -> {
                    if (mediaStream.videoTracks.size() > 0) {
                        VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                        if (remoteRenderer != null) {
                            Log.d(TAG, "设置远程视频轨道到远程视图");
                            videoTrack.addSink(remoteRenderer);
                        } else {
                            Log.w(TAG, "远程视图未初始化，无法显示远程视频");
                        }
                    } else {
                        Log.w(TAG, "远程媒体流中没有视频轨道");
                    }
                    if (listener != null) {
                        Log.d(TAG, "通知远程媒体流已添加");
                        listener.onRemoteStreamAdded();
                    }
                });
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "移除媒体流");
                if (listener != null) {
                    listener.onRemoteStreamRemoved();
                }
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "创建数据通道");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "需要重新协商");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "添加轨道");
            }
        });

        isPeerConnectionReady = true;

        // 处理队列中的远程ICE候选者
        for (IceCandidate candidate : queuedRemoteCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        queuedRemoteCandidates.clear();

        // 创建本地媒体流
        createLocalMediaStream();

        // 如果是呼叫方，创建Offer
        if (isCaller) {
            createOffer();
        }
    }

    /**
     * 创建本地媒体流
     */
    private void createLocalMediaStream() {
        Log.d(TAG, "开始创建本地媒体流");

        if (isCaller) {
            // 只有呼叫方才需要创建本地视频流
            if (localRenderer == null || rootEglBase == null) {
                Log.e(TAG, "本地视图或EGL上下文未初始化");
                if (listener != null) {
                    listener.onCallFailed("视频初始化失败");
                }
                return;
            }
        }

        try {
            // 如果已经存在本地媒体流，先释放
            if (localMediaStream != null) {
                Log.d(TAG, "释放已存在的本地媒体流");
                localMediaStream.dispose();
                localMediaStream = null;
            }

            localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
            Log.d(TAG, "创建本地媒体流成功");

            // 创建音频轨道
            Log.d(TAG, "创建音频轨道");
            audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
            localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localMediaStream.addTrack(localAudioTrack);
            Log.d(TAG, "音频轨道创建成功");

            // 只有呼叫方才需要创建视频轨道
            if (isCaller) {
                Log.d(TAG, "创建视频捕获器");
                videoCapturer = createVideoCapturer();
                if (videoCapturer == null) {
                    Log.e(TAG, "无法创建视频捕获器");
                    if (listener != null) {
                        listener.onCallFailed("无法访问摄像头");
                    }
                    return;
                }
                Log.d(TAG, "视频捕获器创建成功");

                Log.d(TAG, "初始化视频捕获器");
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
                videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
                Log.d(TAG, "视频捕获器初始化成功");

                Log.d(TAG, "启动视频捕获");
                videoCapturer.startCapture(1280, 720, 30); // 高清分辨率，30fps
                Log.d(TAG, "视频捕获启动成功");

                Log.d(TAG, "创建视频轨道");
                localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
                localVideoTrack.setEnabled(true);
                Log.d(TAG, "视频轨道创建成功");

                Log.d(TAG, "设置视频渲染器");
                localVideoTrack.addSink(localRenderer);
                localMediaStream.addTrack(localVideoTrack);
                Log.d(TAG, "视频渲染器设置成功");

                // 检查视频轨道状态
                if (localVideoTrack != null) {
                    Log.d(TAG, "视频轨道状态: enabled=" + localVideoTrack.enabled() +
                              ", state=" + localVideoTrack.state());
                }
            }

            // 如果对等连接已创建，添加媒体轨道
            if (peerConnection != null) {
                Log.d(TAG, "添加媒体轨道到对等连接");
                // 使用addTrack替代addStream
                if (localAudioTrack != null) {
                    peerConnection.addTrack(localAudioTrack, Collections.singletonList(LOCAL_MEDIA_STREAM_ID));
                }
                if (localVideoTrack != null) {
                    peerConnection.addTrack(localVideoTrack, Collections.singletonList(LOCAL_MEDIA_STREAM_ID));
                }
                Log.d(TAG, "本地媒体轨道添加完成");
            } else {
                Log.w(TAG, "对等连接未创建，等待连接创建后添加媒体轨道");
            }

        } catch (Exception e) {
            Log.e(TAG, "创建本地媒体流失败: " + e.getMessage(), e);
            if (listener != null) {
                listener.onCallFailed("创建本地媒体流失败: " + e.getMessage());
            }
        }
    }

    /**
     * 创建视频捕获器
     */
    private VideoCapturer createVideoCapturer() {
        Log.d(TAG, "开始创建视频捕获器");
        VideoCapturer videoCapturer = null;
        try {
            if (Camera2Enumerator.isSupported(context)) {
                Log.d(TAG, "使用Camera2 API");
                videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
            } else {
                Log.d(TAG, "使用Camera1 API");
                videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
            }
        } catch (Exception e) {
            Log.e(TAG, "创建视频捕获器失败: " + e.getMessage(), e);
        }
        return videoCapturer;
    }

    /**
     * 创建相机捕获器
     */
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        Log.d(TAG, "开始创建相机捕获器");
        final String[] deviceNames = enumerator.getDeviceNames();
        Log.d(TAG, "可用摄像头数量: " + deviceNames.length);

        // 优先尝试后置摄像头
        for (String deviceName : deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                Log.d(TAG, "尝试使用后置摄像头: " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    Log.d(TAG, "成功使用后置摄像头");
                    return videoCapturer;
                }
            }
        }

        // 如果后置摄像头不可用，尝试前置摄像头
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "尝试使用前置摄像头: " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    Log.d(TAG, "成功使用前置摄像头");
                    return videoCapturer;
                }
            }
        }

        // 如果找不到前置或后置摄像头，尝试任意可用摄像头
        for (String deviceName : deviceNames) {
            Log.d(TAG, "尝试使用摄像头: " + deviceName);
            VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
            if (videoCapturer != null) {
                Log.d(TAG, "成功使用摄像头: " + deviceName);
                return videoCapturer;
            }
        }

        Log.e(TAG, "找不到可用的摄像头");
        return null;
    }

    /**
     * 创建Offer
     */
    private void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.d(TAG, "创建Offer成功");
                executor.execute(() -> {
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            signalingClient.sendSdp(sdp);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "设置本地SDP失败: " + s);
                            if (listener != null) {
                                listener.onCallFailed("设置本地SDP失败: " + s);
                            }
                        }
                    }, sdp);
                });
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "创建Offer失败: " + s);
                if (listener != null) {
                    listener.onCallFailed("创建Offer失败: " + s);
                }
            }
        }, constraints);
    }

    /**
     * 设置远程SDP描述
     *
     * @param sdp 远程SDP描述
     */
    public void setRemoteDescription(SessionDescription sdp) {
        Log.d(TAG, "开始设置远程SDP描述: " + sdp.type);
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection为空，无法设置远程SDP");
            return;
        }
        
        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "设置远程SDP成功");
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "设置远程SDP失败: " + error);
                if (listener != null) {
                    listener.onCallFailed("设置远程SDP失败: " + error);
                }
            }
        }, sdp);
    }

    /**
     * 创建Answer
     */
    public void createAnswer() {
        Log.d(TAG, "开始创建Answer");
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection为空，无法创建Answer");
            return;
        }
        
        // 设置媒体约束
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        
        Log.d(TAG, "Answer媒体约束: " + constraints.mandatory.toString());
        
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.d(TAG, "成功创建Answer，SDP长度: " + sdp.description.length());
                executor.execute(() -> {
                    Log.d(TAG, "开始设置本地Answer");
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "成功设置本地Answer，准备发送到信令服务器");
                            // 通过信令服务器发送answer
                            signalingClient.sendAnswer(sdp);
                            Log.d(TAG, "Answer已发送到信令服务器");
                        }

                        @Override
                        public void onSetFailure(String error) {
                            Log.e(TAG, "设置本地Answer失败: " + error);
                            if (listener != null) {
                                listener.onCallFailed("设置本地Answer失败: " + error);
                            }
                        }
                    }, sdp);
                });
            }

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "创建Answer失败: " + error);
                if (listener != null) {
                    listener.onCallFailed("创建Answer失败: " + error);
                }
            }
        }, constraints);
    }

    /**
     * SignalingListener 接口实现
     */
    @Override
    public void onConnected() {
        Log.d(TAG, "信令服务器连接成功");
        executor.execute(() -> {
            try {
                Log.d(TAG, "开始创建对等连接");
                createPeerConnection();

                // 创建本地媒体流
                Log.d(TAG, "开始创建本地媒体流");
                createLocalMediaStream();

                // 如果是呼叫方，创建初始Offer用于通话请求
                if (isCaller) {
                    Log.d(TAG, "作为呼叫方，创建初始Offer用于通话请求");
                    createInitialOffer();
                } else {
                    Log.d(TAG, "作为被呼叫方，等待接收Offer");
                }
            } catch (Exception e) {
                Log.e(TAG, "创建对等连接或本地媒体流失败: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onCallFailed("创建连接失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 创建初始Offer用于通话请求
     */
    private void createInitialOffer() {
        MediaConstraints constraints = new MediaConstraints();
        // 盲人用户不需要接收视频
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.d(TAG, "创建初始Offer成功，用于通话请求");
                executor.execute(() -> {
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "设置本地SDP成功，准备发送通话请求");
                            // 发送通话请求，包含offer
                            String email = BeYourEyesApplication.getInstance().getUserEmail();
                            if (email == null) {
                                Log.e(TAG, "用户邮箱为空，无法发送通话请求");
                                if (listener != null) {
                                    listener.onCallFailed("用户邮箱为空，无法发送通话请求");
                                }
                                return;
                            }
                            
                            Log.d(TAG, "发送通话请求，包含初始Offer");
                            signalingClient.sendCallRequest(email, "需要帮助", sdp);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "设置本地SDP失败: " + s);
                            if (listener != null) {
                                listener.onCallFailed("设置本地SDP失败: " + s);
                            }
                        }
                    }, sdp);
                });
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "创建Offer失败: " + s);
                if (listener != null) {
                    listener.onCallFailed("创建通话请求失败: " + s);
                }
            }
        }, constraints);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "信令服务器断开连接");
        executor.execute(() -> {
            if (listener != null) {
                listener.onCallDisconnected();
            }
            disconnect();
        });
    }

    @Override
    public void onRemoteSdpReceived(SessionDescription sdp) {
        Log.d(TAG, "接收到远程SDP: " + sdp.type);
        if (sdp.type == SessionDescription.Type.ANSWER) {
            Log.d(TAG, "收到Answer，准备建立连接");
        } else if (sdp.type == SessionDescription.Type.OFFER) {
            Log.d(TAG, "收到Offer，准备创建Answer");
        }
        
        executor.execute(() -> {
            try {
                if (peerConnection == null) {
                    Log.e(TAG, "接收到远程SDP但PeerConnection不可用");
                    createPeerConnection();
                }

                // 确保PeerConnection已创建
                if (peerConnection == null) {
                    Log.e(TAG, "无法创建PeerConnection");
                    if (listener != null) {
                        listener.onCallFailed("无法创建连接");
                    }
                    return;
                }

                // 设置远程描述
                peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "设置远程SDP成功，类型: " + sdp.type);
                        // 如果收到的是Offer，无论是否为呼叫方都创建Answer
                        if (sdp.type == SessionDescription.Type.OFFER) {
                            Log.d(TAG, "开始创建Answer");
                            createAnswer();
                        } else if (sdp.type == SessionDescription.Type.ANSWER) {
                            Log.d(TAG, "成功设置远程Answer，等待ICE连接建立");
                        }
                    }

                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "设置远程SDP失败: " + s);
                        if (listener != null) {
                            listener.onCallFailed("设置远程SDP失败: " + s);
                        }
                    }
                }, sdp);

                // 如果对等连接已就绪，处理所有排队的ICE候选
                drainCandidates();

            } catch (Exception e) {
                Log.e(TAG, "处理远程SDP时出错", e);
                if (listener != null) {
                    listener.onCallFailed("处理远程SDP时出错: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidateReceived(IceCandidate candidate) {
        Log.d(TAG, "接收到远程ICE候选者: sdpMid=" + candidate.sdpMid + ", index=" + candidate.sdpMLineIndex);
        executor.execute(() -> {
            try {
                if (peerConnection != null && isPeerConnectionReady) {
                    Log.d(TAG, "直接添加ICE候选者到peer connection");
                    peerConnection.addIceCandidate(candidate);
                } else {
                    Log.d(TAG, "PeerConnection未就绪，将ICE候选者加入队列");
                    queuedRemoteCandidates.add(candidate);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理远程ICE候选者时出错", e);
                // 这里不需要通知失败，ICE候选失败通常不是致命错误
            }
        });
    }

    /**
     * 处理队列中的所有ICE候选
     */
    private void drainCandidates() {
        if (peerConnection != null && isPeerConnectionReady && !queuedRemoteCandidates.isEmpty()) {
            Log.d(TAG, "处理队列中的ICE候选者，数量: " + queuedRemoteCandidates.size());
            for (IceCandidate candidate : queuedRemoteCandidates) {
                try {
                    peerConnection.addIceCandidate(candidate);
                } catch (Exception e) {
                    Log.e(TAG, "添加队列中的ICE候选者失败", e);
                }
            }
            queuedRemoteCandidates.clear();
        }
    }

    /**
     * 简化的SDP观察者实现
     */
    private static class SimpleSdpObserver implements SdpObserver {
        private static final String TAG = "SimpleSdpObserver";

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

    /**
     * 获取本地SDP描述
     */
    public SessionDescription getLocalDescription() {
        if (peerConnection != null) {
            return peerConnection.getLocalDescription();
        }
        return null;
    }

    /**
     * 断开连接并释放资源
     */
    public void disconnect() {
        Log.d(TAG, "执行断开连接操作");
        // 创建一个线程用于优雅关闭executor
        final CountDownLatch disconnectLatch = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                Log.d(TAG, "开始释放WebRTC资源");
                if (videoCapturer != null) {
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "停止视频捕获失败: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                    videoCapturer = null;
                }

                if (peerConnection != null) {
                    peerConnection.close();
                    peerConnection = null;
                }

                if (videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }

                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }

                if (localVideoTrack != null) {
                    localVideoTrack.dispose();
                    localVideoTrack = null;
                }

                if (localAudioTrack != null) {
                    localAudioTrack.dispose();
                    localAudioTrack = null;
                }

                if (localRenderer != null) {
                    localRenderer.release();
                    localRenderer = null;
                }

                if (remoteRenderer != null) {
                    remoteRenderer.release();
                    remoteRenderer = null;
                }

                if (rootEglBase != null) {
                    rootEglBase.release();
                    rootEglBase = null;
                }

                signalingClient.disconnect();

                isPeerConnectionReady = false;
                queuedRemoteCandidates.clear();

                Log.d(TAG, "WebRTC资源释放完成");
            } finally {
                disconnectLatch.countDown();
            }
        });

        try {
            // 等待资源释放完成，设置合理的超时时间
            boolean completed = disconnectLatch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                Log.w(TAG, "断开连接操作超时");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "等待断开连接操作被中断", e);
            Thread.currentThread().interrupt();
        }

        // 关闭executor
        executor.shutdown();
        try {
            // 等待所有任务完成
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                Log.w(TAG, "执行器未能在规定时间内关闭，强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "等待执行器关闭被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Log.d(TAG, "断开连接操作完成");
    }

    @Override
    public void onCallAccepted(String roomId) {
        Log.d(TAG, "通话请求被接受，房间ID: " + roomId);
        executor.execute(() -> {
            try {
                this.roomId = roomId;
                
                // 如果是被呼叫方，等待接收Offer
                if (!isCaller) {
                    Log.d(TAG, "作为被呼叫方，等待接收Offer");
                    return;
                }
                
                // 如果是呼叫方，等待接收Answer
                Log.d(TAG, "作为呼叫方，等待接收Answer");
            } catch (Exception e) {
                Log.e(TAG, "处理通话接受失败: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onCallFailed("处理通话接受失败: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void onCallRejected(String reason) {

    }

    @Override
    public void onCallQueued(int position) {

    }

    @Override
    public void onCallTimeout() {

    }

    /**
     * 添加ICE候选者
     *
     * @param candidate ICE候选者
     */
    public void addIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "开始添加ICE候选者: sdpMid=" + candidate.sdpMid + ", index=" + candidate.sdpMLineIndex);
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection为空，无法添加ICE候选者");
            return;
        }
        
        executor.execute(() -> {
            try {
                peerConnection.addIceCandidate(candidate);
                Log.d(TAG, "成功添加ICE候选者");
            } catch (Exception e) {
                Log.e(TAG, "添加ICE候选者失败: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onCallFailed("添加ICE候选者失败: " + e.getMessage());
                }
            }
        });
    }
}
