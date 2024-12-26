import asyncio
import random
from django.core.management.base import BaseCommand
import time
from concurrent.futures import ThreadPoolExecutor
from requests.exceptions import ProxyError
import utils
from app import models
import os
from django.utils import timezone
from datetime import timedelta, datetime


def remove_hash_from_filename(filename):
    """去掉文件名中的哈希值部分"""
    base_name, ext = os.path.splitext(filename)  # 获取文件名和扩展名
    parts = base_name.rsplit('_', 1)  # 从右侧分割一次
    return parts[0] + ext if len(parts) > 1 else filename  # 重新组合文件名


def process_user(user):
    now = timezone.now().replace(second=0, microsecond=0)
    auto_user = models.AutoReplaceUser.objects.filter(
        create_time__in=[now],
        status=False,
        execution=False,
    ).first()

    if auto_user:
        auto_user.execution = True
        auto_user.save()
        user = auto_user.user
        copy_user = auto_user.copy_user
        img_file = user.image.name if user.image else None
        current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"时间:{current_time}白号 {copy_user.phone} 替换中用户名:{user.username}")
        # 调用 utils.copy_user_info 函数进行信息替换
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        message = loop.run_until_complete(utils.copy_user_info(
            user=copy_user,
            username=user.username,
            img_file=img_file,
            about=user.about,
            name=user.name,
            first_name=user.first_name,
            last_name=user.last_name,
            msg=f"【用户名<{user.username}>不存在】",
        ))
        loop.close()
        if message:
            # 所有的关于 AutoReplaceUser 的 user 的状态改成 True
            auto_replace_users = models.AutoReplaceUser.objects.filter(user=user)
            auto_replace_users.update(status=True)
            user.status = False
            user.save()
            copy_user.copyObj = user
            copy_user.save()
        else:
            attempts = models.AutoReplaceUser.objects.filter(user=user, execution=True).count()
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            loop.run_until_complete(utils.send_mail_to_admin_async(
                "白号替换失败",
                f"白号 {copy_user.phone} 替换用户名:【{user.username}】失败，尝试次数: {attempts}"
            ))
            loop.close()
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
            if user.about != about:
                user.about = about
                data_changed = True
            if user.first_name != first_name:
                user.first_name = first_name
                data_changed = True
            if user.last_name != last_name:
                user.last_name = last_name
                data_changed = True
            user.save()

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
            print(f"用户名 {user.username} 不存在")
            # 调用随机获取三个白号
            random_copy_user(user)
    except ProxyError:
        pass
    except Exception as e:
        print(f"处理用户 {user.username} 时发生错误: {e}")


# def random_copy_user(user):
#     if not models.AutoReplaceUser.objects.filter(user=user).exists():
#         white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)
#         random_users = random.sample(list(white_users), 3)
#
#         for index, copy_user in enumerate(random_users):
#             # 第一条记录，创建时间为当前时间加上1分钟
#             create_time_1 = timezone.now().replace(second=0, microsecond=0) + timedelta(minutes=index + 1)
#             models.AutoReplaceUser.objects.create(user=user, copy_user=copy_user, create_time=create_time_1)
#             # 第二条记录，创建时间为当前时间加上45、46、47分钟
#             create_time_2 = timezone.now().replace(second=0, microsecond=0) + timedelta(minutes=index + 45 + 1)
#             models.AutoReplaceUser.objects.create(user=user, copy_user=copy_user, create_time=create_time_2)
def random_copy_user(user):
    # 首先判断是否已存在与该 user 相关的记录
    if not models.AutoReplaceUser.objects.filter(user=user).exists():
        # 获取未被复制的白名单用户
        white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)

        # 检查 white_users 中是否至少有 3 个用户
        if len(white_users) < 3:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            loop.run_until_complete(utils.send_mail_to_admin_async(
                "没有多余的白号了",
                f"请及时添加白号，至少需要3个以上的白号"
            ))
            loop.close()
            return

        # 随机选择 3 个用户
        random_users = random.sample(list(white_users), 3)

        for index, copy_user in enumerate(random_users):
            # 第一条记录，创建时间为当前时间加上1分钟
            create_time_1 = timezone.now().replace(second=0, microsecond=0) + timedelta(minutes=index + 1)
            models.AutoReplaceUser.objects.create(user=user, copy_user=copy_user, create_time=create_time_1)

            # 第二条记录，创建时间为当前时间加上45、46、47分钟
            create_time_2 = timezone.now().replace(second=0, microsecond=0) + timedelta(minutes=index + 45 + 1)
            models.AutoReplaceUser.objects.create(user=user, copy_user=copy_user, create_time=create_time_2)


class Command(BaseCommand):
    help = 'Run the Telegram web task'

    def handle(self, *args, **kwargs):
        self.start()

    def start(self):
        while True:
            # , username="testtest666888"
            users = models.TelegramUserName.objects.filter(status=True)
            with ThreadPoolExecutor(max_workers=10) as executor:
                executor.map(process_user, users)
            time.sleep(0.1)
