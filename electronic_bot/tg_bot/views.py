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
from .utlis import get_okex


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
            before_data_money = data.money
            print(f"充值金额: {data.money}")
            # 更新充值记录的状态
            results.update(status=1)  # 批量更新状态为1，避免逐条循环
            # 比例 0.002
            data.money = data.money * Decimal('1.002')
            print(f"充值加比例：{data.money}")
            # 汇率转换
            okex = get_okex()
            usdt_to_cny_rate = Decimal(str(okex))
            print(f"当前汇率: {usdt_to_cny_rate}")
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
            bot.send_message(data.tg_id, f'USDT充值成功\n充值USDT:<b>{before_data_money}</b>', parse_mode='html')
        else:
            des = '没有找到对应的充值记录'

        return JsonResponse({'status': 'ok', 'des': des})

#
# import random
# from datetime import datetime, timedelta
# from django.http import JsonResponse
# from django.views import View
#
# # 数据生成函数
# def generate_fake_data():
#     """生成 50 条假数据"""
#     platforms = ["ag", "pg", "mg", "pt"]
#     currencies = ["CNY", "USD", "EUR"]
#     game_names = ["Slot Adventure", "Lucky Slot", "Mega Jackpot"]
#
#     data = []
#     for i in range(50):
#         bet_time = datetime.now() - timedelta(days=random.randint(0, 2), hours=random.randint(0, 23))
#         last_update_time = bet_time + timedelta(minutes=random.randint(1, 120))
#
#         record = {
#             "playerId": f"tg214229809",
#             "platType": random.choice(platforms),
#             "currency": random.choice(currencies),
#             "gameType": "2",  # 固定为老虎机类型
#             "gameName": random.choice(game_names),
#             "round": f"{random.randint(10000000, 99999999)}",
#             "table": '1111',
#             "seat": '2222',
#             "betAmount": round(random.uniform(10, 1000), 2),
#             "validAmount": round(random.uniform(5, 1000), 2),
#             "settledAmount": round(random.uniform(-500, 500), 2),
#             "betContent": "Random Bet Content",
#             "status": random.choice([0, 1, 2, 3]),
#             "gameOrderId": f"{i + 1:06}",
#             "betTime": bet_time.strftime("%Y-%m-%d %H:%M:%S"),
#             "lastUpdateTime": last_update_time.strftime("%Y-%m-%d %H:%M:%S"),
#         }
#         data.append(record)
#     return data
#
# class RecordHistory(View):
#     """动态生成假数据并分页返回"""
#
#     def get(self, request):
#         page_no = int(request.GET.get("pageNo", 1))
#         page_size = int(request.GET.get("pageSize", 200))
#
#         all_data = generate_fake_data()  # 生成假数据
#         total = len(all_data)
#
#         # 分页逻辑
#         start = (page_no - 1) * page_size
#         end = start + page_size
#         paginated_data = all_data[start:end]
#
#         response = {
#             "code": 10000,
#             "msg": "Request succeeded",
#             "data": {
#                 "total": total,
#                 "pageNo": page_no,
#                 "pageSize": page_size,
#                 "list": paginated_data,
#             },
#         }
#
#         return JsonResponse(response)
