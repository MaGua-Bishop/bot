import random
import time
from datetime import timedelta
from decimal import Decimal, ROUND_HALF_UP

import schedule
from django.utils import timezone
from django.core.management.base import BaseCommand
from tg_bot.bot_config import bot
from tg_bot.models import TgUser, TgLuckydraw
from tg_bot.utils import get_work_group_id, divide_red_packet


# 定义格式化日期和时间的函数
def format_datetime():
    local_date = timezone.now().replace(hour=22, minute=0, second=0, microsecond=0)
    return local_date.strftime('%Y-%m-%d %H:%M')


# 定义定时任务
def execute_lottery():
    current_date = timezone.localdate()
    print(f"定时任务已启动，日期:{current_date},开始执行抽奖")

    # 查询当天的所有记录，且状态为0（未开奖）
    luckydraw_list = TgLuckydraw.objects.filter(luckydraw_time__date=current_date, status=0)
    print(luckydraw_list)

    if luckydraw_list.exists():

        print(f"开始进行抽奖... 参与用户数: {len(luckydraw_list)}")

        total_pool = Decimal(len(luckydraw_list) * 10)  # 每个用户10积分
        # 检查奖池积分是否达到100
        if total_pool >= Decimal(100):
            # 奖池的70%作为中奖积分
            integral = total_pool * Decimal(0.7)
            integral = integral.quantize(Decimal('0.00'), rounding=ROUND_HALF_UP)
            print(f"奖池总积分: {total_pool}, 中奖积分: {integral}")

            # 进行抽奖，随机选择中奖者
            winner = luckydraw_list.order_by('?').first()
            # money_list = divide_red_packet(integral, len(winners))

            # 更新中奖者的金额并发放奖励
            winner.money = integral
            winner.save()
            tg_user = TgUser.objects.get(tg_id=winner.tg_id)
            tg_user.money += winner.money
            tg_user.save()

            # 构建中奖者信息
            text = f"<b>{format_datetime()}</b> 开奖信息\n参与用户数: {len(luckydraw_list)}\n奖池总积分: <b>{integral}</b>\n中奖用户: \n<code>{winner.tg_id}</code>\t中奖积分:<b>{integral}</b>"

            # 发送消息到工作群和用户
            send_messages_to_groups(text)
            send_messages_to_users(luckydraw_list, text)
            luckydraw_list.update(status=1, update_time=timezone.now())
        else:
            print(f"没有达到100积分，今天不开奖，参与用户数: {len(luckydraw_list)}")

            next_draw_time = timezone.now() + timedelta(days=1)
            next_draw_time = next_draw_time.replace(hour=22, minute=0, second=0, microsecond=0)
            # 完整的文本消息
            participants = "\n".join([f"<code>{luckydraw.tg_id}</code>" for luckydraw in luckydraw_list])
            text = f"<b>{format_datetime()}</b> \n开奖信息: 当前奖池积分未达到100，今天未进行开奖。\n下次开奖时间: {next_draw_time.strftime('%Y-%m-%d %H:%M')}\n"
            full_text = f"{text}\n参与用户:\n{participants}"
            send_messages_to_groups(
                full_text)
            send_messages_to_users(luckydraw_list,
                                   f"<b>{format_datetime()}</b> 开奖信息: 当前奖池积分未达到100，今天未进行开奖。\n下次开奖时间: {next_draw_time.strftime('%Y-%m-%d %H:%M')}")
            luckydraw_list.update(luckydraw_time=next_draw_time, update_time=timezone.now())
    else:
        print("没有抽奖的用户")
        send_messages_to_groups(f"<b>{format_datetime()}</b> \n开奖信息: 今日无用户参与\n参与用户数: 0")


# 向工作群发送消息
def send_messages_to_groups(text):
    chat_id = get_work_group_id();
    try:
        bot.send_message(chat_id=chat_id, text=text, parse_mode='html')
    except Exception as e:
        print(f"发送消息到群 {chat_id} 失败: {e}")


# 向用户发送消息
def send_messages_to_users(luckydraw_list, text):
    for luckydraw in luckydraw_list:
        try:
            bot.send_message(chat_id=luckydraw.tg_id, text=text, parse_mode='html')
        except Exception as e:
            # 记录失败的错误并跳过当前用户
            print(f"发送消息到用户 {luckydraw.tg_id} 失败: {e}")
            continue  # 继续处理下一个用户


# 定义定时任务
class Command(BaseCommand):
    help = '每天22点执行一次的任务'

    def handle(self, *args, **kwargs):
        # 每天 22:00 执行一次
        schedule.every().day.at("22:00").do(execute_lottery)
        # schedule.every(1).minute.do(execute_lottery)
        while True:
            # 运行所有等待中的任务
            schedule.run_pending()
            time.sleep(1)
