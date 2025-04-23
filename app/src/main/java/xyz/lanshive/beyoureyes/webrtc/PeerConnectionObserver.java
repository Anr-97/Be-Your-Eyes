package xyz.lanshive.beyoureyes.webrtc;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public interface PeerConnectionObserver {
    default void onSignalingChange(PeerConnection.SignalingState signalingState) {}
    default void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}
    default void onIceConnectionReceivingChange(boolean b) {}
    default void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}
    default void onIceCandidate(IceCandidate iceCandidate) {}
    default void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
    default void onAddStream(MediaStream mediaStream) {}
    default void onRemoveStream(MediaStream mediaStream) {}
    default void onDataChannel(DataChannel dataChannel) {}
    default void onRenegotiationNeeded() {}
    default void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
    default void onConnectionChange(PeerConnection.PeerConnectionState newState) {}
    default void onSelectedCandidatePairChanged(org.webrtc.CandidatePairChangeEvent event) {}
}