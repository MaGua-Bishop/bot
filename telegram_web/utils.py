import json
import requests
import urllib3
from lxml import etree
from telethon.sync import TelegramClient
from telethon.tl.functions.account import UpdateProfileRequest, UpdateUsernameRequest, CheckUsernameRequest
from telethon.tl.functions.photos import UploadProfilePhotoRequest, DeletePhotosRequest, GetUserPhotosRequest
from telethon.tl.types import InputPeerUser, InputPhoto
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from django.conf import settings
from telethon.errors import UsernameOccupiedError, UsernameInvalidError, FloodWaitError
import socks
from urllib.parse import urlparse
import time
from requests.exceptions import RequestException, ProxyError

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def save_image(url: str):
    """保存图片"""
    result = requests.get(url).content
    return result


def get_telegram_user_data(username: str):
    """获取用户的信息"""
    print(f"开始获取用户信息：{username}")
    status = True
    first_name = ''
    last_name = ''
    url = f"https://t.me/{username}"
    proxy = settings.PROXY_URL
    proxies = {'http': proxy, 'https': proxy}
    try:
        result = requests.get(url, proxies=proxies, verify=False).text
        html = etree.HTML(result)
        name_parts = html.xpath('//div[@class="tgme_page_title"]//text()')
        name = ''.join(name_parts).strip()
        if name:
            name_parts = name.split(' ', 1)
            first_name = name_parts[0]
            last_name = name_parts[1] if len(name_parts) > 1 else ''
        else:
            name = ""
            status = False
    except ProxyError as e:
        name = ""
        status = True
    except RequestException as e:
        name = ""
        status = True

    about = " ".join(html.xpath('//div[@class="tgme_page_description "]//text()'))
    img_url = "".join(html.xpath('//img[@class="tgme_page_photo_image"]/@src'))
    if img_url and not img_url.startswith("data:image"):
        image = save_image(img_url)
        image_name = img_url.split('/')[-1][:20] + ".jpg"
    else:
        image = None
        image_name = None
    return status, name, about, image, image_name, first_name, last_name


def send_mail_to_admin(title, content):
    print(title, content)
    """发送信息给系统管理员"""
    url = settings.EMAIL_URL
    data = {
        'title': title,
        'content': content,
    }
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            print("邮件发送成功")
        else:
            print(f"邮件发送失败, 错误信息: {response.text}")
    except Exception as e:
        print(f"邮件发送失败, 请求异常: {e}")


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


from asgiref.sync import sync_to_async


