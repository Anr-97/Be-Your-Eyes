package xyz.lanshive.beyoureyes.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.os.VibratorManager;
/**
 * 处理铃声播放的工具类
 */
public class RingtonePlayer {
    private static final String TAG = "RingtonePlayer";
    private static RingtonePlayer instance;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean isRinging = false;
    private final Context context; // 添加Context成员变量

    private void initVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31)及以上使用新的API
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            // 低版本Android继续使用旧API
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    // 在构造函数或初始化方法中调用
    public RingtonePlayer(Context context) {
        this.context = context.getApplicationContext();
        initVibrator(this.context);
    }

    public static synchronized RingtonePlayer getInstance(Context context) {
        if (instance == null) {
            instance = new RingtonePlayer(context.getApplicationContext());
        }
        return instance;
    }

    public void playIncomingCallRingtone() {
        try {
            if (isRinging) {
                Log.d(TAG, "铃声已经在播放中");
                return;
            }

            // 停止可能存在的上一个播放
            stopRingtone();

            // 获取默认来电铃声
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            // 创建并配置MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, ringtoneUri);  // 使用成员变量context
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 同时开启振动
            startVibration();

            isRinging = true;
            Log.d(TAG, "铃声和振动已开始");
        } catch (Exception e) {
            Log.e(TAG, "播放来电铃声失败", e);

            // 尝试使用备用方法
            try {
                Ringtone r = RingtoneManager.getRingtone(context,  // 使用成员变量context
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                r.play();
            } catch (Exception ex) {
                Log.e(TAG, "备用铃声播放方法也失败", ex);
            }
        }
    }

private void startVibration() {
    if (vibrator != null && vibrator.hasVibrator()) {
        // 使用新的VibrationEffect API
        long[] pattern = {0, 500, 500};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 创建振动模式：振动500ms，暂停500ms
            android.os.VibrationEffect effect = android.os.VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(effect);
        } else {
            // 兼容旧版本
            vibrator.vibrate(pattern, 0); // Warning suppressed
        }
    }
}

    public void stopRingtone() {
        isRinging = false;

        // 停止MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "停止铃声失败", e);
            }
        }

        // 停止振动
        if (vibrator != null) {
            vibrator.cancel();
        }

        Log.d(TAG, "铃声和振动已停止");
    }
}