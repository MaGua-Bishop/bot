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
import time


def save_image(url: str):
    """保存图片"""
    result = requests.get(url).content
    return result


def get_telegram_user_data(username: str):
    """获取用户的信息"""
    status = True
    url = f"https://t.me/{username}"

    result = requests.get(url).text
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
    if img_url != "":
        image = save_image(img_url)
        image_name = img_url.split('/')[-1][:20] + ".jpg"
    return status, name, about, image, image_name


def send_mail_to_admin(title, content):
    print(title, content)
    """发送信息给系统管理员"""
    sender_email = settings.EMAIL_USER
    sender_password = settings.EMAIL_PASSWORD
    admin_email = "li2604984003@gmail.com"
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
    client = TelegramClient(session, app_id, app_hash)
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


def is_username_available(client, username):
    """检查用户名是否可用"""
    try:
        result = client(CheckUsernameRequest(username=username))
        return result
    except Exception as e:
        print(f"检查用户名 {username} 时发生错误: {str(e)}")
        return False


def copy_user_info(user, username, img_file, about, name):
    """模仿用户"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    img_file = f"media/{img_file}"

    # 读取 JSON 文件以获取 API 凭证
    with open(json_file, "r") as fp:
        data = json.loads(fp.read())
    app_id = data['app_id']
    app_hash = data['app_hash']
    proxy = (socks.SOCKS5, '127.0.0.1', 7890, True)
    client = TelegramClient(session, app_id, app_hash, proxy=proxy)
    # 使用同步 API
    client.start()

    original_username = username
    last_word = original_username[-1]
    username = f"{original_username}{last_word}"

    while not is_username_available(client, username):
        username += last_word

    try:
        client(UpdateUsernameRequest(username=username))
        print(f"用户名已更新为: {username}")
        # 更新其他用户资料
        client(UpdateProfileRequest(
            first_name=name,
            last_name="(主)",  # 如果需要可以设置
            about=about  # 简介
        ))
    except Exception as e:
        print(f"更新用户资料时发生错误: {str(e)}")
        return f"更新失败: {str(e)}"

    # 上传头像
    file = client.upload_file(img_file)
    client(UploadProfilePhotoRequest(
        file=file
    ))
    client.disconnect()
    return "更新成功"


if __name__ == '__main__':
    import os

    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')
    import django

    django.setup()
    from app import models

    data = models.CopyTelegramUser.objects.first()
    get_user_info(data)
