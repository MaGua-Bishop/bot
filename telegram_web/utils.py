import json
import requests
from lxml import etree
from telethon.sync import TelegramClient
from telethon.tl.functions.account import UpdateProfileRequest, UpdateUsernameRequest, CheckUsernameRequest
from telethon.tl.functions.photos import UploadProfilePhotoRequest
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from django.conf import settings
from telethon.errors import UsernameOccupiedError, UsernameInvalidError, FloodWaitError
import socks
from urllib.parse import urlparse
import time


def save_image(url: str):
    """保存图片"""
    result = requests.get(url).content
    return result


def get_telegram_user_data(username: str):
    """获取用户的信息"""
    print(f"开始获取用户信息：{username}")
    status = True
    url = f"https://t.me/{username}"
    proxy = settings.PROXY
    proxies = {'http': proxy, 'https': proxy}
    result = requests.get(url, proxies=proxies).text
    html = etree.HTML(result)
    try:
        name = html.xpath('//div[@class="tgme_page_title"]//text()')[0]
    except:
        name = ""
        status = False

    about = " ".join(html.xpath('//div[@class="tgme_page_description "]//text()'))
    img_url = "".join(html.xpath('//img[@class="tgme_page_photo_image"]/@src'))
    image = None
    image_name = None
    if img_url != "" and not img_url.startswith("data:image"):
        image = save_image(img_url)
        image_name = img_url.split('/')[-1][:20] + ".jpg"
    return status, name, about, image, image_name


async def send_mail_to_admin_async(title, content):
    print(title, content)
    """发送信息给系统管理员"""
    sender_email = settings.EMAIL_USER
    sender_password = settings.EMAIL_PASSWORD
    admin_email = settings.ADMIN_EMAIL
    # admin_email = "y17373081487@gmail.com"
    # 创建邮件内容
    msg = MIMEMultipart()
    msg['From'] = sender_email
    msg['To'] = admin_email
    msg['Subject'] = title

    # 添加邮件正文
    msg.attach(MIMEText(content, 'html'))

    try:
        # 连接到 Gmail SMTP 服务器并发送邮件
        with smtplib.SMTP('smtp.gmail.com', 587) as server:
            server.starttls()  # 启用 TLS
            server.login(sender_email, sender_password)  # 登录
            server.send_message(msg)  # 发送邮件
            print("邮件发送成功")
    except Exception as e:
        print(f"邮件发送失败: {e}")


def send_mail_to_admin(title, content):
    print(title, content)
    """发送信息给系统管理员"""
    sender_email = settings.EMAIL_USER
    sender_password = settings.EMAIL_PASSWORD
    admin_email = settings.ADMIN_EMAIL
    # admin_email = "y17373081487@gmail.com"
    # 创建邮件内容
    msg = MIMEMultipart()
    msg['From'] = sender_email
    msg['To'] = admin_email
    msg['Subject'] = title

    # 添加邮件正文
    msg.attach(MIMEText(content, 'html'))

    try:
        # 连接到 Gmail SMTP 服务器并发送邮件
        with smtplib.SMTP('smtp.gmail.com', 587) as server:
            server.starttls()  # 启用 TLS
            server.login(sender_email, sender_password)  # 登录
            server.send_message(msg)  # 发送邮件
            print("邮件发送成功")
    except Exception as e:
        print(f"邮件发送失败: {e}")


