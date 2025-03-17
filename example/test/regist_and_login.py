import requests

# 定义接口的 URL
BASE_URL = "http://47.109.150.64:3000/api/auth"
REGISTER_URL = f"{BASE_URL}/register"
LOGIN_URL = f"{BASE_URL}/login"
STATISTICS_URL = f"{BASE_URL}/statistics"  # 新增统计端点URL

# 测试用的邮箱和密码
email = "lanshive@qq.com"
password = "123456"

# 定义测试数据
register_data = {
    "email": email,  # 测试用的邮箱地址
    "password": password,
    "userType": "VOLUNTEER",
    "code": "422092"  # 假设这是正确的验证码
}

login_data = {
    "email": email,
    "password": password
}


# 通用的响应检查函数
def check_response(response, success_codes=[200, 201]):
    if response.status_code in success_codes:
        print("请求成功！")
        print("响应内容:", response.json())
        return True
    else:
        print(f"请求失败，状态码: {response.status_code}")
        print("响应内容:", response.text)
        return False


# 测试注册接口
def test_register():
    print("测试注册接口...")
    try:
        print("注册数据:", register_data)
        response = requests.post(REGISTER_URL, json=register_data)
        if check_response(response):
            print("注册成功！")
        else:
            print("注册失败，请检查注册数据或服务器日志。")
    except requests.exceptions.RequestException as e:
        print("注册请求失败，错误信息:", e)


# 测试登录接口
def test_login():
    print("\n测试登录接口...")
    try:
        print("登录数据:", login_data)
        response = requests.post(LOGIN_URL, json=login_data)
        if check_response(response):
            print("登录成功！")
        else:
            print("登录失败，请检查登录数据或服务器日志。")
    except requests.exceptions.RequestException as e:
        print("登录请求失败，错误信息:", e)


# 测试统计接口
def test_statistics():
    print("\n测试统计接口...")
    try:
        response = requests.get(STATISTICS_URL)  # 发送GET请求
        if check_response(response):
            data = response.json()
            # 检查返回的统计数据是否符合预期
            if "blindUsers" in data and "volunteers" in data:
                print("统计数据格式正确！")
                print(f"HELPER 用户数量: {data['blindUsers']}")
                print(f"VOLUNTEER 用户数量: {data['volunteers']}")
            else:
                print("统计数据格式不正确，缺少字段！")
        else:
            print("统计接口请求失败，请检查服务器日志。")
    except requests.exceptions.RequestException as e:
        print("统计请求失败，错误信息:", e)


# 主函数
if __name__ == "__main__":
    # 测试注册接口
    test_register()

    # 测试登录接口
    test_login()

    # 测试统计接口
    test_statistics()