def copy_user_info(user, username, img_file, about, name, first_name, last_name, msg=""):
    """模仿用户"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    img_file = f"media/{img_file}"

    # first_name，last_name，about为None时，使用默认值""
    if first_name is None:
        first_name = ""
    if last_name is None:
        last_name = ""
    if about is None:
        about = ""

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
    client.start()
    # 判断是否授权
    if not client.is_user_authorized():
        print(f"自动替换手机号:{phone_number}白号未授权，请先登录。")
        return None
    try:
        client(UpdateUsernameRequest(username=username))  # 确保使用 await
        user.username = username
        user.save()
        print(f"用户名已更新为: {username}")
    except Exception as e:
        print(f"用户名:{username}发生错误: {e}")
        return None
        # if "The username is already taken" in str(e):
        #     return None
    try:
        # 更新其他用户资料
        client(UpdateProfileRequest(  # 确保使用 await
            first_name=first_name,
            last_name=last_name,
            about=about  # 简介
        ))
    except Exception as e:
        # 根据异常内容判断是否是字数超出的问题
        if "first_name" in str(e) and "too long" in str(e):
            send_mail_to_admin(
                '用户模仿失败',
                f'{user.username} 的账号模仿失败: 名称超出字数限制。'
            )
            return f"{user.username} 的账号模仿失败: 名称超出字数限制。"
        elif "The provided bio is too long (caused by UpdateProfileRequest)" in str(e):
            send_mail_to_admin(
                '用户模仿失败',
                f'{user.username} 的账号模仿失败: 简介超出字数限制。'
            )
            return f'{user.username} 的账号模仿失败: 简介超出字数限制。'
        else:
            print(f"更新用户资料时发生错误: {str(e)}")
            return f"更新失败: {str(e)}"

    # 上传头像 ima_file
    is_image = True
    if "默认头像" not in img_file:
        me = client.get_me()  # 获取当前账户的信息
        user_id = me.id
        user = client.get_entity(user_id)  # 获取用户信息
        user_id = user.id

        # 获取用户的照片
        photos = client(GetUserPhotosRequest(
            user_id=user_id,  # 传递 user_id
            offset=0,  # 从第一张照片开始获取
            max_id=0,  # 没有特定的最大 ID
            limit=100  # 最多获取 100 张照片
        ))
        # 如果用户有照片，删除所有照片
        if photos.photos:
            # 使用 InputPhoto 构造照片对象，删除所有照片
            photo_ids = [
                InputPhoto(
                    id=photo.id,
                    access_hash=photo.access_hash,
                    file_reference=photo.file_reference  # 使用 file_reference
                ) for photo in photos.photos
            ]
            client(DeletePhotosRequest(id=photo_ids))  # 删除所有照片
        else:
            print("用户没有照片")

        file = client.upload_file(img_file)
        client(UploadProfilePhotoRequest(  # 确保使用 await
            file=file
        ))
    else:
        me = client.get_me()  # 获取当前账户的信息
        user_id = me.id
        user = client.get_entity(user_id)  # 获取用户信息
        user_id = user.id

        # 获取用户的照片
        photos = client(GetUserPhotosRequest(
            user_id=user_id,  # 传递 user_id
            offset=0,  # 从第一张照片开始获取
            max_id=0,  # 没有特定的最大 ID
            limit=100  # 最多获取 100 张照片
        ))

        # 如果用户有照片，删除所有照片
        if photos.photos:
            # 使用 InputPhoto 构造照片对象，删除所有照片
            photo_ids = [
                InputPhoto(
                    id=photo.id,
                    access_hash=photo.access_hash,
                    file_reference=photo.file_reference  # 使用 file_reference
                ) for photo in photos.photos
            ]
            client(DeletePhotosRequest(id=photo_ids))  # 删除所有照片
            is_image = False
        else:
            print("用户没有照片")

    # 发送成功通知
    url = "http://37.1.216.161:8000/"
    title = f"{msg}{phone_number} 的账号成功替换上原用户名【{username}】的资料。"
    if is_image:
        avatar_content = f'<td><img src="{url}{img_file}"></td>'
    else:
        avatar_content = f'<td>{username} 没有头像，白号头像已删除</td>'
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
                        <td>{first_name}{last_name if last_name else ""}</td>
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
                        {avatar_content}
                    </tr>
                </tbody>
            </table>
            """
    send_mail_to_admin(
        title,
        content,
    )
    client.disconnect()
    return title + "\n" + f"用户名:{username}\n名称:{name}\n简介:{about}"


