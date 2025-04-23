package xyz.lanshive.beyoureyes.model;

import android.text.format.DateUtils;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HelpRecord {
    @SerializedName("recipientEmail")
    private String userEmail;
    
    @SerializedName("helpTime")
    private String helpTime;
    
    @SerializedName("duration")
    private int duration; // 时长（分钟）
    
    @SerializedName("status")
    private String status; // 状态：completed/cancelled

    public HelpRecord(String userEmail, String helpTime, int duration, String status) {
        this.userEmail = userEmail;
        this.helpTime = helpTime;
        this.duration = duration;
        this.status = status;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    // 从邮箱获取用户名
    public String getUserName() {
        if (userEmail == null || userEmail.isEmpty()) {
            return "未知用户";
        }
        int atIndex = userEmail.indexOf('@');
        return atIndex > 0 ? userEmail.substring(0, atIndex) : userEmail;
    }

    public String getHelpTime() {
        return helpTime;
    }

    // 格式化显示时间
    public String getFormattedTime() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(helpTime);
            if (date == null) return helpTime;

            Calendar now = Calendar.getInstance();
            Calendar helpDate = Calendar.getInstance();
            helpDate.setTime(date);
            helpDate.add(Calendar.HOUR_OF_DAY, 8); // UTC+8时区

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = timeFormat.format(helpDate.getTime());

            if (now.get(Calendar.YEAR) == helpDate.get(Calendar.YEAR)) {
                if (now.get(Calendar.DAY_OF_YEAR) == helpDate.get(Calendar.DAY_OF_YEAR)) {
                    return "今天 " + timeStr;
                } else if (now.get(Calendar.DAY_OF_YEAR) - helpDate.get(Calendar.DAY_OF_YEAR) == 1) {
                    return "昨天 " + timeStr;
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日", Locale.getDefault());
                    return dateFormat.format(helpDate.getTime()) + " " + timeStr;
                }
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
                return dateFormat.format(helpDate.getTime()) + " " + timeStr;
            }
        } catch (ParseException e) {
            return helpTime;
        }
    }

    public void setHelpTime(String helpTime) {
        this.helpTime = helpTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    // 获取格式化的状态显示
    public String getFormattedStatus() {
        if ("completed".equals(status)) {
            return "已完成";
        } else if ("cancelled".equals(status)) {
            return "未接通";
        } else {
            return status;
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }
}