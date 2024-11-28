import threading
from datetime import datetime
from django.core.management.base import BaseCommand
import time

from telebot import types

from tg_bot.models import TgButton, TgTimingMessage, TGInvite, TGInviteTimingMessage
from tg_bot.bot_config import bot


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
        invite_timing_message_list = TGInviteTimingMessage.objects.filter(time__hour=current_hour,
                                                                          time__minute=current_minute)
        if not invite_timing_message_list.exists():
            print(f"当前时间: {current_time.strftime('%H:%M:%S')} 没有需要发送的消息")
            return

        # 为每个定时消息创建一个线程
        threads = []
        for invite_timing_message in invite_timing_message_list:
            thread = threading.Thread(target=self.send_message, args=(invite_timing_message,))
            threads.append(thread)
            thread.start()

        # 等待所有线程完成
        for thread in threads:
            thread.join()

    def send_message(self, invite_timing_message):
        invite_id = invite_timing_message.invite_id
        timing_message_id = invite_timing_message.timing_message_id

        # 获取群信息
        try:
            group = TGInvite.objects.get(id=invite_id)
        except TGInvite.DoesNotExist:
            print(f"Group with id {invite_id} not found.")
            return

        # 获取按钮信息
        button_list = TgButton.objects.filter(timing_message=timing_message_id)
        print(f"共{len(button_list)}个按钮")

        markup = types.InlineKeyboardMarkup()
        row = []  # 用于存储一行的按钮

        # 将按钮按行添加
        for index, button in enumerate(button_list):
            row.append(types.InlineKeyboardButton(text=button.name, url=button.url))

            # 每两个按钮一组
            if (index + 1) % 2 == 0:
                markup.add(*row)  # 添加当前行的按钮
                row = []  # 清空当前行的按钮

        # 如果还有剩余的按钮，添加到最后一行
        if row:
            markup.add(*row)

        try:
            # 获取定时消息
            timing_message = TgTimingMessage.objects.get(id=timing_message_id)
            bot.copy_message(
                chat_id=group.chat_id,  # 目标聊天 ID
                from_chat_id=timing_message.tg_id,  # 来源聊天 ID
                message_id=timing_message.message_id,  # 消息 ID
                reply_markup=markup  # 附带按钮
            )
            print(f"Message sent to {group.chat_id} from {timing_message.tg_id}")

        except TgTimingMessage.DoesNotExist:
            print(f"TgTimingMessage with id {timing_message_id} not found.")
        except Exception as e:
            print(f"Error sending message to {group.chat_id}: {e}")
