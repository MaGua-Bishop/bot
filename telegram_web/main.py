"""检查用户名是否过期，过期则需要发送报警"""
import os

import utils

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')
import django

django.setup()
from app import models


def start():
    """检查用户名是否过期，过期则需要发送报警"""
    for user in models.TelegramUserName.objects.filter(status=True):
        status, name, about, image, image_name = utils.get_telegram_user_data(user.username)
        if status:

            if user.name != name or user.about != about or user.image.name != "images/" + image_name:
                # 数据变更

                tr = ""
                if user.name != name:
                    tr += f"""<tr>
      <td>名称</td>
      <td>{user.name}</td>
      <td>{name}</td>
    </tr>"""
                if user.about != about:
                    tr += f"""<tr>
                          <td>简介</td>
                          <td>{user.about}</td>
                          <td>{about}</td>
                        </tr>"""
                if user.image.name != "images/" + image_name:
                    url = "http://127.0.0.1:8000/"
                    tr += f"""<tr>
                                  <td>头像</td>
                                  <td><img href="{url}{"media/"+user.image.name}"></td>
                                  <td><img href="{url}{"media/images/" + image_name}"></td>
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
            # 用户不存在
            utils.send_mail_to_admin(f"用户 {name}<{user.username}> 不存在", "")


if __name__ == '__main__':
    start()
