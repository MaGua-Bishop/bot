import asyncio
import random
from django.core.management.base import BaseCommand
import time
from concurrent.futures import ThreadPoolExecutor
from requests.exceptions import ProxyError
import utils
from app import models
import os


def remove_hash_from_filename(filename):
    """去掉文件名中的哈希值部分"""
    base_name, ext = os.path.splitext(filename)  # 获取文件名和扩展名
    parts = base_name.rsplit('_', 1)  # 从右侧分割一次
    return parts[0] + ext if len(parts) > 1 else filename  # 重新组合文件名


def process_user(user):
    try:
        # 尝试获取 Telegram 用户数据
        status, name, about, image, image_name, first_name, last_name = utils.get_telegram_user_data(user.username)
        if status:
            data_changed = False  # 用于标记数据是否发生变化

            if image and image_name:  # 确保 image 和 image_name 都不为 None
                new_image_path = "images/" + image_name
                if remove_hash_from_filename(user.original_image.name) != remove_hash_from_filename(new_image_path):
                    data_changed = True
                    from django.core.files.base import ContentFile
                    user.image = ContentFile(image, name=image_name)  # 更新头像
                    user.original_image = user.image

            if user.name != name:
                user.name = name
                user.save()
            if user.about != about:
                user.about = about
                user.save()
                data_changed = True
            if user.first_name != first_name:
                user.first_name = first_name
                user.save()
                data_changed = True
            if user.last_name != last_name:
                user.last_name = last_name
                user.save()
                data_changed = True
            if data_changed:
                changes = []
                if user.name != name:
                    changes.append(f"""<tr><td>名称</td><td>{user.name}</td><td>{name}</td></tr>""")
                if user.about != about:
                    changes.append(f"""<tr><td>简介</td><td>{user.about}</td><td>{about}</td></tr>""")

                tr = "".join(changes)
                url = "http://127.0.0.1:8000/"
                if remove_hash_from_filename(user.original_image.name) != remove_hash_from_filename(user.image.name):
                    tr += f"""<tr><td>头像</td><td><img src=\"{url}media/{user.original_image.name}\"></td><td><img src=\"{url}media/images/{image_name}\"></td></tr>"""

                if tr:
                    text = f"""变更内容如下:<table border=\"1\"><thead><tr><th>变更字段</th><th>变更前</th><th>变更后</th></tr></thead><tbody>{tr}</tbody></table>"""
                    print(text)
        else:
            process_inactive_user(user)

    except ProxyError:
        pass  # 跳过当前用户
    except Exception as e:
        print(f"处理用户 {user.username} 时发生错误: {e}")


async def process_inactive_user(user):
    """处理状态为 False 的用户"""
    white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)
    img_file = user.image.name if user.image else None
    attempts = 0
    success = False

    while white_users.exists() and attempts < 3:
        copy_user = random.choice(white_users)
        try:
            await asyncio.sleep(60 * (attempts + 1))
            print(f"白号 {copy_user.phone} 替换中... 尝试次数: {attempts + 1}")
            message = await utils.copy_user_info(
                user=copy_user,
                username=user.username,
                img_file=img_file,
                about=user.about,
                name=user.name,
                first_name=user.first_name,
                last_name=user.last_name,
                msg=f"【用户名<{user.username}>不存在】",
            )
            if message:
                user.status = False
                user.save()
                copy_user.copyObj = user
                copy_user.save()
                success = True
                break  # 成功后退出循环
            else:
                attempts += 1
                asyncio.run(utils.send_mail_to_admin_async(
                    "白号替换失败",
                    f"白号 {copy_user.phone} 替换失败，尝试次数: {attempts}，尝试下一个白号替换。"
                ))
                print(f"用户名 {user.username} 被占用，尝试下一个白号。")
        except Exception as e:
            print(f"白号 {copy_user.phone} 替换失败: {str(e)}")
        attempts += 1
        white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)

    if success:
        pass
    else:
        for i in range(3):
            await asyncio.sleep(60 * (45 + i))
            print(f"白号 {copy_user.phone} 再次尝试替换中... 尝试次数: {i + 1}")
            message = await utils.copy_user_info(
                user=copy_user,
                username=user.username,
                img_file=img_file,
                about=user.about,
                name=user.name,
                first_name=user.first_name,
                last_name=user.last_name,
                msg=f"【用户名<{user.username}>不存在】",
            )
            if message:
                user.status = False
                user.save()
                copy_user.copyObj = user
                copy_user.save()
                success = True
                break
            else:
                asyncio.run(utils.send_mail_to_admin_async(
                    "白号替换失败",
                    f"白号 {copy_user.phone} 替换失败，尝试次数: {i+ 3}，尝试下一个白号替换。"
                ))
        if not success:
            await utils.send_mail_to_admin_async(f"用户名【{user.username}】替换失败",
                                                 f"用户 {user.username} 的信息替换失败，所有尝试均未成功。")


class Command(BaseCommand):
    help = 'Run the Telegram web task'

    def handle(self, *args, **kwargs):
        self.start()

    def start(self):
        while True:
            users = models.TelegramUserName.objects.filter(status=True)
            with ThreadPoolExecutor(max_workers=10) as executor:
                executor.map(process_user, users)
            time.sleep(0.1)
