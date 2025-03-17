package top.lanshive.beyoureyes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class PrivacyActivity extends AppCompatActivity {
    private static final String TAG = "PrivacyActivity";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        // 初始化 WebView
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        // 加载隐私政策内容
String privacyContent = "<!DOCTYPE html><html><head>" +
        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
        "<style>" +
        "body { padding: 20px; line-height: 1.6; font-size: 16px; font-family: sans-serif; }" +
        "h2 { color: #005cf7; margin-bottom: 20px; }" +
        "h3 { color: #333; margin-top: 20px; margin-bottom: 10px; }" +
        "p { color: #555; margin-bottom: 15px; }" +
        "ul { margin-bottom: 15px; padding-left: 20px; }" +
        "li { margin-bottom: 8px; color: #555; }" +
        "</style>" +
        "</head><body>" +

        // 标题
        "<p>欢迎使用 <strong>Be Your Eyes</strong>！我们深知隐私对您的重要性，并致力于保护您的个人信息安全。本隐私政策旨在向您说明我们如何收集、使用、存储和保护您的个人信息，以及您对个人信息享有的权利。</p>" +

        // 信息收集
        "<h3>1. 信息收集</h3>" +
        "<p>为了向您提供更好的服务，我们可能会收集以下信息：</p>" +
        "<ul>" +
        "<li><strong>账户信息</strong>：如姓名、电子邮件地址、电话号码等。</li>" +
        "<li><strong>位置信息</strong>：当您使用基于位置的服务时，我们会收集您的地理位置信息，以便为您匹配附近的志愿者。</li>" +
        "<li><strong>设备信息</strong>：包括设备型号、操作系统版本、唯一设备标识符等，用于优化服务兼容性。</li>" +
        "<li><strong>使用数据</strong>：如服务访问时间、使用频率、功能偏好等，帮助我们改进产品体验。</li>" +
        "</ul>" +

        // 信息使用
        "<h3>2. 信息使用</h3>" +
        "<p>我们收集的信息将用于以下用途：</p>" +
        "<ul>" +
        "<li><strong>提供服务</strong>：为您匹配志愿者、提供视力协助服务等。</li>" +
        "<li><strong>改进服务</strong>：分析用户行为，优化产品功能和用户体验。</li>" +
        "<li><strong>安全保障</strong>：验证身份、检测异常活动，保护您的账户安全。</li>" +
        "<li><strong>沟通与通知</strong>：向您发送服务更新、重要通知或用户支持信息。</li>" +
        "</ul>" +

        // 信息保护
        "<h3>3. 信息保护</h3>" +
        "<p>我们采取严格的技术和管理措施保护您的个人信息安全：</p>" +
        "<ul>" +
        "<li><strong>数据加密</strong>：对传输和存储的数据进行加密，防止未经授权的访问。</li>" +
        "<li><strong>访问控制</strong>：限制员工和合作伙伴对个人信息的访问权限。</li>" +
        "<li><strong>安全审计</strong>：定期审查和更新安全措施，确保符合行业标准。</li>" +
        "</ul>" +

        // 信息共享
        "<h3>4. 信息共享</h3>" +
        "<p>我们承诺不会将您的个人信息出售给第三方。仅在以下情况下，我们可能会共享您的信息：</p>" +
        "<ul>" +
        "<li><strong>获得您的同意</strong>：在您明确同意的情况下，与第三方共享信息。</li>" +
        "<li><strong>法律要求</strong>：根据法律法规或政府部门的合法要求。</li>" +
        "<li><strong>服务必要</strong>：与志愿者或合作伙伴共享必要信息，以完成服务请求。</li>" +
        "</ul>" +

        // 用户权利
        "<h3>5. 您的权利</h3>" +
        "<p>您对您的个人信息享有以下权利：</p>" +
        "<ul>" +
        "<li><strong>访问权</strong>：随时查看我们持有的您的个人信息。</li>" +
        "<li><strong>更正权</strong>：要求更正不准确或不完整的个人信息。</li>" +
        "<li><strong>删除权</strong>：要求删除您的个人信息（法律法规另有规定的除外）。</li>" +
        "<li><strong>撤回同意</strong>：随时撤回对个人信息处理的同意。</li>" +
        "</ul>" +

        // 联系我们
        "<h3>6. 联系我们</h3>" +
        "<p>如果您对本隐私政策有任何疑问，或需要行使您的权利，请通过以下方式联系我们：</p>" +
        "<p><strong>邮箱：BeYourEyes@qq.com</strong></p>" +

        "</body></html>";
        
        webView.loadDataWithBaseURL(null, privacyContent, "text/html", "UTF-8", null);

        // 设置同意按钮点击事件
        Button agreeButton = findViewById(R.id.agreeButton);
        agreeButton.setOnClickListener(v -> {
            // 保存用户已同意隐私政策的状态
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("privacy_accepted", true).apply();
            
            // 跳转到注册页面
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
