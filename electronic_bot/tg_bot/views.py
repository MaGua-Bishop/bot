import json

import telebot
from django.http import JsonResponse
from django.views import View
from .bot_config import bot
import tg_bot.bot_message_handler
import tg_bot.bot_callback_handler
from datetime import timedelta
from django.utils import timezone
from .models import TgRecharge, TgUser, AmountChange
from decimal import Decimal
import pytz


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

        # 使用Django ORM替代原始SQL查询
        results = TgRecharge.objects.filter(
            create_time__gte=time_limit,
            money=amount,
            status=0,
            pay_type='USDT'
        )

        if results.exists():
            des = '充值成功'
            data = results.first()  # 获取第一个符合条件的记录
            user = TgUser.objects.get(tg_id=data.tg_id)

            # 更新充值记录的状态
            results.update(status=1)  # 批量更新状态为1，避免逐条循环

            # 汇率转换
            usdt_to_cny_rate = Decimal('7.26')
            print(f"用户充值的USDT金额: {data.money}")
            cny_amount = data.money * usdt_to_cny_rate  # 将USDT转换为CNY
            print(f"给用户转成的CNY金额: {cny_amount}")

            # 更新用户余额
            before_amount = user.money
            user.money += cny_amount
            user.save()
            AmountChange.objects.create(
                user=user,
                change_type='+',
                name='USDT充值',
                change_amount=cny_amount,
                before_amount=before_amount,
                after_amount=user.money
            )
            # 发送成功消息
            bot.send_message(data.tg_id, f'USDT充值成功\n充值USDT:<b>{data.money}</b>', parse_mode='html')
        else:
            des = '没有找到对应的充值记录'

        return JsonResponse({'status': 'ok', 'des': des})
