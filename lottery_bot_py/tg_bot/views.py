from datetime import timedelta

import telebot
from django.http import JsonResponse
from django.views import View
from .bot_config import bot
import tg_bot.bot_message_handler
import tg_bot.bot_callback_handler
import tg_bot.bot_inline_handler
import json
from django.utils import timezone
import pytz
from .models import TgUser, TgRecharge
from decimal import Decimal


class BotView(View):
    def post(self, request, *args, **kwargs):
        json_str = request.body.decode('UTF-8')
        update = telebot.types.Update.de_json(json_str)
        bot.process_new_updates([update])
        return JsonResponse({'status': 'ok'})


class RechargeView(View):
    def post(self, request):
        data = json.loads(request.body)
        from_account = data.get('from')
        to_account = data.get('to')
        amount = data.get('amount')
        transaction_time = data.get('transactionTime')
        tx_id = data.get('txId')

        print(data)

        # 获取当前时间15分钟前的时间戳
        shanghai_tz = pytz.timezone('Asia/Shanghai')
        time_limit = timezone.now().astimezone(shanghai_tz) - timedelta(minutes=15)

        results = TgRecharge.objects.filter(
            create_time__gte=time_limit,
            money=amount,
            status=0
        )

        if results.exists():
            des = '充值成功'
            data = results.first()
            user = TgUser.objects.get(tg_id=data.tg_id)
            print(f"充值金额: {data.money}")
            results.update(status=1)

            user.money += data.money
            user.save()
            # 发送成功消息
            bot.send_message(data.tg_id, f'充值成功\n当前余额:<b>{user.money}</b>', parse_mode='html')
        else:
            des = '没有找到对应的充值记录'

        return JsonResponse({'status': 'ok', 'des': des})
