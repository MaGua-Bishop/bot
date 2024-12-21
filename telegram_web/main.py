import os
import django

import utils

# 设置 Django 环境变量
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')

# 初始化 Django
django.setup()

from app import models  # 确保模型的引用在 django.setup() 之后

if __name__ == '__main__':
    for user in models.TelegramUserName.objects.filter(status=True).order_by('-id'):
        text= f'{user.username}'
        print(text)
        status, name, about, image, image_name = utils.get_telegram_user_data(user.username)
        print(status, name, about)
