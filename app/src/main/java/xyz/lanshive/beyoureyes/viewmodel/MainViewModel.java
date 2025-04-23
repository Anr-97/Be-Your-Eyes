package xyz.lanshive.beyoureyes.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isOnline = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> totalHelp = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> monthlyHelp = new MutableLiveData<>(0);
    private final MutableLiveData<String> totalHours = new MutableLiveData<>("0h");

    public MutableLiveData<Boolean> getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(boolean online) {
        isOnline.setValue(online);
    }

    public MutableLiveData<Integer> getTotalHelp() {
        return totalHelp;
    }

    public void setTotalHelp(int total) {
        totalHelp.setValue(total);
    }

    public MutableLiveData<Integer> getMonthlyHelp() {
        return monthlyHelp;
    }

    public void setMonthlyHelp(int monthly) {
        monthlyHelp.setValue(monthly);
    }

    public MutableLiveData<String> getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(String hours) {
        totalHours.setValue(hours);
    }

    // 从服务器加载统计数据
    public void loadStatistics() {
        // TODO: 实现从服务器获取统计数据的逻辑
    }

    // 更新在线状态
    public void updateOnlineStatus(boolean online) {
        setIsOnline(online);
        // TODO: 实现向服务器更新在线状态的逻辑
    }
} 