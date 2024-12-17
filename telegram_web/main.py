"""检查用户名是否过期，过期则需要发送报警"""
import os
import random

import utils

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')
import django

django.setup()
from app import models


def remove_hash_from_filename(filename):
    """去掉文件名中的哈希值部分"""
    base_name, ext = os.path.splitext(filename)  # 获取文件名和扩展名
    # 假设哈希值是通过下划线分隔的，去掉最后一部分
    parts = base_name.rsplit('_', 1)  # 从右侧分割一次
    return parts[0] + ext if len(parts) > 1 else filename  # 重新组合文件名


import asyncio
import time

def start():
    """检查用户名是否过期，过期则需要发送报警"""
    while True:  # 持续执行
        for user in models.TelegramUserName.objects.filter(status=True):
            status, name, about, image, image_name = utils.get_telegram_user_data(user.username)
            if status:
                data_changed = False  # 用于标记数据是否发生变化

                # 检查头像
                new_image_path = "images/" + image_name
                if remove_hash_from_filename(user.original_image.name) != remove_hash_from_filename(
                        new_image_path):  # 去掉哈希值进行比较
                    data_changed = True
                    # 更新原头像路径
                    user.original_image = user.image.url  # 保存当前头像的 URL
                    from django.core.files.base import ContentFile
                    user.image = ContentFile(image, name=image_name)  # 更新头像

                # 检查名称和简介
                if user.name != name:
                    data_changed = True
                if user.about != about:
                    data_changed = True

                # 如果有数据变更，更新数据库并发送邮件
                if data_changed:
                    changes = []  # 用于存储变更信息

                    if user.name != name:  # 检查 name 是否变化
                        changes.append(f"""<tr>
          <td>名称</td>
          <td>{user.name}</td>
          <td>{name}</td>
        </tr>""")

                    if user.about != about:  # 检查 about 是否变化
                        changes.append(f"""<tr>
                              <td>简介</td>
                              <td>{user.about}</td>
                              <td>{about}</td>
                            </tr>""")

                    # 保存变更到数据库
                    user.name = name
                    user.about = about
                    user.save()

                    # 构建邮件内容
                    tr = "".join(changes)  # 将所有变更信息合并
                    url = "http://127.0.0.1:8000/"
                    if remove_hash_from_filename(user.original_image.name) != remove_hash_from_filename(
                            user.image.name):  # 去掉哈希值进行比较
                        tr += f"""<tr>
                                      <td>头像</td>
                                      <td><img src="{url}{"media/" + user.original_image.name}"></td>
                                      <td><img src="{url}{"media/images/" + image_name}"></td>
                                    </tr>"""

                    # 只有在有变更时才发送邮件
                    if tr:  # 确保变更内容不为空
                        text = f"""变更内容如下: 
    <table border="1">
    <thead>
    <tr>
      <th>变更字段</th>
      <th>变更前</th>
      <th>变更后</th>
    </tr>
    </thead>
    <tbody>
    {tr}
    </tbody> 
    </table>
                        """
                        # 使用 asyncio.run 调用异步邮件发送
                        asyncio.run(utils.send_mail_to_admin_async(f"【资料变更】 {name}<{user.username}>", text))
                else:
                    print(f"{user.username}数据没有变化")
            else:
                # 用户不存在
                # 使用 asyncio.run 调用异步邮件发送
                # asyncio.run(utils.send_mail_to_admin_async(f"用户 {name}<{user.username}> 不存在", ""))

                # 尝试使用白号进行替换
                white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)  # 获取所有白号
                img_file = user.image.name if user.image else None

                while white_users.exists():
                    # 随机选择一个白号进行尝试
                    copy_user = random.choice(white_users)  # 随机选择一个白号

                    try:
                        # 在第1分钟和第2分钟尝试
                        time.sleep(60)  # 等待 1 分钟
                        message = asyncio.run(utils.copy_user_info(
                            user=copy_user,  # 当前白号用户对象
                            username=user.username,  # 被模仿用户的用户名
                            img_file=img_file,  # 被模仿用户的头像文件
                            about=user.about,  # 被模仿用户的简介
                            name=user.name  # 被模仿用户的名称
                        ))
                        if message:
                            user.status = False
                            user.save()
                            copy_user.copyObj = user
                            copy_user.save()
                            asyncio.run(utils.send_mail_to_admin_async(f"【用户名<{user.username}>不存在】", message))
                            break  # 成功后退出循环
                    except Exception as e:
                        asyncio.run(utils.send_mail_to_admin_async(f"白号 {copy_user.username} 替换失败: {str(e)}", ""))

                    # 重新获取白号列表，以便在下次循环中选择新的白号
                    white_users = models.CopyTelegramUser.objects.filter(copyObj_id=None)  # 更新白号列表

                if not white_users.exists():
                    asyncio.run(utils.send_mail_to_admin_async("没有可用的白号用户", ""))

        time.sleep(0.1)  # 每次查询之间间隔 0.1 秒

if __name__ == '__main__':
    start()
