import requests
import json


def send_code(email):
    """
    向指定的接口发送 POST 请求，验证接口是否可用。
    """
    url = "http://47.109.150.64:3000/api/auth/send-code"  # 接口地址
    headers = {
        "Content-Type": "application/json"
    }
    data = {
        "email": email
    }

    try:
        # 发送 POST 请求
        response = requests.post(url, headers=headers, data=json.dumps(data))

        # 检查响应状态码
        if response.status_code == 200:
            print("接口可用，返回状态码：200")
            print("响应内容：", response.json())  # 打印响应的 JSON 数据
        else:
            print(f"接口返回错误状态码：{response.status_code}")
            print("响应内容：", response.text)
    except requests.exceptions.RequestException as e:
        print(f"请求失败：{e}")


if __name__ == "__main__":
    email = "lanshive@qq.com"  # 测试用的邮箱地址
    send_code(email)