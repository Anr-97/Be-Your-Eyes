package top.lanshive.beyoureyes.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import top.lanshive.beyoureyes.BeYourEyesApplication;
import top.lanshive.beyoureyes.R;

public class HomeFragment extends Fragment {
    private TextView textRole;
    private TextView textWelcome;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        textRole = root.findViewById(R.id.text_role);
        textWelcome = root.findViewById(R.id.text_welcome);

        // 使用辅助方法判断用户身份
        if (BeYourEyesApplication.getInstance().isBlindUser()) {
            textRole.setText("视障用户");
            textWelcome.setText("欢迎使用 Be Your Eyes，随时获取帮助");
        } else {
            textRole.setText("志愿者");
            textWelcome.setText("欢迎使用 Be Your Eyes，感谢您的爱心付出");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        textRole = null;
        textWelcome = null;
    }
} 