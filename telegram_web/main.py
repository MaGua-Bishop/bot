"""检查用户名是否过期，过期则需要发送报警"""
import asyncio
import os

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


def start():
    """检查用户名是否过期，过期则需要发送报警"""
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
                utils.send_mail_to_admin(f"【资料变更】 {name}<{user.username}>", text)
            else:
                print(f"{user.username}数据没有变化")
        else:
            # 用户不存在
            utils.send_mail_to_admin(f"用户 {name}<{user.username}> 不存在", "")
            copy_user = models.CopyTelegramUser.objects.filter(copyObj_id=None).first()
            if copy_user:
                img_file = user.image.name if user.image else None
                message = asyncio.run(utils.copy_user_info(
                    user=copy_user,  # 当前用户对象
                    username=user.username,  # 被模仿用户的用户名
                    img_file=img_file,  # 被模仿用户的头像文件
                    about=user.about,  # 被模仿用户的简介
                    name=user.name  # 被模仿用户的名称
                ))
                if message:
                    user.status = False
                    user.save()
                    utils.send_mail_to_admin(f"【复制用户】 {name}<{user.username}>", message)


if __name__ == '__main__':
    start()
