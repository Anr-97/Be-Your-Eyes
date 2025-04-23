import requests

# 定义接口的 URL
BASE_URL = "http://47.109.150.64:3000/api/auth"
REGISTER_URL = f"{BASE_URL}/forgotPass"


# 定义测试数据
register_data = {
    "email": "lanshive@qq.com",  # 测试用的邮箱地址
}


# 测试注册验证码发送接口
def test_mail():
    print("测试忘记密码邮件发送接口...")
    try:
        print("注册数据:", register_data)
        response = requests.post(REGISTER_URL, json=register_data)
        if response.status_code == 404:
            print("该邮箱还未注册！")
        elif response.status_code == 200:
            print("重置验证码已发送，请查收邮件")
        else:
            print(f"请求失败，状态码: {response.status_code}")
            print("响应内容:", response.text)
    except requests.exceptions.RequestException as e:
        print("注册请求失败，错误信息:", e)


if __name__ == "__main__":
    test_mail()