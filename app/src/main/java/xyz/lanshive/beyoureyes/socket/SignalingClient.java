package xyz.lanshive.beyoureyes.socket;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;
import xyz.lanshive.beyoureyes.webrtc.IncomingCallActivity;
import xyz.lanshive.beyoureyes.utils.RingtonePlayer;

public class SignalingClient {
    private static final String TAG = "SignalingClient";
    private static final String SERVER_URL = "https://www.lanshive.xyz:8080";
    private static final String EVENT_JOIN = "join";
    private static final String EVENT_LEAVE = "leave";
    private static final String EVENT_MESSAGE = "message";
    private static final String EVENT_ICE_CANDIDATE = "ice-candidate";
    private static final String EVENT_OFFER = "offer";
    private static final String EVENT_ANSWER = "answer";
    private static final String EVENT_USER_ONLINE = "user_online";
    private static final String EVENT_CALL_REQUEST = "call_request";
    private static final String EVENT_CALL_ACCEPTED = "call_accepted";
    private static final String EVENT_CALL_REJECTED = "call_rejected";
    private static final String EVENT_CALL_QUEUED = "call_queued";
    private static final String EVENT_CALL_TIMEOUT = "call_timeout";
    private static final String EVENT_INCOMING_CALL = "incoming_call";

    private Socket socket;
    private SignalingListener listener;
    private String roomId;
    private String token;

    public SignalingClient(SignalingListener listener) {
        this.listener = listener;
    }

    public void connect(String roomId, String token) {
        if (socket != null && socket.connected()) {
            Log.w(TAG, "信令客户端已经连接，跳过重复连接");
            return;
        }

        this.roomId = roomId;
        this.token = token;

        try {
            IO.Options options = new IO.Options();
            options.query = "roomId=" + roomId + "&token=" + token;
            options.reconnection = true;
            options.forceNew = true;
            socket = IO.socket(new URI(SERVER_URL), options);

            socket.off();

            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on(EVENT_OFFER, onOffer);
            socket.on(EVENT_ANSWER, onAnswer);
            socket.on(EVENT_ICE_CANDIDATE, onIceCandidate);
            socket.on(EVENT_CALL_ACCEPTED, onCallAccepted);
            socket.on(EVENT_CALL_REJECTED, onCallRejected);
            socket.on(EVENT_CALL_QUEUED, onCallQueued);
            socket.on(EVENT_CALL_TIMEOUT, onCallTimeout);
            socket.on(EVENT_INCOMING_CALL, onIncomingCall);

            socket.connect();
            Log.d(TAG, "开始连接信令服务器");
        } catch (URISyntaxException e) {
            Log.e(TAG, "连接信令服务器失败", e);
        }
    }

    private Emitter.Listener onConnect = args -> {
        Log.d(TAG, "已连接到信令服务器");
        // 发送用户上线事件
        try {
            String email = BeYourEyesApplication.getInstance().getUserEmail();
            String userType = BeYourEyesApplication.getInstance().getCurrentUserRole()
                    .equals(BeYourEyesApplication.ROLE_BLIND) ? "blind" : "volunteer";

            // 检查是否应该发送在线状态
            if (userType.equals("volunteer") &&
                BeYourEyesApplication.getInstance().isVolunteerOnline()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("userId", email);
                jsonObject.put("userType", userType);

                Log.d(TAG, "发送志愿者上线事件: userId=" + email);
                socket.emit(EVENT_USER_ONLINE, jsonObject);
            }

            // 如果是盲人用户，立即发送通话请求
            if (userType.equals("blind")) {
                Log.d(TAG, "盲人用户立即发送通话请求");
                JSONObject callRequestData = new JSONObject();
                callRequestData.put("userId", email);
                callRequestData.put("requirements", "需要帮助");
                callRequestData.put("offer", new JSONObject());

                Log.d(TAG, "发送通话请求: userId=" + email + ", requirements=需要帮助");
                socket.emit(EVENT_CALL_REQUEST, callRequestData);
                Log.d(TAG, "通话请求已发送");
            }
        } catch (JSONException e) {
            Log.e(TAG, "发送用户上线事件或通话请求失败", e);
        }

        if (listener != null) {
            listener.onConnected();
        }
    };

    private Emitter.Listener onDisconnect = args -> {
        Log.d(TAG, "已断开与信令服务器的连接");
        if (listener != null) {
            listener.onDisconnected();
        }
    };