def admin_copy_user_info(user, username, img_file, about, name, first_name, last_name, msg="", is_last_name=True):
    """模仿用户"""
    json_file = f"media/{user.fileJson}"
    session = f"media/{user.session}"
    img_file = f"media/{img_file}"

    # first_name，last_name，about为None时，使用默认值""
    if first_name is None:
        first_name = ""
    if last_name is None:
        last_name = ""
    if about is None:
        about = ""

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
    client.start()  # 确保使用 await 启动客户端

    original_username = username
    last_word = original_username[-1]
    username = f"{original_username}{last_word}"
    while not is_username_available(client, username):  # 确保使用 await
        username += last_word

    try:
        client(UpdateUsernameRequest(username=username))  # 确保使用 await
        user.username = username
        user.save()
        print(f"用户名已更新为: {username}")
    except Exception as e:
        print(f"用户名:{username}发生错误: {e}")

    try:
        if is_last_name:
            last_name += "(主)"
        # 更新其他用户资料
        client(UpdateProfileRequest(  # 确保使用 await
            first_name=first_name,
            last_name=last_name,
            about=about  # 简介
        ))
    except Exception as e:
        # 根据异常内容判断是否是字数超出的问题
        if "first_name" in str(e) and "too long" in str(e):
            send_mail_to_admin(
                '用户模仿失败',
                f'{user.username} 的账号模仿失败: 名称超出字数限制。'
            )
            return f"{user.username} 的账号模仿失败: 名称超出字数限制。"
        elif "The provided bio is too long (caused by UpdateProfileRequest)" in str(e):
            send_mail_to_admin(
                '用户模仿失败',
                f'手机号码: {user.phone_number} 的账号模仿失败: 简介超出字数限制。'
            )
            return f'手机号码: {user.phone_number} 的账号模仿失败: 简介超出字数限制。'
        else:
            print(f"更新用户资料时发生错误: {str(e)}")
            return f"更新失败: {str(e)}"

    # 上传头像 ima_file
    is_image = True
    if "默认头像" not in img_file:
        me = client.get_me()  # 获取当前账户的信息
        user_id = me.id
        user = client.get_entity(user_id)  # 获取用户信息
        user_id = user.id

        # 获取用户的照片
        photos = client(GetUserPhotosRequest(
            user_id=user_id,  # 传递 user_id
            offset=0,  # 从第一张照片开始获取
            max_id=0,  # 没有特定的最大 ID
            limit=100  # 最多获取 100 张照片
        ))
        # 如果用户有照片，删除所有照片
        if photos.photos:
            # 使用 InputPhoto 构造照片对象，删除所有照片
            photo_ids = [
                InputPhoto(
                    id=photo.id,
                    access_hash=photo.access_hash,
                    file_reference=photo.file_reference  # 使用 file_reference
                ) for photo in photos.photos
            ]
            client(DeletePhotosRequest(id=photo_ids))  # 删除所有照片
        else:
            print("用户没有照片")

        file = client.upload_file(img_file)
        client(UploadProfilePhotoRequest(  # 确保使用 await
            file=file
        ))
    else:
        me = client.get_me()  # 获取当前账户的信息
        user_id = me.id
        user = client.get_entity(user_id)  # 获取用户信息
        user_id = user.id

        # 获取用户的照片
        photos = client(GetUserPhotosRequest(
            user_id=user_id,  # 传递 user_id
            offset=0,  # 从第一张照片开始获取
            max_id=0,  # 没有特定的最大 ID
            limit=100  # 最多获取 100 张照片
        ))

        # 如果用户有照片，删除所有照片
        if photos.photos:
            # 使用 InputPhoto 构造照片对象，删除所有照片
            photo_ids = [
                InputPhoto(
                    id=photo.id,
                    access_hash=photo.access_hash,
                    file_reference=photo.file_reference  # 使用 file_reference
                ) for photo in photos.photos
            ]
            client(DeletePhotosRequest(id=photo_ids))  # 删除所有照片
            is_image = False
        else:
            print("用户没有照片")

    # 发送成功通知
    url = "http://37.1.216.161:8000/"
    title = f"{msg}{phone_number} 的账号成功替换上原用户名【{original_username}】的资料。"
    if is_image:
        avatar_content = f'<td><img src="{url}{img_file}"></td>'
    else:
        avatar_content = f'<td>{original_username} 没有头像，白号头像已删除</td>'
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
                        <td>{first_name}{last_name if last_name else ""}</td>
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
                        {avatar_content}
                    </tr>
                </tbody>
            </table>
            """
    send_mail_to_admin(
        title,
        content,
    )
    client.disconnect()  # 确保使用 await
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
