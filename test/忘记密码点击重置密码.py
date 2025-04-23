import requests

# 定义接口的 URL
BASE_URL = "http://47.109.150.64:3000/api/auth"
RESET_URL = f"{BASE_URL}/resetPassword"

# 定义测试数据
data = {
    "email": "lanshive@qq.com",  # 测试用的邮箱地址
    "code": "935831",  # 假设这是正确的验证码
    "newPassword": "123456789"
}


def test_reset():
    print("测试重置密码接口...")
    try:
        print("用户数据:", data)
        response = requests.post(RESET_URL, json=data)
        if response.status_code == 401:
            print("验证码错误或已过期！")
        if response.status_code == 404:
            print("用户不存在！")
        if response.status_code == 200:
            print("密码重置成功！")
        else:
            print(f"请求失败，状态码: {response.status_code}")
            print("响应内容:", response.text)
    except requests.exceptions.RequestException as e:
        print("注册请求失败，错误信息:", e)


if __name__ == "__main__":
    test_reset()