    private Emitter.Listener onConnectError = args -> {
        Log.e(TAG, "连接信令服务器失败: " + (args.length > 0 ? args[0] : "未知错误"));
    };

private Emitter.Listener onIncomingCall = args -> {
    Log.d(TAG, "收到通话请求，开始处理");
    try {
        if (args.length == 0 || args[0] == null) {
            Log.e(TAG, "收到通话请求但数据为空");
            return;
        }

        JSONObject data = (JSONObject) args[0];
        Log.d(TAG, "通话请求数据: " + data.toString());

        // 获取来电信息
        String callerId = data.getString("userId");
        String requirements = data.getString("requirements");

        // 创建启动通话界面的Intent
        Context context = BeYourEyesApplication.getInstance();
        Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("caller_id", callerId);
        intent.putExtra("requirements", requirements);

        // 显示通话界面
        Log.d(TAG, "准备显示通话界面");
        context.startActivity(intent);
        Log.d(TAG, "通话界面显示完成");

        // 播放铃声
        Log.d(TAG, "准备播放来电铃声");
        // 使用新创建的RingtonePlayer类
        RingtonePlayer.getInstance(context).playIncomingCallRingtone();
        Log.d(TAG, "铃声播放完成");
    } catch (Exception e) {
        Log.e(TAG, "处理通话请求时出错", e);
    }
};

    private Emitter.Listener onCallAccepted = args -> {
        Log.d(TAG, "通话请求已被接受");
        try {
            if (args.length == 0 || args[0] == null) {
                Log.e(TAG, "收到通话接受事件但数据为空");
                return;
            }

            JSONObject data = (JSONObject) args[0];
            String requestId = data.getString("requestId");
            String roomId = data.getString("roomId");

            Log.d(TAG, "通话请求已被接受，房间ID: " + roomId);

            // 更新当前房间ID
            this.roomId = roomId;

            // 通知监听器通话已被接受，可以创建Offer
            if (listener != null) {
                listener.onCallAccepted(roomId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析call_accepted事件失败", e);
        }
    };

    private Emitter.Listener onCallRejected = args -> {
        Log.d(TAG, "通话请求已被拒绝");
        try {
            if (args.length > 0 && args[0] != null) {
                JSONObject data = (JSONObject) args[0];
                String reason = data.optString("reason", "未知原因");
                Log.d(TAG, "拒绝原因: " + reason);

                if (listener != null) {
                    listener.onCallRejected(reason);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析call_rejected事件失败", e);
        }
    };

    private Emitter.Listener onCallQueued = args -> {
        Log.d(TAG, "通话请求已加入队列");
        try {
            if (args.length > 0 && args[0] != null) {
                JSONObject data = (JSONObject) args[0];
                int position = data.optInt("position", -1);
                Log.d(TAG, "队列位置: " + position);

                if (listener != null && position >= 0) {
                    listener.onCallQueued(position);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析call_queued事件失败", e);
        }
    };

    private Emitter.Listener onCallTimeout = args -> {
        Log.d(TAG, "通话请求已超时");
        if (listener != null) {
            listener.onCallTimeout();
        }
    };

    private Emitter.Listener onOffer = args -> {
        try {
            if (args.length == 0 || args[0] == null) {
                Log.e(TAG, "收到offer但数据为空");
                return;
            }

            JSONObject data = (JSONObject) args[0];
            String sdp = data.getString("sdp");
            SessionDescription offer = new SessionDescription(
                    SessionDescription.Type.OFFER, sdp);

            Log.d(TAG, "收到Offer SDP: " + sdp.substring(0, Math.min(50, sdp.length())) + "...");

            if (listener != null) {
                listener.onRemoteSdpReceived(offer);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析offer失败", e);
        }
    };

    private Emitter.Listener onAnswer = args -> {
        try {
            if (args.length == 0 || args[0] == null) {
                Log.e(TAG, "收到answer但数据为空");
                return;
            }

            Log.d(TAG, "收到Answer消息");
            JSONObject data = (JSONObject) args[0];
            String sdp = data.getString("sdp");
            Log.d(TAG, "解析Answer SDP: " + sdp.substring(0, Math.min(50, sdp.length())) + "...");
            SessionDescription answer = new SessionDescription(
                    SessionDescription.Type.ANSWER, sdp);
            Log.d(TAG, "Answer解析成功，转发给SignalingListener");

            if (listener != null) {
                listener.onRemoteSdpReceived(answer);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析answer失败", e);
        }
    };

    private Emitter.Listener onIceCandidate = args -> {
        try {
            if (args.length == 0 || args[0] == null) {
                Log.e(TAG, "收到ice candidate但数据为空");
                return;
            }

            JSONObject data = (JSONObject) args[0];
            String candidate = data.getString("candidate");
            String sdpMid = data.getString("sdpMid");
            int sdpMLineIndex = data.getInt("sdpMLineIndex");
            IceCandidate iceCandidate = new IceCandidate(
                    sdpMid, sdpMLineIndex, candidate);

            Log.d(TAG, "收到ICE候选: " + candidate.substring(0, Math.min(50, candidate.length())) + "...");

            if (listener != null) {
                listener.onRemoteIceCandidateReceived(iceCandidate);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析ice candidate失败", e);
        }
    };

    public void sendSdp(SessionDescription sdp) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "无法发送SDP，socket未连接");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("roomId", roomId);

            JSONObject sdpJson = new JSONObject();
            sdpJson.put("type", sdp.type.canonicalForm());
            sdpJson.put("sdp", sdp.description);

            // 根据SDP类型选择不同的事件
            String eventName;
            if (sdp.type == SessionDescription.Type.OFFER) {
                eventName = EVENT_OFFER;
                data.put("offer", sdpJson);
            } else if (sdp.type == SessionDescription.Type.ANSWER) {
                eventName = EVENT_ANSWER;
                data.put("answer", sdpJson);
            } else {
                Log.e(TAG, "不支持的SDP类型: " + sdp.type);
                return;
            }

            // 记录发送的数据
            Log.d(TAG, "发送SDP类型: " + sdp.type.canonicalForm());
            Log.d(TAG, "发送SDP内容前50个字符: " + sdp.description.substring(0, Math.min(50, sdp.description.length())));

            socket.emit(eventName, data);
            Log.d(TAG, sdp.type.canonicalForm() + "已发送");
        } catch (JSONException e) {
            Log.e(TAG, "发送SDP失败", e);
        }
    }

    public void sendIceCandidate(IceCandidate candidate) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "无法发送ICE候选，socket未连接");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("roomId", roomId);
            data.put("candidate", candidate.sdp);
            data.put("sdpMid", candidate.sdpMid);
            data.put("sdpMLineIndex", candidate.sdpMLineIndex);

            Log.d(TAG, "发送ICE候选: " + candidate.sdp.substring(0, Math.min(50, candidate.sdp.length())) + "...");

            socket.emit(EVENT_ICE_CANDIDATE, data);
        } catch (JSONException e) {
            Log.e(TAG, "发送ICE candidate失败", e);
        }
    }

    public void sendCallRequest(String userId, String requirements, SessionDescription offer) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "无法发送通话请求，socket未连接");
            return;
        }

        Log.d(TAG, "发送通话请求: userId=" + userId + ", requirements=" + requirements);
        try {
            JSONObject data = new JSONObject();
            data.put("userId", userId);
            data.put("requirements", requirements);

            if (offer != null) {
                JSONObject offerJson = new JSONObject();
                offerJson.put("type", offer.type.canonicalForm());
                offerJson.put("sdp", offer.description);
                data.put("offer", offerJson);
            } else {
                data.put("offer", new JSONObject());
            }

            socket.emit(EVENT_CALL_REQUEST, data);
            Log.d(TAG, "通话请求已发送");
        } catch (JSONException e) {
            Log.e(TAG, "发送通话请求失败", e);
        }
    }

