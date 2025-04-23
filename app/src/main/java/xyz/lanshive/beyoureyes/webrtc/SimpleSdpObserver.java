package xyz.lanshive.beyoureyes.webrtc;

import android.util.Log;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SimpleSdpObserver implements SdpObserver {
    private static final String TAG = "SimpleSdpObserver";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(TAG, "SDP创建成功");
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
