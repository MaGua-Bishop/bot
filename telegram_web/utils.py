import json
import requests
from lxml import etree
from telethon.sync import TelegramClient
from telethon.tl.functions.account import UpdateProfileRequest
from telethon.tl.functions.photos import UploadProfilePhotoRequest


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
    """发送信息给系统管理员"""
    print(title, content)


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


def copy_user_info(user, username, img_file, about, name):
    """模仿用户"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    img_file = f"media/{img_file}"
    with open(json_file, "r") as fp:
        data = json.loads(fp.read())
    app_id = data['app_id']
    app_hash = data['app_id']
    client = TelegramClient(session, app_id, app_hash)
    # 使用同步 API
    client.start()

    client(UpdateProfileRequest(
        first_name=name,
        last_name="(主)",  # 如果需要可以设置
        about=about  # 简介
    ))
    file = client.upload_file(img_file)
    client(UploadProfilePhotoRequest(
        file=file
    ))
    client.disconnect()


if __name__ == '__main__':
    import os

    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')
    import django

    django.setup()
    from app import models

    data = models.CopyTelegramUser.objects.first()
    get_user_info(data)
