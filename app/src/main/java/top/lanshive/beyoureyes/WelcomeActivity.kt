package top.lanshive.beyoureyes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import top.lanshive.beyoureyes.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 隐藏ActionBar
        supportActionBar?.hide()

        // 设置状态栏透明
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // 设置按钮点击事件
        binding.needHelpButton.setOnClickListener {
            // TODO: 跳转到需要帮助的注册/登录界面
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.volunteerButton.setOnClickListener {
            // TODO: 跳转到志愿者注册/登录界面
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
} 