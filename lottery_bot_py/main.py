import asyncio
import os
import django

from django.core.management import call_command

# 设置 Django 环境变量
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'lottery_bot_py.settings')

# 初始化 Django
django.setup()

if __name__ == '__main__':
    call_command('daily_luckydraw_task')
