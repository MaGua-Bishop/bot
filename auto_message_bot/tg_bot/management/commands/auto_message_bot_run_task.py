from datetime import datetime
from django.core.management.base import BaseCommand
import time

from telebot import types

from tg_bot.models import TgButton, TgTimingMessage, TGInvite
from tg_bot.bot_config import bot
from tg_bot.utils import create_markup


class Command(BaseCommand):
    help = '每分钟执行一次的任务'

    def handle(self, *args, **kwargs):
        while True:
            self.my_scheduled_task()
            time.sleep(60)

    def my_scheduled_task(self):
        print("检查是否有定时消息需要发送")
        current_time = datetime.now()
        current_hour = current_time.hour
        current_minute = current_time.minute
        print(f"当前时间: {current_time.strftime('%H:%M:%S')}")

        # 使用 filter 方法从数据库中获取当前时间需要发送的定时消息
        message_list = TgTimingMessage.objects.filter(time__hour=current_hour, time__minute=current_minute)
        if not message_list.exists():
            print(f"当前时间: {current_time.strftime('%H:%M:%S')} 没有需要发送的消息")
            return

        for message in message_list:
            self.send_message(message)

    def send_message(self, message):
        tg_id = message.tg_id
        # 获取所有群/频道
        group_list = TGInvite.objects.filter(inviter_id=tg_id)
        print(f"共{len(group_list)}个群/频道")
        if not group_list.exists():
            print(f"{tg_id}没有群/频道")
            return

        # 获取按钮信息
        button_list = TgButton.objects.filter(timing_message=message)
        print(f"共{len(button_list)}个按钮")
        markup = types.InlineKeyboardMarkup()  # 不指定 row_width
        row = []  # 用于存储一行的按钮
        for index, button in enumerate(button_list):
            row.append(types.InlineKeyboardButton(text=button.name, url=button.url))
            # 每两个按钮一组
            if (index + 1) % 2 == 0:
                markup.add(*row)  # 添加当前行的按钮
                row = []  # 清空当前行的按钮

        # 如果还有剩余的按钮，添加到最后一行
        if row:
            markup.add(*row)

        for group in group_list:
            # 检查消息是否存在
            try:
                bot.copy_message(
                    chat_id=group.chat_id,  # 目标聊天 ID
                    from_chat_id=message.tg_id,  # 来源聊天 ID
                    message_id=message.message_id,  # 消息 ID
                    reply_markup=markup  # 附带按钮
                )
            except Exception as e:
                print(f"Error sending message to {group.chat_id}: {e}")