    // 重载方法，兼容不带offer的调用
    public void sendCallRequest(String userId, String requirements) {
        sendCallRequest(userId, requirements, null);
    }

    public void sendCallResponse(String callerId, boolean accept) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "无法发送通话响应，socket未连接");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("callerId", callerId);
            data.put("accept", accept);

            String eventName = accept ? "accept_call" : "reject_call";
            socket.emit(eventName, data);
            Log.d(TAG, "通话响应已发送: " + (accept ? "接受" : "拒绝"));
        } catch (JSONException e) {
            Log.e(TAG, "发送通话响应失败", e);
        }
    }

    public void disconnect() {
        if (socket != null) {
            Log.d(TAG, "断开信令服务器连接");
            socket.disconnect();
            socket = null;
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public void sendAnswer(SessionDescription sdp) {
    }

    /**
     * 接受通话请求
     *
     * @param roomId 房间ID
     * @param answer 本地Answer，可以为null
     */
    public void acceptCall(String roomId, SessionDescription answer) {
        Log.d(TAG, "接受通话请求，房间ID: " + roomId);
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "Socket未连接，无法接受通话");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("roomId", roomId);
            if (answer != null) {
                data.put("sdp", answer.description);
                data.put("type", answer.type.canonicalForm());
            }

            socket.emit("call_accepted", data);
            Log.d(TAG, "已发送通话接受消息");
        } catch (JSONException e) {
            Log.e(TAG, "构建通话接受消息失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝通话请求
     *
     * @param roomId 房间ID
     * @param reason 拒绝原因
     */
    public void rejectCall(String roomId, String reason) {
        Log.d(TAG, "拒绝通话请求，房间ID: " + roomId + ", 原因: " + reason);
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "Socket未连接，无法拒绝通话");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("roomId", roomId);
            data.put("reason", reason);

            socket.emit("call_rejected", data);
            Log.d(TAG, "已发送通话拒绝消息");
        } catch (JSONException e) {
            Log.e(TAG, "构建通话拒绝消息失败: " + e.getMessage());
        }
    }
}