def get_user_info(user):
    """获取用户的信息"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    with open(json_file, "r") as fp:
        data = json.loads(fp.read())
    app_id = data['app_id']
    app_hash = data['app_id']
    client = TelegramClient(session, app_id, app_hash, proxy=get_telethon_proxy())
    # 使用同步 API
    client.start()
    # 获取自己的信息
    me = client.get_me()
    print(me)
    phone = me.phone
    username = me.username
    name = me.first_name + " " + me.last_name
    print(phone, username, name)
    client.disconnect()


def get_telethon_proxy():
    # 隧道代理 URL
    PROXY_URL = settings.PROXY_URL

    # 解析代理 URL
    parsed_url = urlparse(PROXY_URL)
    proxy_type = socks.HTTP if parsed_url.scheme == 'http' else socks.HTTP
    proxy_host = parsed_url.hostname
    proxy_port = parsed_url.port
    proxy_username = parsed_url.username
    proxy_password = parsed_url.password
    # 配置代理
    proxy = (proxy_type, proxy_host, proxy_port, True, proxy_username, proxy_password)
    return proxy


def is_username_available(client, username):
    """检查用户名是否可用"""
    try:
        result = client(CheckUsernameRequest(username=username))
        return result
    except Exception as e:
        print(f"检查用户名 {username} 时发生错误: {str(e)}")
        return False


async def copy_user_info(user, username, img_file, about, name, msg="", last_name="(主)"):
    """模仿用户"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    img_file = f"media/{img_file}"

    # 读取json文件
    if user.fileJson:
        try:
            with open(json_file, "r") as fp:
                data = json.loads(fp.read())
                app_id = data['app_id']
                app_hash = data['app_hash']
                phone_number = data.get('phone')
        except Exception as e:
            raise ValueError(f"Error reading JSON file: {e}")
    else:
        # 使用settings中的
        app_id = settings.API_ID
        app_hash = settings.API_HASH
        phone_number = user.phone
    # proxy = (socks.SOCKS5, '127.0.0.1', 7890, True)
    client = TelegramClient(session, app_id, app_hash, proxy=get_telethon_proxy())
    # 使用同步 API
    await client.start()  # 确保使用 await 启动客户端

    original_username = username
    last_word = original_username[-1]

    while not await is_username_available(client, username):  # 确保使用 await
        username += last_word

    try:
        await client(UpdateUsernameRequest(username=username))  # 确保使用 await
        user.username = username
        user.save()
        print(f"用户名已更新为: {username}")
    except Exception as e:
        print(f"用户名:{username}发生错误: {e}")
        if "The username is not different from the current username" in str(e):
            print(f"更新用户资料时发生错误: {e}")

    try:
        # 更新其他用户资料
        await client(UpdateProfileRequest(  # 确保使用 await
            first_name=name,
            last_name=last_name,  # 如果需要可以设置
            about=about  # 简介
        ))
    except Exception as e:
        # 根据异常内容判断是否是字数超出的问题
        if "first_name" in str(e) and "too long" in str(e):
            await send_mail_to_admin_async(
                '用户模仿失败',
                f'{user.username} 的账号模仿失败: 名称超出字数限制。'
            )
            return f"{user.username} 的账号模仿失败: 名称超出字数限制。"
        elif "The provided bio is too long (caused by UpdateProfileRequest)" in str(e):
            await send_mail_to_admin_async(
                '用户模仿失败',
                f'手机号码: {user.phone_number} 的账号模仿失败: 简介超出字数限制。'
            )
            return f'手机号码: {user.phone_number} 的账号模仿失败: 简介超出字数限制。'
        else:
            print(f"更新用户资料时发生错误: {str(e)}")
            return f"更新失败: {str(e)}"

    # 上传头像
    file = await client.upload_file(img_file)  # 确保使用 await
    await client(UploadProfilePhotoRequest(  # 确保使用 await
        file=file
    ))

    # 发送成功通知
    url = "http://127.0.0.1:8000/"
    title = f"{msg}{phone_number} 的账号成功替换上原用户名【{original_username}】的资料。"
    content = f"""
            <table border="1">
                <thead>
                    <tr>
                        <th>变更字段</th>
                        <th>变更后</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>名称</td>
                        <td>{name}{last_name if last_name else ""}</td>
                    </tr>
                    <tr>
                        <td>简介</td>
                        <td>{about}</td>
                    </tr>
                    <tr>
                        <td>用户名</td>
                        <td>{username}</td>
                    </tr>
                    <tr>
                        <td>头像</td>
                        <td><img src="{url}{img_file}"></td>
                    </tr>
                </tbody>
            </table>
            """
    await send_mail_to_admin_async(
        title,
        content,
    )
    await client.disconnect()  # 确保使用 await
    return title + "\n" + f"用户名:{username}\n名称:{name}(主)\n简介:{about}"


import random
import string


def generate_random_letters(length):
    """
    生成指定长度的随机字母字符串。

    :param length: int, 要生成的字符串长度
    :return: str, 随机字母字符串
    """
    if length <= 0:
        raise ValueError("长度必须为正整数")
    return ''.join(random.choices(string.ascii_letters, k=length))


if __name__ == '__main__':
    print(generate_random_letters(10))
