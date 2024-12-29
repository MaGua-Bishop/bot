import asyncio
import os
import django

import utils
from django.core.management import call_command

# 设置 Django 环境变量
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'telegram_web.settings')

# 初始化 Django
django.setup()

from app import models
import utils

if __name__ == '__main__':
    call_command('telegram_web_task')
    # utils.send_mail_to_admin(
    #     "邮件测试",
    #     f"邮件测试"
    # )
