package xyz.lanshive.beyoureyes.socket;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public interface SignalingListener {
    void onConnected();
    void onDisconnected();
    void onRemoteSdpReceived(SessionDescription sdp);
    void onRemoteIceCandidateReceived(IceCandidate candidate);
    void onCallAccepted(String roomId);

    // 添加的新方法
    void onCallRejected(String reason);
    void onCallQueued(int position);
    void onCallTimeout();
}