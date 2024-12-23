import asyncio
import os
import django

import utils

# 设置 Django 环境变量
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')

# 初始化 Django
django.setup()

from app import models
import utils

if __name__ == '__main__':
    status, name, about, image, image_name, first_name, last_name = utils.get_telegram_user_data('Gky2019')
    print(status)
